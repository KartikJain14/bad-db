package org.bad.db.common;

public class Constants {
    public static final int PAGE_SIZE = 4096; // 4KB
    public static final String DB_EXTENSION = ".db";
    public static final String WAL_EXTENSION = ".log";
    
    // Page Header Offsets
    public static final int PAGE_ID_OFFSET = 0;
    public static final int PAGE_TYPE_OFFSET = 4;
    public static final int SLOT_COUNT_OFFSET = 8;
    public static final int FREE_SPACE_OFFSET = 12;
    public static final int HEADER_SIZE = 16;
}
