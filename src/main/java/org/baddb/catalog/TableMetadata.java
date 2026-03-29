package org.baddb.catalog;

import java.io.*;

/**
 * TableMetadata holds all persistent information about a database table.
 * This includes its name, the column {@link Schema}, and the entry point for its data (rootPageId).
 * This class is designed to be serialized into a page for persistence in the catalog.
 */
public class TableMetadata implements Serializable {
    /** The unique name of the table in the database. */
    private final String tableName;
    /** The structure of the rows within this table. */
    private final Schema schema;
    /** The ID of the first page (or root node of a B+ Tree) containing the table's records. */
    private final int rootPageId;

    /**
     * Creates new table metadata.
     *
     * @param tableName name of the table
     * @param schema schema definition
     * @param rootPageId ID for the first page of data
     */
    public TableMetadata(String tableName, Schema schema, int rootPageId) {
        this.tableName = tableName;
        this.schema = schema;
        this.rootPageId = rootPageId;
    }

    /** @return the table's name */
    public String getTableName() {
        return tableName;
    }

    /** @return the table's schema */
    public Schema getSchema() {
        return schema;
    }

    /** @return the ID of the first data page */
    public int getRootPageId() {
        return rootPageId;
    }

    /**
     * Converts this metadata object into a byte array for disk storage.
     * Uses standard Java Serialization for simplicity in this educational implementation.
     *
     * @return the serialized byte array
     * @throws IOException if occurs an error during serialization
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        return baos.toByteArray();
    }

    /**
     * Reconstructs a TableMetadata object from its disk-serialized byte array.
     *
     * @param data the raw bytes read from the catalog page
     * @return the reconstructed TableMetadata object
     * @throws IOException if occurs a read error
     * @throws ClassNotFoundException if the class definition is missing during deserialization
     */
    public static TableMetadata deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (TableMetadata) ois.readObject();
    }
}
