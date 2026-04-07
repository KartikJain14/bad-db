package org.baddb.storage;

import org.baddb.index.BTree;
import org.baddb.model.Record;
import org.baddb.model.Schema;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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
        this.dataStartOffset = fileManager.getCurrentPosition();
        
        rebuildIndex();
    }

    private void rebuildIndex() throws IOException {
        long currentOffset = dataStartOffset;
        long fileLength = fileManager.getFileLength();
        
        while (currentOffset < fileLength) {
            try {
                Record r = fileManager.readRecord(currentOffset, schema);
                int pk = (Integer) r.getValue(0);
                index.insert(pk, currentOffset);
                
                // Move currentOffset to the start of the next record
                currentOffset = fileManager.getCurrentPosition();
            } catch (Exception e) {
                break; // End of file or record corruption
            }
        }
    }

    public void insertRecord(Record record) throws IOException {
        long offset = fileManager.appendRecord(record, schema);
        int primaryKey = (Integer) record.getValue(0);
        index.insert(primaryKey, offset);
    }

    public Record searchByPrimaryKey(int key) throws IOException {
        Long offset = index.search(key);
        return offset == null ? null : fileManager.readRecord(offset, schema);
    }

    public List<Record> select(String columnName, Object value) throws IOException {
        List<Record> results = new ArrayList<>();
        int colIndex = -1;
        for (int i = 0; i < schema.getColumnCount(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                colIndex = i;
                break;
            }
        }
        if (colIndex == -1) return results;
        if (colIndex == 0 && value instanceof Integer) {
            Record r = searchByPrimaryKey((Integer) value);
            if (r != null) results.add(r);
            return results;
        }
        List<Record> all = getAllRecords();
        for (Record r : all) {
            Object recordVal = r.getValue(colIndex);
            if (recordVal != null && recordVal.equals(value)) {
                results.add(r);
            }
        }
        return results;
    }

    public List<Record> getAllRecords() throws IOException {
        return fileManager.scanAllRecords(dataStartOffset, schema);
    }

    public void close() throws IOException { fileManager.close(); }
    public Schema getSchema() { return schema; }
    public String getName() { return name; }
}
