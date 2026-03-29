package org.baddb.storage;

/**
 * Defines the different types of pages used in the database storage system.
 * Each type has a unique byte identifier stored in the page header to allow the
 * engine to correctly interpret the page's contents.
 */
public enum PageType {
    /** An uninitialized or corrupted page. */
    INVALID((byte) 0),
    /** A page that uses the slotted-page layout to store variable-length records. */
    SLOTTED((byte) 1),
    /** A specialized page for storing database schema metadata (the system catalog). */
    CATALOG((byte) 2),
    /** A leaf node in a B+ Tree index, containing actual keys and pointers to records. */
    INDEX_LEAF((byte) 3),
    /** An internal node in a B+ Tree index, containing keys and pointers to other tree nodes. */
    INDEX_INTERNAL((byte) 4);

    /** The raw byte value representing this page type in the disk header. */
    private final byte value;

    PageType(byte value) {
        this.value = value;
    }

    /** @return the byte value of the page type */
    public byte getValue() {
        return value;
    }

    /**
     * Converts a raw byte value from the disk into its corresponding PageType enum.
     *
     * @param value the byte value read from the header
     * @return the matching PageType, or INVALID if no match is found
     */
    public static PageType fromValue(byte value) {
        for (PageType type : values()) {
            if (type.value == value) return type;
        }
        return INVALID;
    }
}
