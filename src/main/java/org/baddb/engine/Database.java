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

/**
 * Database is the primary entry point for the Bad-DB engine.
 * it orchestrates all core components: storage, buffering, cataloging, and transactions.
 * It also handles the startup recovery process to ensure data integrity after a crash.
 */
public class Database implements AutoCloseable {
    private final DiskManager diskManager;
    private final BufferManager bufferManager;
    private final CatalogManager catalogManager;
    private final TransactionManager txManager;
    private final WALManager walManager;

    /**
     * Initializes the database engine.
     * This constructor assembles all internal managers and triggers the crash recovery process.
     *
     * @param dbName the base name for the database files (.db and .wal)
     * @throws IOException if occurs an error initializing files or during recovery
     */
    public Database(String dbName) throws IOException {
        // Construct file paths for the main data file and the Write-Ahead Log.
        Path dbPath = Paths.get(dbName + Constants.DB_EXTENSION);
        Path walPath = Paths.get(dbName + Constants.WAL_EXTENSION);

        // 1. Initialize Storage and Buffering layers.
        this.diskManager = new DiskManager(dbPath);
        this.bufferManager = new BufferManager(diskManager, 100); // 100 pages = ~400KB cache
        
        // 2. Initialize Logging and Transaction layers.
        this.walManager = new WALManager(walPath);
        this.txManager = new TransactionManager(walManager, bufferManager);

        // 3. CRITICAL: Run recovery BEFORE allowing any new operations.
        // This ensures uncommitted changes from a previous run are rolled back.
        RecoveryManager recovery = new RecoveryManager(walPath, diskManager);
        recovery.recover();

        // 4. Initialize the Catalog (after recovery has ensured Page 0 is consistent).
        this.catalogManager = new CatalogManager(bufferManager);
    }

    /**
     * Creates a new table in the database.
     *
     * @param name unique name for the table
     * @param schema the column definition for the table
     * @throws IOException if the table exists or if occurs a disk error
     */
    public void createTable(String name, Schema schema) throws IOException {
        // Allocate the first data page (root) for this table's heap file.
        int rootPageId = diskManager.allocatePage();
        catalogManager.createTable(name, schema, rootPageId);
        // Persist the catalog update immediately.
        bufferManager.flushAll();
    }

    /**
     * Retrieves an accessor for an existing table.
     *
     * @param name name of the table to look up
     * @return a Table object for performing operations, or null if not found
     */
    public Table getTable(String name) {
        TableMetadata meta = catalogManager.getTableMetadata(name);
        if (meta == null) return null;
        return new Table(meta, bufferManager, txManager);
    }

    /** @return a new active transaction */
    public Transaction beginTransaction() throws IOException {
        return txManager.beginTransaction();
    }

    /** Commits the given transaction. */
    public void commit(Transaction tx) throws IOException {
        txManager.commit(tx);
    }

    /** Rolls back the given transaction. */
    public void rollback(Transaction tx) throws IOException {
        txManager.rollback(tx);
    }

    /**
     * Safely shuts down the database engine.
     * Ensures all pending changes are flushed to disk.
     */
    @Override
    public void close() throws IOException {
        bufferManager.flushAll();
        walManager.close();
        diskManager.close();
    }
}
