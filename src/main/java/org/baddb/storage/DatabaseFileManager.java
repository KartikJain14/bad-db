package org.baddb.storage;

import org.baddb.model.Column;
import org.baddb.model.Record;
import org.baddb.model.Schema;
import org.baddb.model.DataType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles raw binary file operations for the BadDB engine.
 */
public class DatabaseFileManager {
    private static final String MAGIC_NUMBER = "BKJ20";
    private static final int VERSION = 1;
    
    private final String dbPath;
    private RandomAccessFile raf;

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
        if (file.exists()) file.delete();
        openDatabase();
        
        raf.writeBytes(MAGIC_NUMBER); // 5 bytes
        raf.writeInt(VERSION);        // 4 bytes
        raf.writeInt(1);              // 4 bytes (Table Count)
        raf.writeUTF(tableName);
        raf.writeInt(schema.getColumnCount());
        for (Column col : schema.getColumns()) {
            raf.writeUTF(col.getName());
            raf.writeInt(col.getType().ordinal());
        }
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
        
        raf.readInt(); // Skip Version
        raf.readInt(); // Skip Table Count
        raf.readUTF(); // Skip Table Name
        
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
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column col = schema.getColumns().get(i);
            Object val = record.getValue(i);
            switch (col.getType()) {
                case INT: raf.writeInt((Integer) val); break;
                case FLOAT: raf.writeFloat((Float) val); break;
                case STRING: raf.writeUTF((String) val); break;
                case BOOLEAN: raf.writeBoolean((Boolean) val); break;
            }
        }
        return offset;
    }

    public Record readRecord(long offset, Schema schema) throws IOException {
        raf.seek(offset);
        Record record = new Record(schema.getColumnCount());
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column col = schema.getColumns().get(i);
            switch (col.getType()) {
                case INT: record.setValue(i, raf.readInt()); break;
                case FLOAT: record.setValue(i, raf.readFloat()); break;
                case STRING: record.setValue(i, raf.readUTF()); break;
                case BOOLEAN: record.setValue(i, raf.readBoolean()); break;
            }
        }
        return record;
    }

    public List<Record> scanAllRecords(long dataStartOffset, Schema schema) throws IOException {
        List<Record> records = new ArrayList<>();
        raf.seek(dataStartOffset);
        while (raf.getFilePointer() < raf.length()) {
            records.add(readRecord(raf.getFilePointer(), schema));
        }
        return records;
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
}
