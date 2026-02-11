package org.bad.db.storage;

import org.bad.db.common.Constants;

import java.nio.ByteBuffer;

/**
 * Basic abstraction of a fixed-size storage unit.
 */
public class Page {
    private final int pageId;
    private final byte[] data;
    private boolean isDirty;

    public Page(int pageId) {
        this.pageId = pageId;
        this.data = new byte[Constants.PAGE_SIZE];
        this.isDirty = false;
        
        // Initialize header
        ByteBuffer.wrap(data).putInt(Constants.PAGE_ID_OFFSET, pageId);
    }

    public Page(int pageId, byte[] initialData) {
        this.pageId = pageId;
        this.data = initialData;
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
}
