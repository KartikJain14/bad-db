package org.bad.db.storage;

import org.bad.db.common.Constants;
import java.nio.ByteBuffer;

/**
 * Slotted Page layout for variable-length records.
 * 
 * [Header (16 bytes)] [Slots (8 bytes each)] ... [Free Space] ... [Data]
 * Slot = (Offset, Length)
 */
public class SlottedPage extends Page {
    
    public SlottedPage(int pageId) {
        super(pageId);
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        buffer.putInt(Constants.SLOT_COUNT_OFFSET, 0);
        buffer.putInt(Constants.FREE_SPACE_OFFSET, Constants.PAGE_SIZE);
    }

    private static final int SLOTTED_PAGE_MAGIC = 0x534C4F54; // "SLOT"

    public SlottedPage(Page page) {
        super(page.getPageId(), page.getData());
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        // Use a magic number at PAGE_TYPE_OFFSET to identify initialized slotted pages
        if (buffer.getInt(Constants.PAGE_TYPE_OFFSET) != SLOTTED_PAGE_MAGIC) {
            buffer.putInt(Constants.PAGE_TYPE_OFFSET, SLOTTED_PAGE_MAGIC);
            buffer.putInt(Constants.SLOT_COUNT_OFFSET, 0);
            buffer.putInt(Constants.FREE_SPACE_OFFSET, Constants.PAGE_SIZE);
            setDirty(true);
        }
    }

    public int insertRecord(byte[] recordData) {
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        int slotCount = buffer.getInt(Constants.SLOT_COUNT_OFFSET);
        int freeSpaceOffset = buffer.getInt(Constants.FREE_SPACE_OFFSET);

        int recordSize = recordData.length;
        int slotOffset = Constants.HEADER_SIZE + (slotCount * 8);

        // Check if there's enough space (Slot size + Record size)
        if (slotOffset + 8 + recordSize > freeSpaceOffset) {
            return -1; // No space
        }

        int newFreeSpaceOffset = freeSpaceOffset - recordSize;
        
        // Write record data
        System.arraycopy(recordData, 0, getData(), newFreeSpaceOffset, recordSize);

        // Write slot
        buffer.putInt(slotOffset, newFreeSpaceOffset);
        buffer.putInt(slotOffset + 4, recordSize);

        // Update header
        buffer.putInt(Constants.SLOT_COUNT_OFFSET, slotCount + 1);
        buffer.putInt(Constants.FREE_SPACE_OFFSET, newFreeSpaceOffset);
        
        setDirty(true);
        return slotCount;
    }

    public byte[] getRecord(int slotId) {
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        int slotCount = buffer.getInt(Constants.SLOT_COUNT_OFFSET);
        if (slotId < 0 || slotId >= slotCount) {
            return null;
        }

        int slotOffset = Constants.HEADER_SIZE + (slotId * 8);
        int recordOffset = buffer.getInt(slotOffset);
        int recordLength = buffer.getInt(slotOffset + 4);

        if (recordOffset == -1) return null; // Tombstone

        byte[] recordData = new byte[recordLength];
        System.arraycopy(getData(), recordOffset, recordData, 0, recordLength);
        return recordData;
    }

    public boolean updateRecord(int slotId, byte[] recordData) {
        // For simplicity, we only update if it fits in the same slot (or we'd need to shift data)
        // A real impl might move data or mark as deleted and insert new.
        // Let's implement delete + insert style or just overwrite if same size.
        
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        int slotOffset = Constants.HEADER_SIZE + (slotId * 8);
        int currentLength = buffer.getInt(slotOffset + 4);

        if (recordData.length == currentLength) {
            int recordOffset = buffer.getInt(slotOffset);
            System.arraycopy(recordData, 0, getData(), recordOffset, recordData.length);
            setDirty(true);
            return true;
        }
        
        // Else: delete and re-insert? (simplified update)
        deleteRecord(slotId);
        // Note: this doesn't reclaim space in this simple version.
        // A real engine would have a compaction process.
        return false; // Indicating it didn't fit in-place or we handle it via engine layer
    }

    public void deleteRecord(int slotId) {
        ByteBuffer buffer = ByteBuffer.wrap(getData());
        int slotOffset = Constants.HEADER_SIZE + (slotId * 8);
        buffer.putInt(slotOffset, -1); // Mark as tombstone
        setDirty(true);
    }

    public int getSlotCount() {
        return ByteBuffer.wrap(getData()).getInt(Constants.SLOT_COUNT_OFFSET);
    }
}
