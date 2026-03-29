package org.baddb.engine;

import org.baddb.catalog.Column;
import org.baddb.catalog.DataType;
import org.baddb.catalog.Schema;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single row of data in a database table.
 * It stores column values in an internal map and provides methods to serialize/deserialize
 * this data into a compact byte format based on a given {@link Schema}.
 */
public class Record {
    /** Stores the mapping of column names to their actual data values. */
    private final Map<String, Object> values;

    /**
     * Initializes an empty record.
     */
    public Record() {
        this.values = new HashMap<>();
    }

    /**
     * Sets the value for a specific column.
     *
     * @param column column name
     * @param value data value
     */
    public void set(String column, Object value) {
        values.put(column, value);
    }

    /**
     * Retrieves the value for a specific column.
     *
     * @param column column name
     * @return the data value
     */
    public Object get(String column) {
        return values.get(column);
    }

    /**
     * Converts the record's values into a byte array according to the table's schema.
     * Fields are written in the order they appear in the schema.
     *
     * @param schema the structural definition of the table
     * @return the serialized record bytes
     * @throws IOException if a mandatory field is missing or an error occurs during writing
     */
    public byte[] serialize(Schema schema) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (Column col : schema.getColumns()) {
            Object val = values.get(col.name());
            // Enforcement of NOT NULL constraints.
            if (val == null && col.isNotNull()) {
                throw new IOException("Constraint violation: Column " + col.name() + " cannot be null");
            }
            writeField(dos, col.type(), val);
        }
        return baos.toByteArray();
    }

    /**
     * Reconstructs a Record from a byte array using the provided schema.
     *
     * @param data the raw bytes of the record
     * @param schema the table's schema
     * @return a hydrated Record object
     * @throws IOException if occurs an error reading from the byte stream
     */
    public static Record deserialize(byte[] data, Schema schema) throws IOException {
        Record record = new Record();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        for (Column col : schema.getColumns()) {
            record.set(col.name(), readField(dis, col.type()));
        }
        return record;
    }

    /** Helper to write individual fields based on their type. */
    private void writeField(DataOutputStream dos, DataType type, Object val) throws IOException {
        switch (type) {
            case INT -> dos.writeInt(val == null ? 0 : (int) val);
            case FLOAT -> dos.writeFloat(val == null ? 0.0f : (float) val);
            case BOOLEAN -> dos.writeBoolean(val != null && (boolean) val);
            case STRING -> {
                String s = (val == null) ? "" : (String) val;
                // writeUTF prepends the string with its length, making it ideal for variable strings.
                dos.writeUTF(s);
            }
        }
    }

    /** Helper to read individual fields based on their type. */
    private static Object readField(DataInputStream dis, DataType type) throws IOException {
        return switch (type) {
            case INT -> dis.readInt();
            case FLOAT -> dis.readFloat();
            case BOOLEAN -> dis.readBoolean();
            case STRING -> dis.readUTF();
        };
    }
}
