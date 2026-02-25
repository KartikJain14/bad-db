package org.baddb.catalog;

import java.io.*;

public class TableMetadata implements Serializable {
    private final String tableName;
    private final Schema schema;
    private final int rootPageId;

    public TableMetadata(String tableName, Schema schema, int rootPageId) {
        this.tableName = tableName;
        this.schema = schema;
        this.rootPageId = rootPageId;
    }

    public String getTableName() {
        return tableName;
    }

    public Schema getSchema() {
        return schema;
    }

    public int getRootPageId() {
        return rootPageId;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        return baos.toByteArray();
    }

    public static TableMetadata deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (TableMetadata) ois.readObject();
    }
}
