package org.baddb.engine;

import org.baddb.buffer.BufferManager;
import org.baddb.catalog.CatalogManager;
import org.baddb.catalog.Schema;
import org.baddb.catalog.TableMetadata;
import org.baddb.common.Constants;
import org.baddb.storage.DiskManager;
import org.baddb.transaction.RecoveryManager;
import org.baddb.transaction.Transaction;
import org.baddb.transaction.TransactionManager;
import org.baddb.transaction.WALManager;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Database implements AutoCloseable {
    private final DiskManager diskManager;
    private final BufferManager bufferManager;
    private final CatalogManager catalogManager;
    private final TransactionManager txManager;
    private final WALManager walManager;

    public Database(String dbName) throws IOException {
        Path dbPath = Paths.get(dbName + Constants.DB_EXTENSION);
        Path walPath = Paths.get(dbName + Constants.WAL_EXTENSION);

        this.diskManager = new DiskManager(dbPath);
        this.bufferManager = new BufferManager(diskManager, 100);
        this.walManager = new WALManager(walPath);
        this.txManager = new TransactionManager(walManager, bufferManager);

        // Run recovery
        RecoveryManager recovery = new RecoveryManager(walPath, diskManager);
        recovery.recover();

        this.catalogManager = new CatalogManager(bufferManager);
    }

    public void createTable(String name, Schema schema) throws IOException {
        // Find or create root page for the table
        int rootPageId = diskManager.allocatePage();
        catalogManager.createTable(name, schema, rootPageId);
        bufferManager.flushAll();
    }

    public Table getTable(String name) {
        TableMetadata meta = catalogManager.getTableMetadata(name);
        if (meta == null) return null;
        return new Table(meta, bufferManager, txManager);
    }

    public Transaction beginTransaction() throws IOException {
        return txManager.beginTransaction();
    }

    public void commit(Transaction tx) throws IOException {
        txManager.commit(tx);
    }

    public void rollback(Transaction tx) throws IOException {
        txManager.rollback(tx);
    }

    @Override
    public void close() throws IOException {
        bufferManager.flushAll();
        walManager.close();
        diskManager.close();
    }
}
