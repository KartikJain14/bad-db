package org.baddb.buffer;

import org.baddb.storage.DiskManager;
import org.baddb.storage.Page;
import org.baddb.storage.SlottedPage;
import org.baddb.storage.PageType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BufferManager is the intermediary between the database engine and the DiskManager.
 * It manages a pool of pages (pageCache) in memory to reduce expensive disk I/O operations.
 * When a page is requested, it first checks the cache; if not found, it fetches it from disk.
 *
 * Current eviction strategy: Simple flush-all when capacity is exceeded (for educational simplicity).
 */
public class BufferManager {
    /** Reference to the DiskManager for reading/writing pages. */
    private final DiskManager diskManager;
    /** Map for quick lookups of pages by their ID. */
    private final Map<Integer, Page> pageCache;
    /** Maximum number of pages allowed in memory before eviction is triggered. */
    private final int capacity;

    /**
     * Initializes a BufferManager with a given disk manager and pool capacity.
     *
     * @param diskManager the disk manager to use for I/O
     * @param capacity the maximum number of pages to keep in the cache
     */
    public BufferManager(DiskManager diskManager, int capacity) {
        this.diskManager = diskManager;
        this.pageCache = new HashMap<>();
        this.capacity = capacity;
    }

    /**
     * Retrieves a page with the given ID.
     * If already in memory, returns the cached version.
     * Otherwise, reads it from disk and caches it.
     *
     * @param pageId the ID of the page to retrieve
     * @return the requested Page object
     * @throws IOException if there occurs a disk read or allocation error
     */
    public Page getPage(int pageId) throws IOException {
        // First check if the page is already in our cache.
        if (pageCache.containsKey(pageId)) {
            return pageCache.get(pageId);
        }

        // If cache is full, we must free up space.
        // We use a simple "flush everything and clear" strategy for now.
        if (pageCache.size() >= capacity) {
            flushAll();
            pageCache.clear();
        }

        // Read page data from disk.
        byte[] data = new byte[org.baddb.common.Constants.PAGE_SIZE];
        diskManager.readPage(pageId, data);
        
        // Wrap the raw byte array with an appropriate Page implementation.
        // Looking at the header of the page data to determine its type.
        PageType type = PageType.fromValue(data[org.baddb.common.Constants.PAGE_TYPE_OFFSET]);
        Page page = switch (type) {
            case SLOTTED -> new SlottedPage(pageId, data);
            // Default to SlottedPage for generic/header roles as they are implemented.
            default -> new SlottedPage(pageId, data); 
        };
        
        // Add to our cache.
        pageCache.put(pageId, page);
        return page;
    }

    /**
     * Creates a brand new page on disk and returns its memory representation.
     *
     * @return the newly created Page
     * @throws IOException if there occurs a disk allocation error
     */
    public Page createPage() throws IOException {
        // Allocate a new ID through disk manager.
        int pageId = diskManager.allocatePage();
        // Create an empty memory representation of a SlottedPage (common for record storage).
        Page page = new SlottedPage(pageId);
        // Track it in our cache so subsequent reads within the same run are fast.
        pageCache.put(pageId, page);
        return page;
    }

    /**
     * Write a specific page back to disk if it has been modified (dirty).
     *
     * @param pageId the ID of the page to flush
     * @throws IOException if there occurs a disk write error
     */
    public void flushPage(int pageId) throws IOException {
        Page page = pageCache.get(pageId);
        if (page != null && page.isDirty()) {
            diskManager.writePage(pageId, page.getData());
            // Once written to disk, it is no longer "dirty" relative to the disk state.
            page.setDirty(false);
        }
    }

    /**
     * Flushes all modified pages in the buffer pool back to disk.
     * This ensures all outstanding changes are persisted.
     *
     * @throws IOException if there occurs an error writing any page
     */
    public void flushAll() throws IOException {
        for (Page page : pageCache.values()) {
            if (page.isDirty()) {
                diskManager.writePage(page.getPageId(), page.getData());
                page.setDirty(false);
            }
        }
    }
}
