package org.baddb.model;

/**
 * Supported data types for the BadDB engine.
 */
public enum DataType {
    INT,
    FLOAT,
    STRING,
    BOOLEAN;

    public static DataType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IllegalArgumentException("Invalid DataType ordinal: " + ordinal);
        }
        return values()[ordinal];
    }
}
