package org.baddb.storage;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

/**
 * Base class for all page types in the database.
 * A Page represents a fixed-size block of data (usually 4KB or 8KB) that is read from or written to disk.
 * It contains a header with metadata (like page ID, type, and slot information) followed by the actual data.
 * The layout is managed via byte offsets defined in {@link Constants}.
 */
public abstract class Page {
    /** The raw byte array representing the page's contents on disk. */
    protected final byte[] data;
    /** The unique identifier for this page. */
    protected final int pageId;
    /** A flag indicating if the page has been modified in memory and needs to be written back to disk. */
    protected boolean isDirty;

    /**
     * Creates a new, blank page in memory.
     * Initializes the header with the provided page ID.
     *
     * @param pageId the ID assigned to this page
     */
    public Page(int pageId) {
        this.data = new byte[Constants.PAGE_SIZE];
        this.pageId = pageId;
        this.isDirty = false;
        
        // Write the page ID into the appropriate header slot in the raw byte array.
        setPageId(pageId);
    }

    /**
     * Creates a page object wrapping existing data read from disk.
     *
     * @param pageId the ID of the page
     * @param data the raw data buffer containing the page's disk content
     */
    public Page(int pageId, byte[] data) {
        this.data = data;
        this.pageId = pageId;
        this.isDirty = false;
    }

    /** @return the page ID */
    public int getPageId() {
        return pageId;
    }

    /** @return the raw byte representation of the page */
    public byte[] getData() {
        return data;
    }

    /** @return true if the page has unsaved changes */
    public boolean isDirty() {
        return isDirty;
    }

    /** @param dirty sets the dirty flag */
    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    /** Internal helper to write the page ID into the byte header. */
    protected void setPageId(int pageId) {
        ByteBuffer.wrap(data).putInt(Constants.PAGE_ID_OFFSET, pageId);
    }

    /** @return the type of the page (e.g., SLOTTED, ROOT) from the header */
    public PageType getType() {
        return PageType.fromValue(data[Constants.PAGE_TYPE_OFFSET]);
    }

    /** Internal helper to set the page type in the byte header. */
    protected void setType(PageType type) {
        data[Constants.PAGE_TYPE_OFFSET] = type.getValue();
    }

    /** 
     * Returns the offset to the start of the free space in the page.
     * Free space usually grows from the end of the header towards the end of the page,
     * or from the end of the page towards the header depending on the strategy.
     * In this implementation, free space usually starts after the slot array.
     */
    public short getFreeSpacePointer() {
        return ByteBuffer.wrap(data).getShort(Constants.FREE_SPACE_OFFSET);
    }

    /** Sets the free space pointer in the header. */
    protected void setFreeSpacePointer(short pointer) {
        ByteBuffer.wrap(data).putShort(Constants.FREE_SPACE_OFFSET, pointer);
    }

    /** @return the number of records (slots) currently stored in this page */
    public short getSlotCount() {
        return ByteBuffer.wrap(data).getShort(Constants.SLOT_COUNT_OFFSET);
    }

    /** Sets the total number of slots in the header. */
    protected void setSlotCount(short count) {
        ByteBuffer.wrap(data).putShort(Constants.SLOT_COUNT_OFFSET, count);
    }

    /** @return the ID of the next page in a linked list of pages (e.g., for overflows or scans) */
    public int getNextPageId() {
        return ByteBuffer.wrap(data).getInt(Constants.NEXT_PAGE_OFFSET);
    }

    /** Sets the next page ID in the header. */
    public void setNextPageId(int pageId) {
        ByteBuffer.wrap(data).putInt(Constants.NEXT_PAGE_OFFSET, pageId);
    }
}
