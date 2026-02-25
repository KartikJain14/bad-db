package org.baddb.engine;

import org.baddb.catalog.Column;
import org.baddb.catalog.DataType;
import org.baddb.catalog.Schema;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Record {
    private final Map<String, Object> values;

    public Record() {
        this.values = new HashMap<>();
    }

    public void set(String column, Object value) {
        values.put(column, value);
    }

    public Object get(String column) {
        return values.get(column);
    }

    public byte[] serialize(Schema schema) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (Column col : schema.getColumns()) {
            Object val = values.get(col.name());
            if (val == null && col.isNotNull()) {
                throw new IOException("Column " + col.name() + " cannot be null");
            }
            writeField(dos, col.type(), val);
        }
        return baos.toByteArray();
    }

    public static Record deserialize(byte[] data, Schema schema) throws IOException {
        Record record = new Record();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        for (Column col : schema.getColumns()) {
            record.set(col.name(), readField(dis, col.type()));
        }
        return record;
    }

    private void writeField(DataOutputStream dos, DataType type, Object val) throws IOException {
        switch (type) {
            case INT -> dos.writeInt(val == null ? 0 : (int) val);
            case FLOAT -> dos.writeFloat(val == null ? 0.0f : (float) val);
            case BOOLEAN -> dos.writeBoolean(val != null && (boolean) val);
            case STRING -> {
                String s = (val == null) ? "" : (String) val;
                dos.writeUTF(s);
            }
        }
    }

    private static Object readField(DataInputStream dis, DataType type) throws IOException {
        return switch (type) {
            case INT -> dis.readInt();
            case FLOAT -> dis.readFloat();
            case BOOLEAN -> dis.readBoolean();
            case STRING -> dis.readUTF();
        };
    }
}
