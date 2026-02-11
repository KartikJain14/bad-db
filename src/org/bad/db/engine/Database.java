package org.bad.db.engine;

import org.bad.db.catalog.Catalog;
import org.bad.db.catalog.Schema;
import org.bad.db.catalog.TableMetadata;
import org.bad.db.storage.BufferManager;
import org.bad.db.storage.DiskManager;
import org.bad.db.transaction.LogManager;
import org.bad.db.transaction.RecoveryManager;
import org.bad.db.transaction.Transaction;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Database class is the central engine coordinator.
 */
public class Database implements AutoCloseable {
    private final DiskManager diskManager;
    private final BufferManager bufferManager;
    private final Catalog catalog;
    private final LogManager logManager;
    private final AtomicLong nextTxnId;

    public Database(String dbName) throws IOException {
        this.diskManager = new DiskManager(dbName + ".db");
        this.bufferManager = new BufferManager(diskManager);
        this.logManager = new LogManager(dbName + ".log");
        
        // Reserve Page 0 for Catalog if new DB
        if (diskManager.allocatePage() == 0) {
            bufferManager.allocatePage(); 
        }
        
        this.catalog = new Catalog(bufferManager);
        this.nextTxnId = new AtomicLong(1);

        // Crash Recovery
        RecoveryManager recoveryManager = new RecoveryManager(logManager, bufferManager);
        recoveryManager.recover();
    }

    public Table createTable(String name, Schema schema) throws IOException {
        if (catalog.getTable(name) != null) {
            return new Table(catalog.getTable(name), bufferManager);
        }
        
        // Allocate first page for the table
        int firstPageId = bufferManager.allocatePage();
        TableMetadata metadata = new TableMetadata(name, schema, firstPageId);
        catalog.addTable(metadata);
        
        return new Table(metadata, bufferManager);
    }

    public Table getTable(String name) {
        TableMetadata metadata = catalog.getTable(name);
        return (metadata != null) ? new Table(metadata, bufferManager) : null;
    }

    public Transaction beginTransaction() throws IOException {
        return new Transaction(nextTxnId.getAndIncrement(), logManager);
    }

    public void commit(Transaction txn) throws IOException {
        txn.commit();
        bufferManager.flushAll(); // Ensure durability on commit
    }

    public void rollback(Transaction txn) throws IOException {
        txn.abort();
        // Undo changes in-memory by discarding dirty pages
        for (org.bad.db.storage.Page page : txn.getWriteSet()) {
            bufferManager.discardPage(page.getPageId());
        }
    }

    @Override
    public void close() throws IOException {
        bufferManager.flushAll();
        logManager.close();
        diskManager.close();
    }
}
