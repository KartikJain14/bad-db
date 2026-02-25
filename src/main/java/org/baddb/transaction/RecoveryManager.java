package org.baddb.transaction;

import org.baddb.storage.DiskManager;
import org.baddb.common.Constants;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class RecoveryManager {
    private final Path walPath;
    private final DiskManager diskManager;

    public RecoveryManager(Path walPath, DiskManager diskManager) {
        this.walPath = walPath;
        this.diskManager = diskManager;
    }

    public void recover() throws IOException {
        File walFile = walPath.toFile();
        if (!walFile.exists()) return;

        Set<Integer> committedTxs = new HashSet<>();
        List<LogRecord> updates = new ArrayList<>();

        // Phase 1: Scan for committed transactions
        try (DataInputStream dis = new DataInputStream(new FileInputStream(walFile))) {
            while (dis.available() > 0) {
                byte typeVal = dis.readByte();
                int txId = dis.readInt();
                LogRecord.LogType type = LogRecord.LogType.fromValue(typeVal);

                if (type == LogRecord.LogType.COMMIT) {
                    committedTxs.add(txId);
                } else if (type == LogRecord.LogType.UPDATE) {
                    int pageId = dis.readInt();
                    byte[] before = new byte[Constants.PAGE_SIZE];
                    byte[] after = new byte[Constants.PAGE_SIZE];
                    dis.readFully(before);
                    dis.readFully(after);
                    updates.add(new LogRecord(type, txId, pageId, before, after));
                }
            }
        }

        // Phase 2: Redo committed, Undo uncommitted
        // Redo from start
        for (LogRecord record : updates) {
            if (committedTxs.contains(record.txId())) {
                diskManager.writePage(record.pageId(), record.afterImage());
            } else {
                // Undo uncommitted
                diskManager.writePage(record.pageId(), record.beforeImage());
            }
        }
    }
}
