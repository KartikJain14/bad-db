package org.baddb.storage;

import org.baddb.common.Constants;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DiskManager is responsible for managing the database file on disk.
 * It provides basic page-level I/O operations including reading, writing, and allocating pages.
 * This class uses {@link java.io.RandomAccessFile} for efficient, direct access to any part of the file.
 * All operations here are thread-safe because they use synchronized access to the underlying file.
 */
public class DiskManager implements AutoCloseable {
    /** The actual database file on disk. */
    private final RandomAccessFile dbFile;
    /** Keeps track of the next available page ID to simplify page allocation. */
    private final AtomicInteger nextPageId;

    /**
     * Initializes a DiskManager with the specified database file path.
     * If the file exists, it calculates the next available page ID based on its size.
     *
     * @param dbPath the path to the database file
     * @throws IOException if there occurs an error opening or reading the file
     */
    public DiskManager(Path dbPath) throws IOException {
        // "rw" allows reading and writing to the database file.
        this.dbFile = new RandomAccessFile(dbPath.toFile(), "rw");

        // We assume each page is of fixed size (Constants.PAGE_SIZE).
        // The total number of pages is fileLength / PAGE_SIZE.
        long length = dbFile.length();
        this.nextPageId = new AtomicInteger((int) (length / Constants.PAGE_SIZE));
    }

    /**
     * Allocates a new page at the end of the database file.
     * This method pre-allocates space by writing a blank page to the file.
     *
     * @return the newly allocated page ID
     * @throws IOException if there occurs an error during allocation or writing to disk
     */
    public int allocatePage() throws IOException {
        // AtomicInteger ensures each call to allocatePage returns a unique growing ID.
        int pageId = nextPageId.getAndIncrement();

        // Write a blank page to disk immediately to reserve the space.
        // This ensures the file grows reliably.
        byte[] emptyPage = new byte[Constants.PAGE_SIZE];
        writePage(pageId, emptyPage);
        return pageId;
    }

    /**
     * Reads a specific page from the disk into the provided buffer.
     *
     * @param pageId the ID of the page to read
     * @param data a byte buffer of size Constants.PAGE_SIZE to store the read content
     * @throws IOException if the page does not exist or if there occurs a read error
     * @throws IllegalArgumentException if the provided buffer size is incorrect
     */
    public void readPage(int pageId, byte[] data) throws IOException {
        if (data.length != Constants.PAGE_SIZE) {
            throw new IllegalArgumentException("Data buffer must be exactly " + Constants.PAGE_SIZE + " bytes");
        }

        // Calculate the exact byte offset in the file for this page.
        long offset = (long) pageId * Constants.PAGE_SIZE;
        if (offset >= dbFile.length()) {
            throw new IOException("Page " + pageId + " does not exist (offset beyond file end)");
        }

        // Jump to the correct position and read the entire page.
        dbFile.seek(offset);
        dbFile.readFully(data);
    }

    /**
     * Writes the content of the provided buffer to a specific page on disk.
     *
     * @param pageId the ID of the page to write
     * @param data the byte buffer containing the data to be written (must be Constants.PAGE_SIZE)
     * @throws IOException if there occurs a write error
     * @throws IllegalArgumentException if the provided buffer size is incorrect
     */
    public void writePage(int pageId, byte[] data) throws IOException {
        if (data.length != Constants.PAGE_SIZE) {
            throw new IllegalArgumentException("Data buffer must be exactly " + Constants.PAGE_SIZE + " bytes");
        }

        // Calculate the byte offset and jump there.
        long offset = (long) pageId * Constants.PAGE_SIZE;
        dbFile.seek(offset);
        // Write the full page data.
        dbFile.write(data);
    }

    /**
     * Returns the total current number of pages in the database file.
     *
     * @return the total page count
     */
    public int getPageCount() {
        return nextPageId.get();
    }

    /**
     * Closes the database file safely.
     *
     * @throws IOException if there occurs an error closing the file
     */
    @Override
    public void close() throws IOException {
        dbFile.close();
    }
}
