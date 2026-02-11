package org.bad.db.transaction;

import java.io.Serializable;

public class LogRecord implements Serializable {
    public enum Type { BEGIN, COMMIT, ABORT, UPDATE }
    
    public final Type type;
    public final long txnId;
    public final int pageId;
    public final byte[] beforeData;
    public final byte[] afterData;

    public LogRecord(Type type, long txnId, int pageId, byte[] before, byte[] after) {
        this.type = type;
        this.txnId = txnId;
        this.pageId = pageId;
        this.beforeData = before;
        this.afterData = after;
    }
}
