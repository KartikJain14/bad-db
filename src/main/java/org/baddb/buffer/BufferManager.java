package org.baddb.buffer;

import org.baddb.storage.DiskManager;
import org.baddb.storage.Page;
import org.baddb.storage.SlottedPage;
import org.baddb.storage.PageType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BufferManager {
    private final DiskManager diskManager;
    private final Map<Integer, Page> pageCache;
    private final int capacity;

    public BufferManager(DiskManager diskManager, int capacity) {
        this.diskManager = diskManager;
        this.pageCache = new HashMap<>();
        this.capacity = capacity;
    }

    public Page getPage(int pageId) throws IOException {
        if (pageCache.containsKey(pageId)) {
            return pageCache.get(pageId);
        }

        if (pageCache.size() >= capacity) {
            flushAll(); // Simple strategy: flush everything when full
            pageCache.clear();
        }

        byte[] data = new byte[org.baddb.common.Constants.PAGE_SIZE];
        diskManager.readPage(pageId, data);
        
        // Wrap with appropriate Page implementation based on type in header
        PageType type = PageType.fromValue(data[org.baddb.common.Constants.PAGE_TYPE_OFFSET]);
        Page page = switch (type) {
            case SLOTTED -> new SlottedPage(pageId, data);
            // Add other types as they are implemented
            default -> new SlottedPage(pageId, data); 
        };
        
        pageCache.put(pageId, page);
        return page;
    }

    public Page createPage() throws IOException {
        int pageId = diskManager.allocatePage();
        Page page = new SlottedPage(pageId);
        pageCache.put(pageId, page);
        return page;
    }

    public void flushPage(int pageId) throws IOException {
        Page page = pageCache.get(pageId);
        if (page != null && page.isDirty()) {
            diskManager.writePage(pageId, page.getData());
            page.setDirty(false);
        }
    }

    public void flushAll() throws IOException {
        for (Page page : pageCache.values()) {
            if (page.isDirty()) {
                diskManager.writePage(page.getPageId(), page.getData());
                page.setDirty(false);
            }
        }
    }
}
