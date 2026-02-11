package org.bad.db.storage;

import org.bad.db.common.Constants;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * DiskManager handles reading and writing of physical pages to the .db file.
 */
public class DiskManager implements AutoCloseable {
    private final RandomAccessFile dbFile;

    public DiskManager(String dbPath) throws IOException {
        this.dbFile = new RandomAccessFile(dbPath, "rw");
    }

    public void writePage(Page page) throws IOException {
        int offset = page.getPageId() * Constants.PAGE_SIZE;
        dbFile.seek(offset);
        dbFile.write(page.getData());
    }

    public Page readPage(int pageId) throws IOException {
        byte[] data = new byte[Constants.PAGE_SIZE];
        int offset = pageId * Constants.PAGE_SIZE;
        
        if (offset >= dbFile.length()) {
            return new Page(pageId); // New empty page
        }

        dbFile.seek(offset);
        dbFile.readFully(data);
        return new Page(pageId, data);
    }

    public int allocatePage() throws IOException {
        return (int) (dbFile.length() / Constants.PAGE_SIZE);
    }

    @Override
    public void close() throws IOException {
        dbFile.close();
    }
}
