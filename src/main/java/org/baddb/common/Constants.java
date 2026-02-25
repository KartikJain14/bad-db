package org.baddb.common;

public class Constants {
    public static final int PAGE_SIZE = 4096;
    public static final int INVALID_PAGE_ID = -1;
    public static final String DB_EXTENSION = ".db";
    public static final String WAL_EXTENSION = ".wal";
    
    // Page Header offsets (simplified for now)
    // [PageID: 4][Type: 1][FreeSpacePointer: 2][SlotCount: 2][NextPageId: 4] = 13 bytes header
    public static final int PAGE_HEADER_SIZE = 13;
    public static final int PAGE_ID_OFFSET = 0;
    public static final int PAGE_TYPE_OFFSET = 4;
    public static final int FREE_SPACE_OFFSET = 5;
    public static final int SLOT_COUNT_OFFSET = 7;
    public static final int NEXT_PAGE_OFFSET = 9;
}
