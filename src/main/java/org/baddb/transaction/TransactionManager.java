package org.baddb.transaction;

import org.baddb.buffer.BufferManager;
import org.baddb.storage.Page;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TransactionManager coordinates the lifecycle of transactions.
 * It ensures that all modifications are logged to the WAL before being applied,
 * and handles the logic for committing or rolling back (aborting) changes.
 */
public class TransactionManager {
    /** The Write-Ahead Log manager for persisting log records. */
    private final WALManager walManager;
    /** The buffer manager for flushing committed pages to disk. */
    private final BufferManager bufferManager;
    /** Atomic counter to generate unique transaction IDs. */
    private final AtomicInteger nextTxId;

    /**
     * Initializes a TransactionManager.
     *
     * @param walManager the WAL manager to use
     * @param bufferManager the buffer manager to use
     */
    public TransactionManager(WALManager walManager, BufferManager bufferManager) {
        this.walManager = walManager;
        this.bufferManager = bufferManager;
        this.nextTxId = new AtomicInteger(1);
    }

    /**
     * Starts a new transaction.
     * Generates a new ID and writes a BEGIN log record to the WAL.
     *
     * @return the newly created Transaction object
     * @throws IOException if occurs an error writing to the WAL
     */
    public Transaction beginTransaction() throws IOException {
        int txId = nextTxId.getAndIncrement();
        Transaction tx = new Transaction(txId);
        // Step 1: Log the start of the transaction.
        walManager.log(new LogRecord(LogRecord.LogType.BEGIN, txId, -1, null, null));
        return tx;
    }

    /**
     * Commits a transaction, making all its changes permanent.
     *
     * @param tx the transaction to commit
     * @throws IOException if occurs an error writing to the WAL or flushing pages
     */
    public void commit(Transaction tx) throws IOException {
        // Step 1: Write COMMIT record to WAL.
        walManager.log(new LogRecord(LogRecord.LogType.COMMIT, tx.getTxId(), -1, null, null));
        // Step 2: Ensure the WAL is physically on disk before telling the user the transaction is done.
        walManager.flush();
        
        tx.setState(TransactionState.COMMITTED);

        // Step 3: Flush modified pages to disk to satisfy Durability (simplified "Force" policy).
        for (int pageId : tx.getModifiedPages()) {
            bufferManager.flushPage(pageId);
        }
    }

    /**
     * Aborts a transaction and rolls back any changes it made to memory.
     *
     * @param tx the transaction to rollback
     * @throws IOException if occurs an error reading pages or writing to the WAL
     */
    public void rollback(Transaction tx) throws IOException {
        // Step 1: Revert changes in memory using the transaction's private Undo logs.
        List<Transaction.UndoEntry> undos = tx.getUndoLogs();
        // We MUST undo in reverse order of the original operations to correctly restore state.
        for (int i = undos.size() - 1; i >= 0; i--) {
            Transaction.UndoEntry undo = undos.get(i);
            Page page = bufferManager.getPage(undo.pageId());
            // Restore the 'beforeImage' to the page in the buffer pool.
            System.arraycopy(undo.beforeImage(), 0, page.getData(), 0, org.baddb.common.Constants.PAGE_SIZE);
            page.setDirty(true);
        }
        
        tx.setState(TransactionState.ABORTED);

        // Step 2: Log the ABORT in the WAL so recovery knows this tx failed.
        walManager.log(new LogRecord(LogRecord.LogType.ABORT, tx.getTxId(), -1, null, null));
        walManager.flush();
    }

    /**
     * Logs an update operation to the WAL and registers it with the transaction.
     *
     * @param tx the transaction performing the update
     * @param pageId ID of the page being modified
     * @param before copy of the page data before the update
     * @param after copy of the page data after the update
     * @throws IOException if occurs an error writing to the WAL
     */
    public void logUpdate(Transaction tx, int pageId, byte[] before, byte[] after) throws IOException {
        // Step 1: Persist the update to the WAL (Write-Ahead).
        walManager.log(new LogRecord(LogRecord.LogType.UPDATE, tx.getTxId(), pageId, before, after));
        // Step 2: Track the modification in the transaction object for commit/rollback logic.
        tx.addModifiedPage(pageId);
        tx.addUndo(pageId, before);
    }
}
