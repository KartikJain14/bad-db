package org.bad.db.transaction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LogManager implements AutoCloseable {
    private final File logFile;
    private final DataOutputStream dos;

    public LogManager(String logPath) throws IOException {
        this.logFile = new File(logPath);
        FileOutputStream fos = new FileOutputStream(logFile, true);
        this.dos = new DataOutputStream(fos);
    }

    public synchronized void appendLog(LogRecord record) throws IOException {
        dos.writeInt(record.type.ordinal());
        dos.writeLong(record.txnId);
        dos.writeInt(record.pageId);
        
        if (record.beforeData != null) {
            dos.writeInt(record.beforeData.length);
            dos.write(record.beforeData);
        } else {
            dos.writeInt(0);
        }
        
        if (record.afterData != null) {
            dos.writeInt(record.afterData.length);
            dos.write(record.afterData);
        } else {
            dos.writeInt(0);
        }
        dos.flush();
    }

    public List<LogRecord> readAllLogs() throws IOException {
        List<LogRecord> logs = new ArrayList<>();
        if (!logFile.exists() || logFile.length() == 0) return logs;

        try (FileInputStream fis = new FileInputStream(logFile);
             DataInputStream dis = new DataInputStream(fis)) {
            while (fis.available() > 0) {
                try {
                    int typeOrd = dis.readInt();
                    long txnId = dis.readLong();
                    int pageId = dis.readInt();
                    
                    int bLen = dis.readInt();
                    byte[] before = null;
                    if (bLen > 0) {
                        before = new byte[bLen];
                        dis.readFully(before);
                    }
                    
                    int aLen = dis.readInt();
                    byte[] after = null;
                    if (aLen > 0) {
                        after = new byte[aLen];
                        dis.readFully(after);
                    }
                    
                    logs.add(new LogRecord(LogRecord.Type.values()[typeOrd], txnId, pageId, before, after));
                } catch (EOFException e) {
                    break;
                }
            }
        }
        return logs;
    }

    @Override
    public void close() throws IOException {
        dos.close();
    }
}
