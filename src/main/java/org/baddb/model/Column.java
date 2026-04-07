package org.baddb.model;

/**
 * Represents a column in a database table schema.
 */
public class Column {
    private final String name;
    private final DataType type;

    public Column(String name, DataType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
