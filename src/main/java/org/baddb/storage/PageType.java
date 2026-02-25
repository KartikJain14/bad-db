package org.baddb.storage;

public enum PageType {
    INVALID((byte) 0),
    SLOTTED((byte) 1),
    CATALOG((byte) 2),
    INDEX_LEAF((byte) 3),
    INDEX_INTERNAL((byte) 4);

    private final byte value;

    PageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PageType fromValue(byte value) {
        for (PageType type : values()) {
            if (type.value == value) return type;
        }
        return INVALID;
    }
}
