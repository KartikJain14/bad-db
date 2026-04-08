package org.baddb.storage;

import org.baddb.model.Column;
import org.baddb.model.DataType;
import org.baddb.model.Record;
import org.baddb.model.Schema;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles raw binary file operations for the BadDB engine.
 */
public class DatabaseFileManager {
    private static final String MAGIC_NUMBER = "BKJ20";
    private static final int VERSION = 2;

    private final String dbPath;
    private RandomAccessFile raf;
    private int fileVersion = VERSION;
    private String tableName;

    public DatabaseFileManager(String dbPath) {
        this.dbPath = dbPath;
    }

    public boolean exists() {
        return new File(dbPath).exists();
    }

    public void openDatabase() throws IOException {
        raf = new RandomAccessFile(dbPath, "rw");
    }

    /**
     * Creates and initializes a NEW database file. Overwrites if exists.
     */
    public void createNewDatabase(String tableName, Schema schema) throws IOException {
        File file = new File(dbPath);
        if (file.exists()) {
            file.delete();
        }
        openDatabase();
        writeMetadata(tableName, schema, VERSION);
    }

    /**
     * Reads metadata from an existing file and builds the Schema object.
     */
    public Schema readMetadata() throws IOException {
        raf.seek(0);

        byte[] magic = new byte[MAGIC_NUMBER.length()];
        raf.readFully(magic);
        if (!new String(magic).equals(MAGIC_NUMBER)) {
            throw new IOException("Invalid database file magic number.");
        }

        this.fileVersion = raf.readInt();
        raf.readInt(); // Skip Table Count
        this.tableName = raf.readUTF();

        int colCount = raf.readInt();
        Schema schema = new Schema();
        for (int i = 0; i < colCount; i++) {
            String name = raf.readUTF();
            int typeOrdinal = raf.readInt();
            schema.addColumn(name, DataType.fromOrdinal(typeOrdinal));
        }

        return schema;
    }

    public long appendRecord(Record record, Schema schema) throws IOException {
        long offset = raf.length();
        raf.seek(offset);
        if (fileVersion >= 2) {
            raf.writeBoolean(true);
        }
        writeRecordValues(record, schema);
        return offset;
    }

    public Record readRecord(long offset, Schema schema) throws IOException {
        return readStoredRecord(offset, schema).getRecord();
    }

    public StoredRecord readStoredRecord(long offset, Schema schema) throws IOException {
        raf.seek(offset);
        boolean active = true;
        if (fileVersion >= 2) {
            active = raf.readBoolean();
        }

        Record record = new Record(schema.getColumnCount());
        populateRecord(record, schema);
        long nextOffset = raf.getFilePointer();
        return new StoredRecord(offset, nextOffset, active, record);
    }

    public void markDeleted(long offset) throws IOException {
        if (fileVersion < 2) {
            throw new IOException("Delete markers are not supported in legacy database files.");
        }
        raf.seek(offset);
        raf.writeBoolean(false);
    }

    public List<Record> scanAllRecords(long dataStartOffset, Schema schema) throws IOException {
        List<Record> records = new ArrayList<>();
        long currentOffset = dataStartOffset;
        while (currentOffset < raf.length()) {
            StoredRecord storedRecord = readStoredRecord(currentOffset, schema);
            if (storedRecord.isActive()) {
                records.add(storedRecord.getRecord());
            }
            currentOffset = storedRecord.getNextOffset();
        }
        return records;
    }

    public void upgradeLegacyFormat(String tableName, Schema schema, long dataStartOffset) throws IOException {
        if (fileVersion >= VERSION) {
            return;
        }

        List<Record> legacyRecords = scanAllRecords(dataStartOffset, schema);
        rewriteDatabase(tableName, schema, legacyRecords);
    }

    public long rewriteDatabase(String tableName, Schema schema, List<Record> records) throws IOException {
        raf.setLength(0);
        raf.seek(0);
        writeMetadata(tableName, schema, VERSION);
        long dataStartOffset = getCurrentPosition();
        for (Record record : records) {
            appendRecord(record, schema);
        }
        return dataStartOffset;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public String getTableName() {
        return tableName;
    }

    public long getFileLength() throws IOException {
        return raf == null ? 0 : raf.length();
    }

    public long getCurrentPosition() throws IOException {
        return raf == null ? 0 : raf.getFilePointer();
    }

    public void close() throws IOException {
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    private void writeMetadata(String tableName, Schema schema, int version) throws IOException {
        this.fileVersion = version;
        this.tableName = tableName;

        raf.writeBytes(MAGIC_NUMBER); // 5 bytes
        raf.writeInt(version);        // 4 bytes
        raf.writeInt(1);              // 4 bytes (Table Count)
        raf.writeUTF(tableName);
        raf.writeInt(schema.getColumnCount());
        for (Column col : schema.getColumns()) {
            raf.writeUTF(col.getName());
            raf.writeInt(col.getType().ordinal());
        }
    }

    private void writeRecordValues(Record record, Schema schema) throws IOException {
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column col = schema.getColumns().get(i);
            Object val = record.getValue(i);
            switch (col.getType()) {
                case INT:
                    raf.writeInt((Integer) val);
                    break;
                case FLOAT:
                    raf.writeFloat((Float) val);
                    break;
                case STRING:
                    raf.writeUTF((String) val);
                    break;
                case BOOLEAN:
                    raf.writeBoolean((Boolean) val);
                    break;
            }
        }
    }

    private void populateRecord(Record record, Schema schema) throws IOException {
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column col = schema.getColumns().get(i);
            switch (col.getType()) {
                case INT:
                    record.setValue(i, raf.readInt());
                    break;
                case FLOAT:
                    record.setValue(i, raf.readFloat());
                    break;
                case STRING:
                    record.setValue(i, raf.readUTF());
                    break;
                case BOOLEAN:
                    record.setValue(i, raf.readBoolean());
                    break;
            }
        }
    }

    public static class StoredRecord {
        private final long offset;
        private final long nextOffset;
        private final boolean active;
        private final Record record;

        public StoredRecord(long offset, long nextOffset, boolean active, Record record) {
            this.offset = offset;
            this.nextOffset = nextOffset;
            this.active = active;
            this.record = record;
        }

        public long getOffset() {
            return offset;
        }

        public long getNextOffset() {
            return nextOffset;
        }

        public boolean isActive() {
            return active;
        }

        public Record getRecord() {
            return record;
        }
    }
}
