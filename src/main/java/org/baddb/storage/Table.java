package org.baddb.storage;

import org.baddb.index.BTree;
import org.baddb.model.Column;
import org.baddb.model.DataType;
import org.baddb.model.Record;
import org.baddb.model.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates table operations: schema, records, and indexing.
 */
public class Table {
    private String name;
    private Schema schema;
    private final DatabaseFileManager fileManager;
    private final BTree index;
    private long dataStartOffset;

    /**
     * Use this constructor when you are about to call initialize() for a new DB.
     */
    public Table(String name, Schema schema, String dbPath) {
        this.name = name;
        this.schema = schema;
        this.fileManager = new DatabaseFileManager(dbPath);
        this.index = new BTree(2);
    }

    /**
     * Use this constructor when you want to load an existing DB from disk.
     */
    public Table(String dbPath) {
        this.fileManager = new DatabaseFileManager(dbPath);
        this.index = new BTree(2);
    }

    /**
     * Creates a fresh database file with the given schema. Overwrites existing data.
     */
    public void initialize() throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Table name is required before initialize().");
        }
        if (schema == null || schema.getColumnCount() == 0) {
            throw new IllegalStateException("Schema must contain at least one column before initialize().");
        }

        fileManager.createNewDatabase(name, schema);
        this.dataStartOffset = fileManager.getCurrentPosition();
    }

    /**
     * Opens an existing database file and builds the B-Tree index by scanning all records.
     */
    public void open() throws IOException {
        if (!fileManager.exists()) {
            throw new IOException("Database file does not exist. Use initialize() first.");
        }
        fileManager.openDatabase();
        this.schema = fileManager.readMetadata();
        this.name = fileManager.getTableName();
        this.dataStartOffset = fileManager.getCurrentPosition();

        if (fileManager.getFileVersion() < 2) {
            fileManager.upgradeLegacyFormat(name, schema, dataStartOffset);
            this.schema = fileManager.readMetadata();
            this.dataStartOffset = fileManager.getCurrentPosition();
        }

        rebuildIndex();
    }

    private void rebuildIndex() throws IOException {
        index.clear();
        long currentOffset = dataStartOffset;
        long fileLength = fileManager.getFileLength();

        while (currentOffset < fileLength) {
            DatabaseFileManager.StoredRecord storedRecord = fileManager.readStoredRecord(currentOffset, schema);
            int primaryKey = getPrimaryKey(storedRecord.getRecord());
            if (storedRecord.isActive()) {
                index.upsert(primaryKey, storedRecord.getOffset());
            } else {
                index.remove(primaryKey);
            }
            currentOffset = storedRecord.getNextOffset();
        }
    }

    public void insertRecord(Record record) throws IOException {
        validateRecord(record, null);
        int primaryKey = getPrimaryKey(record);
        if (index.contains(primaryKey)) {
            throw new IllegalArgumentException("Record with primary key " + primaryKey + " already exists.");
        }

        long offset = fileManager.appendRecord(record, schema);
        index.insert(primaryKey, offset);
    }

    public boolean upsertRecord(Record record) throws IOException {
        validateRecord(record, null);
        int primaryKey = getPrimaryKey(record);
        if (exists(primaryKey)) {
            return updateRecord(primaryKey, record);
        }

        insertRecord(record);
        return false;
    }

    public boolean updateRecord(int primaryKey, Record updatedRecord) throws IOException {
        validateRecord(updatedRecord, primaryKey);
        Long existingOffset = index.search(primaryKey);
        if (existingOffset == null) {
            return false;
        }

        long newOffset = fileManager.appendRecord(updatedRecord, schema);
        fileManager.markDeleted(existingOffset);
        index.upsert(primaryKey, newOffset);
        return true;
    }

    public boolean deleteRecord(int primaryKey) throws IOException {
        Long offset = index.search(primaryKey);
        if (offset == null) {
            return false;
        }

        fileManager.markDeleted(offset);
        index.remove(primaryKey);
        return true;
    }

    public void compact() throws IOException {
        List<Record> activeRecords = getAllRecords();
        this.dataStartOffset = fileManager.rewriteDatabase(name, schema, activeRecords);
        rebuildIndex();
    }

    public Record searchByPrimaryKey(int key) throws IOException {
        Long offset = index.search(key);
        return offset == null ? null : fileManager.readRecord(offset, schema);
    }

    public boolean exists(int key) {
        return index.contains(key);
    }

    public int countRecords() {
        return index.snapshot().size();
    }

    public List<Record> select(String columnName, Object value) throws IOException {
        List<Record> results = new ArrayList<>();
        int colIndex = resolveColumnIndex(columnName);
        if (colIndex == -1) {
            return results;
        }

        if (colIndex == 0 && value instanceof Integer) {
            Record record = searchByPrimaryKey((Integer) value);
            if (record != null) {
                results.add(record);
            }
            return results;
        }

        List<Record> all = getAllRecords();
        for (Record record : all) {
            Object recordVal = record.getValue(colIndex);
            if (value == null ? recordVal == null : value.equals(recordVal)) {
                results.add(record);
            }
        }
        return results;
    }

    public List<Record> getAllRecords() throws IOException {
        return fileManager.scanAllRecords(dataStartOffset, schema);
    }

    public void close() throws IOException {
        fileManager.close();
    }

    public Schema getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    private int resolveColumnIndex(String columnName) {
        for (int i = 0; i < schema.getColumnCount(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private int getPrimaryKey(Record record) {
        return (Integer) record.getValue(0);
    }

    private void validateRecord(Record record, Integer expectedPrimaryKey) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null.");
        }
        if (schema == null) {
            throw new IllegalStateException("Schema is not loaded.");
        }
        if (record.getValues().length != schema.getColumnCount()) {
            throw new IllegalArgumentException("Record column count does not match schema.");
        }

        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column column = schema.getColumns().get(i);
            Object value = record.getValue(i);
            if (value == null) {
                throw new IllegalArgumentException("Column '" + column.getName() + "' cannot be null.");
            }
            if (!isCompatibleType(column.getType(), value)) {
                throw new IllegalArgumentException("Column '" + column.getName()
                        + "' expects " + column.getType() + " but received "
                        + value.getClass().getSimpleName() + ".");
            }
        }

        if (expectedPrimaryKey != null && !expectedPrimaryKey.equals(record.getValue(0))) {
            throw new IllegalArgumentException("Updated record must keep the same primary key.");
        }
    }

    private boolean isCompatibleType(DataType dataType, Object value) {
        switch (dataType) {
            case INT:
                return value instanceof Integer;
            case FLOAT:
                return value instanceof Float;
            case STRING:
                return value instanceof String;
            case BOOLEAN:
                return value instanceof Boolean;
            default:
                return false;
        }
    }
}
