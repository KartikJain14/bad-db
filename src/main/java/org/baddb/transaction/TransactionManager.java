package org.baddb.transaction;

import org.baddb.buffer.BufferManager;
import org.baddb.storage.Page;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionManager {
    private final WALManager walManager;
    private final BufferManager bufferManager;
    private final AtomicInteger nextTxId;

    public TransactionManager(WALManager walManager, BufferManager bufferManager) {
        this.walManager = walManager;
        this.bufferManager = bufferManager;
        this.nextTxId = new AtomicInteger(1);
    }

    public Transaction beginTransaction() throws IOException {
        int txId = nextTxId.getAndIncrement();
        Transaction tx = new Transaction(txId);
        walManager.log(new LogRecord(LogRecord.LogType.BEGIN, txId, -1, null, null));
        return tx;
    }

    public void commit(Transaction tx) throws IOException {
        walManager.log(new LogRecord(LogRecord.LogType.COMMIT, tx.getTxId(), -1, null, null));
        walManager.flush();
        tx.setState(TransactionState.COMMITTED);
        // Flush modified pages to disk (Durability)
        for (int pageId : tx.getModifiedPages()) {
            bufferManager.flushPage(pageId);
        }
    }

    public void rollback(Transaction tx) throws IOException {
        // Roll back changes in memory using Undo logs
        List<Transaction.UndoEntry> undos = tx.getUndoLogs();
        // Undo in reverse order
        for (int i = undos.size() - 1; i >= 0; i--) {
            Transaction.UndoEntry undo = undos.get(i);
            Page page = bufferManager.getPage(undo.pageId());
            System.arraycopy(undo.beforeImage(), 0, page.getData(), 0, org.baddb.common.Constants.PAGE_SIZE);
            page.setDirty(true);
        }
        
        tx.setState(TransactionState.ABORTED);
        walManager.log(new LogRecord(LogRecord.LogType.ABORT, tx.getTxId(), -1, null, null));
        walManager.flush();
    }

    public void logUpdate(Transaction tx, int pageId, byte[] before, byte[] after) throws IOException {
        walManager.log(new LogRecord(LogRecord.LogType.UPDATE, tx.getTxId(), pageId, before, after));
        tx.addModifiedPage(pageId);
        tx.addUndo(pageId, before);
    }
}
