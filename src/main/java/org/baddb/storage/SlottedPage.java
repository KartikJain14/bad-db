package org.baddb.storage;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

public class SlottedPage extends Page {
    private static final int SLOT_SIZE = 4; // 2 bytes offset, 2 bytes size

    public SlottedPage(int pageId) {
        super(pageId);
        setType(PageType.SLOTTED);
        setFreeSpacePointer((short) Constants.PAGE_SIZE);
        setSlotCount((short) 0);
    }

    public SlottedPage(int pageId, byte[] data) {
        super(pageId, data);
        if (getType() == PageType.INVALID) {
            setType(PageType.SLOTTED);
            setFreeSpacePointer((short) Constants.PAGE_SIZE);
            setSlotCount((short) 0);
        }
    }

    public int insertRecord(byte[] record) {
        short size = (short) record.length;
        short slotCount = getSlotCount();
        short freeSpacePointer = getFreeSpacePointer();
        
        // Calculate needed space: slot entry (4) + record size
        int neededSpace = SLOT_SIZE + size;
        int usedSpaceHeader = Constants.PAGE_HEADER_SIZE + (slotCount * SLOT_SIZE);
        
        if (freeSpacePointer - usedSpaceHeader < neededSpace) {
            return -1; // No space
        }

        // 1. Move free space pointer up
        short newOffset = (short) (freeSpacePointer - size);
        setFreeSpacePointer(newOffset);

        // 2. Write record data
        System.arraycopy(record, 0, data, newOffset, size);

        // 3. Update slot directory
        int slotId = slotCount;
        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort(slotOffset, newOffset);
        buffer.putShort(slotOffset + 2, size);

        // 4. Increment slot count
        setSlotCount((short) (slotCount + 1));
        setDirty(true);

        return slotId;
    }

    public byte[] getRecord(int slotId) {
        if (slotId < 0 || slotId >= getSlotCount()) return null;

        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short offset = buffer.getShort(slotOffset);
        short size = buffer.getShort(slotOffset + 2);

        if (offset == 0) return null; // Tombstone or empty

        byte[] record = new byte[size];
        System.arraycopy(data, offset, record, 0, size);
        return record;
    }

    public boolean updateRecord(int slotId, byte[] newRecord) {
        if (slotId < 0 || slotId >= getSlotCount()) return false;

        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // short oldOffset = buffer.getShort(slotOffset);
        short oldSize = buffer.getShort(slotOffset + 2);

        if (newRecord.length == oldSize) {
            // In-place update
            short offset = buffer.getShort(slotOffset);
            System.arraycopy(newRecord, 0, data, offset, oldSize);
            setDirty(true);
            return true;
        }

        // For now, if size differs, we don't handle fragmentation (simplified)
        // A real DB would mark as deleted and re-insert, or compact.
        // Let's implement simple fragmentation for now: mark old as 0 and insert new if space.
        // Actually, just returning false for now to keep it simple as per "lite" requirement.
        return false; 
    }

    public void deleteRecord(int slotId) {
        if (slotId < 0 || slotId >= getSlotCount()) return;

        int slotOffset = Constants.PAGE_HEADER_SIZE + (slotId * SLOT_SIZE);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort(slotOffset, (short) 0); // Mark as deleted
        buffer.putShort(slotOffset + 2, (short) 0);
        setDirty(true);
    }
}
