package org.baddb.storage;

import org.baddb.common.Constants;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class DiskManager implements AutoCloseable {
    private final RandomAccessFile dbFile;
    private final AtomicInteger nextPageId;

    public DiskManager(Path dbPath) throws IOException {
        this.dbFile = new RandomAccessFile(dbPath.toFile(), "rw");
        // Initialize nextPageId based on file length
        long length = dbFile.length();
        this.nextPageId = new AtomicInteger((int) (length / Constants.PAGE_SIZE));
    }

    public int allocatePage() throws IOException {
        int pageId = nextPageId.getAndIncrement();
        // Zero out the page on disk to reserve space
        byte[] emptyPage = new byte[Constants.PAGE_SIZE];
        writePage(pageId, emptyPage);
        return pageId;
    }

    public void readPage(int pageId, byte[] data) throws IOException {
        if (data.length != Constants.PAGE_SIZE) {
            throw new IllegalArgumentException("Data buffer must be exactly " + Constants.PAGE_SIZE + " bytes");
        }
        long offset = (long) pageId * Constants.PAGE_SIZE;
        if (offset >= dbFile.length()) {
            throw new IOException("Page " + pageId + " does not exist");
        }
        dbFile.seek(offset);
        dbFile.readFully(data);
    }

    public void writePage(int pageId, byte[] data) throws IOException {
        if (data.length != Constants.PAGE_SIZE) {
            throw new IllegalArgumentException("Data buffer must be exactly " + Constants.PAGE_SIZE + " bytes");
        }
        long offset = (long) pageId * Constants.PAGE_SIZE;
        dbFile.seek(offset);
        dbFile.write(data);
    }

    public int getPageCount() {
        return nextPageId.get();
    }

    @Override
    public void close() throws IOException {
        dbFile.close();
    }
}
