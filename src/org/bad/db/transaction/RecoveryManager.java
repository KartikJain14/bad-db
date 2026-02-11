package org.bad.db.transaction;

import org.bad.db.storage.BufferManager;
import org.bad.db.storage.Page;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecoveryManager {
    private final LogManager logManager;
    private final BufferManager bufferManager;

    public RecoveryManager(LogManager logManager, BufferManager bufferManager) {
        this.logManager = logManager;
        this.bufferManager = bufferManager;
    }

    public void recover() throws IOException {
        List<LogRecord> logs = logManager.readAllLogs();
        Set<Long> committedTxns = new HashSet<>();
        
        // Phase 1: Identify committed transactions
        for (LogRecord record : logs) {
            if (record.type == LogRecord.Type.COMMIT) {
                committedTxns.add(record.txnId);
            }
        }

        // Phase 2: Redo committed transactions
        for (LogRecord record : logs) {
            if (record.type == LogRecord.Type.UPDATE && committedTxns.contains(record.txnId)) {
                Page page = bufferManager.getPage(record.pageId);
                System.arraycopy(record.afterData, 0, page.getData(), 0, record.afterData.length);
                page.setDirty(true);
            }
        }
        
        bufferManager.flushAll();
    }
}
