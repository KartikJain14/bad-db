package org.baddb.storage;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

/**
 * SlottedPage implements a flexible record storage layout.
 * It uses a "slotted-page" architecture where:
 * 1. A slot directory (array of pointers) grows from the top (after the header) downwards.
 * 2. Actual record data grows from the bottom of the page upwards.
 * 3. The space between the slot directory and the records is the "free space".
 *
 * This layout allows for variable-length records and efficient deletion/re-insertion.
 */
public class SlottedPage extends Page {
    /** Each slot in the directory is 4 bytes: 2 bytes for the record's offset, 2 bytes for its size. */
    private static final int SLOT_SIZE = 4;

    /**
     * Initializes a brand new slotted page.
     * Sets the free space pointer to the very end of the page.
     *
     * @param pageId the ID for this page
     */
    public SlottedPage(int pageId) {
        super(pageId);
        setType(PageType.SLOTTED);
        // Free space starts at the end of the page and grows backwards.
        setFreeSpacePointer((short) Constants.PAGE_SIZE);
        setSlotCount((short) 0);
    }

    /**
     * Reconstructs a slotted page from existing data.
     *
     * @param pageId the ID for this page
     * @param data the raw byte content
     */
    public SlottedPage(int pageId, byte[] data) {
        super(pageId, data);
        // If the page was just allocated but not initialized, set it up.
        if (getType() == PageType.INVALID) {
            setType(PageType.SLOTTED);
            setFreeSpacePointer((short) Constants.PAGE_SIZE);
            setSlotCount((short) 0);
        }
    }

    /**
     * Inserts a record into the page if there is enough free space.
     * The record is copied to the end of the free space, and a new slot is added to the directory.
     *
     * @param record the byte array to store
     * @return the slot ID (index) of the inserted record, or -1 if no space is available
     */
    public int insertRecord(byte[] record) {
        short size = (short) record.length;
        short slotCount = getSlotCount();
        short freeSpacePointer = getFreeSpacePointer();
        
        // Calculate needed space: 4 bytes for the new slot entry + the actual record bytes.
        int neededSpace = SLOT_SIZE + size;
        // Used space at the top = header size + current slot directory size.
        int usedSpaceHeader = Constants.PAGE_HEADER_SIZE + (slotCount * SLOT_SIZE);
        
        // Check if the gap between the directory and the records is large enough.
        if (freeSpacePointer - usedSpaceHeader < neededSpace) {
            return -1; // Insufficient space in this page.
        }

        // 1. Calculate the new start offset for the record (moving backwards from the current pointer).
        short newOffset = (short) (freeSpacePointer - size);
        setFreeSpacePointer(newOffset);

        // 2. Copy the record data into the page buffer at the calculated offset.
        System.arraycopy(record, 0, data, newOffset, size);

        // 3. Update the slot directory at the top.
        int slotId = slotCount;
        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Store the offset where the record begins.
        buffer.putShort(slotOffset, newOffset);
        // Store the record's size.
        buffer.putShort(slotOffset + 2, size);

        // 4. Update metadata and mark page as modified.
        setSlotCount((short) (slotCount + 1));
        setDirty(true);

        return slotId;
    }

    /**
     * Retrieves a record's data using its slot ID.
     *
     * @param slotId the index in the slot directory
     * @return the record data as a byte array, or null if deleted or invalid
     */
    public byte[] getRecord(int slotId) {
        if (slotId < 0 || slotId >= getSlotCount()) return null;

        // Calculate where the slot metadata is located.
        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short offset = buffer.getShort(slotOffset);
        short size = buffer.getShort(slotOffset + 2);

        // offset 0 implies the record was deleted (a tombstone).
        if (offset == 0) return null;

        // Extract and return the record bytes.
        byte[] record = new byte[size];
        System.arraycopy(data, offset, record, 0, size);
        return record;
    }

    /**
     * Updates an existing record's contents.
     * Currently only supports in-place updates if the size remains the same.
     *
     * @param slotId the ID of the record to update
     * @param newRecord the new data for the record
     * @return true if successful, false otherwise
     */
    public boolean updateRecord(int slotId, byte[] newRecord) {
        if (slotId < 0 || slotId >= getSlotCount()) return false;

        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short oldSize = buffer.getShort(slotOffset + 2);

        // Only handle same-size updates to avoid complex fragmentation management/compaction.
        if (newRecord.length == oldSize) {
            short offset = buffer.getShort(slotOffset);
            System.arraycopy(newRecord, 0, data, offset, oldSize);
            setDirty(true);
            return true;
        }

        // Variable-sized updates would require de-fragmentation or moving the record.
        // Returning false for simplicity in this version.
        return false; 
    }

    /**
     * Marks a record as deleted by zeroing its entry in the slot directory.
     * Note: This does not immediately reclaim the space (fragmentation exists until compaction).
     *
     * @param slotId the ID of the record to delete
     */
    public void deleteRecord(int slotId) {
        if (slotId < 0 || slotId >= getSlotCount()) return;

        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Setting offset to 0 marks this slot as empty/"deleted".
        buffer.putShort(slotOffset, (short) 0);
        buffer.putShort(slotOffset + 2, (short) 0);
        setDirty(true);
    }
}
