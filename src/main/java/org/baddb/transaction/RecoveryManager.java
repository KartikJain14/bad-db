package org.baddb.transaction;

import org.baddb.storage.DiskManager;
import org.baddb.common.Constants;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * RecoveryManager implements a simple ARIES-style recovery algorithm.
 * When the database starts up, it scans the WAL to identify which transactions committed
 * before the crash and which were in-flight.
 * - Transactions with a COMMIT record are "redone" (REDO).
 * - Transactions without a COMMIT record are "undone" (UNDO).
 */
public class RecoveryManager {
    /** Path to the Write-Ahead Log file. */
    private final Path walPath;
    /** The DiskManager to apply recovery changes directly to the database file. */
    private final DiskManager diskManager;

    /**
     * Initializes a RecoveryManager.
     *
     * @param walPath path to the log file to analyze
     * @param diskManager disk manager to apply fixes
     */
    public RecoveryManager(Path walPath, DiskManager diskManager) {
        this.walPath = walPath;
        this.diskManager = diskManager;
    }

    /**
     * Performs the recovery process.
     * 1. Scans the WAL to find all COMMIT markers.
     * 2. Replays (REDO) updates for committed transactions.
     * 3. Reverts (UNDO) updates for any uncommitted transactions found in the log.
     *
     * @throws IOException if occurs a disk read or write error during recovery
     */
    public void recover() throws IOException {
        File walFile = walPath.toFile();
        if (!walFile.exists()) return;

        // Keep track of which transactions actually finished successfully.
        Set<Integer> committedTxs = new HashSet<>();
        // Temporary list to hold update records for re-processing.
        List<LogRecord> updates = new ArrayList<>();

        // Phase 1: Analysis Pass.
        // We read through the log to understand the state of the database at the time of the crash.
        try (DataInputStream dis = new DataInputStream(new FileInputStream(walFile))) {
            while (dis.available() > 0) {
                byte typeVal = dis.readByte();
                int txId = dis.readInt();
                LogRecord.LogType type = LogRecord.LogType.fromValue(typeVal);

                if (type == LogRecord.LogType.COMMIT) {
                    committedTxs.add(txId);
                } else if (type == LogRecord.LogType.UPDATE) {
                    // For UPDATE records, we must read the page ID and the full snapshots.
                    int pageId = dis.readInt();
                    byte[] before = new byte[Constants.PAGE_SIZE];
                    byte[] after = new byte[Constants.PAGE_SIZE];
                    dis.readFully(before);
                    dis.readFully(after);
                    updates.add(new LogRecord(type, txId, pageId, before, after));
                }
                // (Optional: handle BEGIN/ABORT if needed for more complex recovery)
            }
        }

        // Phase 2: Redo/Undo Pass.
        // We iterate through all logged updates and apply them based on the transaction's fate.
        for (LogRecord record : updates) {
            if (committedTxs.contains(record.txId())) {
                // REDO: The transaction committed, so ensure its 'afterImage' is on disk.
                diskManager.writePage(record.pageId(), record.afterImage());
            } else {
                // UNDO: The transaction did NOT commit (or crashed mid-way), so restore the 'beforeImage'.
                diskManager.writePage(record.pageId(), record.beforeImage());
            }
        }
        
        // After recovery, the database file should be in a consistent state.
    }
}
