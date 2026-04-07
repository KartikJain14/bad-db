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
        return values()[ordinal];
    }
}
