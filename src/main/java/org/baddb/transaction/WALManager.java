package org.baddb.transaction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class WALManager implements AutoCloseable {
    private final FileOutputStream walStream;

    public WALManager(Path walPath) throws IOException {
        this.walStream = new FileOutputStream(walPath.toFile(), true);
    }

    public synchronized void log(LogRecord record) throws IOException {
        byte[] data = record.serialize();
        walStream.write(data);
    }

    public synchronized void flush() throws IOException {
        walStream.getFD().sync();
    }

    @Override
    public void close() throws IOException {
        walStream.close();
    }
}
