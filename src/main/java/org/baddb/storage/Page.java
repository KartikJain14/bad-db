package org.baddb.storage;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

public abstract class Page {
    protected final byte[] data;
    protected final int pageId;
    protected boolean isDirty;

    public Page(int pageId) {
        this.data = new byte[Constants.PAGE_SIZE];
        this.pageId = pageId;
        this.isDirty = false;
        
        // Initialize header
        setPageId(pageId);
    }

    public Page(int pageId, byte[] data) {
        this.data = data;
        this.pageId = pageId;
        this.isDirty = false;
    }

    public int getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    protected void setPageId(int pageId) {
        ByteBuffer.wrap(data).putInt(Constants.PAGE_ID_OFFSET, pageId);
    }

    public PageType getType() {
        return PageType.fromValue(data[Constants.PAGE_TYPE_OFFSET]);
    }

    protected void setType(PageType type) {
        data[Constants.PAGE_TYPE_OFFSET] = type.getValue();
    }

    public short getFreeSpacePointer() {
        return ByteBuffer.wrap(data).getShort(Constants.FREE_SPACE_OFFSET);
    }

    protected void setFreeSpacePointer(short pointer) {
        ByteBuffer.wrap(data).putShort(Constants.FREE_SPACE_OFFSET, pointer);
    }

    public short getSlotCount() {
        return ByteBuffer.wrap(data).getShort(Constants.SLOT_COUNT_OFFSET);
    }

    protected void setSlotCount(short count) {
        ByteBuffer.wrap(data).putShort(Constants.SLOT_COUNT_OFFSET, count);
    }

    public int getNextPageId() {
        return ByteBuffer.wrap(data).getInt(Constants.NEXT_PAGE_OFFSET);
    }

    public void setNextPageId(int pageId) {
        ByteBuffer.wrap(data).putInt(Constants.NEXT_PAGE_OFFSET, pageId);
    }
}
