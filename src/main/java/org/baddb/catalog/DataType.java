package org.baddb.catalog;

/**
 * Represents the fundamental data types supported by the database engine's column definitions.
 */
public enum DataType {
    /** Fixed-size 4-byte integer. */
    INT,
    /** Fixed-size 4 or 8-byte floating point number. */
    FLOAT,
    /** Binary truth value (stored as 1 or 0). */
    BOOLEAN,
    /** Variable-length character string. */
    STRING
}
