package org.bad.db.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BufferManager manages a cache of pages in memory.
 */
public class BufferManager {
    private final DiskManager diskManager;
    private final Map<Integer, Page> pageCache;

    private int nextAvailablePageId = -1;

    public BufferManager(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.pageCache = new HashMap<>();
        this.nextAvailablePageId = diskManager.allocatePage();
    }

    public Page getPage(int pageId) throws IOException {
        if (pageCache.containsKey(pageId)) {
            return pageCache.get(pageId);
        }
        Page page = diskManager.readPage(pageId);
        pageCache.put(pageId, page);
        return page;
    }

    public void flushPage(int pageId) throws IOException {
        Page page = pageCache.get(pageId);
        if (page != null && page.isDirty()) {
            diskManager.writePage(page);
            page.setDirty(false);
        }
    }

    public void discardPage(int pageId) {
        pageCache.remove(pageId);
    }

    public void flushAll() throws IOException {
        for (Page page : pageCache.values()) {
            if (page.isDirty()) {
                diskManager.writePage(page);
                page.setDirty(false);
            }
        }
    }

    public int allocatePage() throws IOException {
        int pageId = nextAvailablePageId++;
        Page page = new Page(pageId);
        pageCache.put(pageId, page);
        return pageId;
    }
}
