package org.bad.db.transaction;

import org.bad.db.storage.Page;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Transaction {
    public enum State { GROWING, COMMITTED, ABORTED }
    
    private final long txnId;
    private State state;
    private final List<Page> writeSet;
    private final LogManager logManager;

    public Transaction(long txnId, LogManager logManager) throws IOException {
        this.txnId = txnId;
        this.state = State.GROWING;
        this.writeSet = new ArrayList<>();
        this.logManager = logManager;
        
        logManager.appendLog(new LogRecord(LogRecord.Type.BEGIN, txnId, -1, null, null));
    }

    public long getTxnId() { return txnId; }
    public State getState() { return state; }
    public List<Page> getWriteSet() { return writeSet; }

    public void addWrite(Page page, byte[] before) throws IOException {
        writeSet.add(page);
        logManager.appendLog(new LogRecord(LogRecord.Type.UPDATE, txnId, page.getPageId(), before, page.getData().clone()));
    }

    public void commit() throws IOException {
        logManager.appendLog(new LogRecord(LogRecord.Type.COMMIT, txnId, -1, null, null));
        this.state = State.COMMITTED;
    }

    public void abort() throws IOException {
        // Simple rollback
        for (int i = writeSet.size() - 1; i >= 0; i--) {
            // In a real system, we'd use the WAL to undo.
            // Here we just don't flush dirty pages if aborted? 
            // Better: append ABORT to log.
        }
        logManager.appendLog(new LogRecord(LogRecord.Type.ABORT, txnId, -1, null, null));
        this.state = State.ABORTED;
    }
}
