package org.baddb.common;

/**
 * Holds global constants and configuration settings for the database engine.
 * This includes disk page sizes, file extensions, and byte-level layout offsets for the page header.
 */
public class Constants {
    /** The fixed size of a database page in bytes (default 4KB). */
    public static final int PAGE_SIZE = 4096;
    /** Sentinel value representing a non-existent or null page reference. */
    public static final int INVALID_PAGE_ID = -1;
    /** Standard file extension for the main database data file. */
    public static final String DB_EXTENSION = ".db";
    /** Standard file extension for the Write-Ahead Log (WAL) file. */
    public static final String WAL_EXTENSION = ".wal";
    
    // Page Header offsets (layout)
    // Structure: [PageID: 4 bytes][Type: 1 byte][FreeSpacePointer: 2 bytes][SlotCount: 2 bytes][NextPageId: 4 bytes]
    // Total header size = 13 bytes.
    
    /** The total size of the fixed metadata at the start of every page. */
    public static final int PAGE_HEADER_SIZE = 13;
    /** Offset where the page's unique ID is stored. */
    public static final int PAGE_ID_OFFSET = 0;
    /** Offset where the page's type identifier is stored. */
    public static final int PAGE_TYPE_OFFSET = 4;
    /** Offset for the pointer indicating where the next record can be inserted. */
    public static final int FREE_SPACE_OFFSET = 5;
    /** Offset where the count of slots/records in the page is stored. */
    public static final int SLOT_COUNT_OFFSET = 7;
    /** Offset for the ID of the logically next page (for linked lists of pages). */
    public static final int NEXT_PAGE_OFFSET = 9;
}
