package org.baddb.transaction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * WALManager (Write-Ahead Log Manager) is responsible for appending log records to a persistent log file.
 * The WAL is essential for database recovery following a crash, as it stores every modification made
 * before those changes are applied to the main database file (Write-Ahead Log protocol).
 */
public class WALManager implements AutoCloseable {
    /** The output stream for appending log records to the WAL file. */
    private final FileOutputStream walStream;

    /**
     * Initializes a WAL manager with the specified log file path.
     *
     * @param walPath the path to the .wal file
     * @throws IOException if there occurs an error opening the file
     */
    public WALManager(Path walPath) throws IOException {
        // True indicates we always append to the end of the file.
        this.walStream = new FileOutputStream(walPath.toFile(), true);
    }

    /**
     * Appends a log record to the end of the WAL file.
     * This method is synchronized to ensure serial access to the log from multiple transactions.
     *
     * @param record the log entry to persist
     * @throws IOException if occurs an error writing to disk
     */
    public synchronized void log(LogRecord record) throws IOException {
        byte[] data = record.serialize();
        walStream.write(data);
    }

    /**
     * Forces the operating system to flush all buffered log data to the physical disk.
     * This is critical for ACID durability; a transaction should only be considered 'committed'
     * once its COMMIT log record is physically synced to disk.
     *
     * @throws IOException if occurs a disk sync error
     */
    public synchronized void flush() throws IOException {
        // fd.sync() ensures data is written to the physical platter/SSD, not just OS buffers.
        walStream.getFD().sync();
    }

    /**
     * Safely closes the WAL file stream.
     *
     * @throws IOException if occurs an error closing the file
     */
    @Override
    public void close() throws IOException {
        walStream.close();
    }
}
