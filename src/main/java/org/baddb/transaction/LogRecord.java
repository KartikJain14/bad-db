package org.baddb.transaction;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

/**
 * Represents a single entry in the Write-Ahead Log (WAL).
 * Log records are used to ensure ACID properties: 
 * - 'beforeImage' allows UNDO (atomicity)
 * - 'afterImage' allows REDO (durability)
 *
 * @param type the type of operation (BEGIN, UPDATE, COMMIT, ABORT)
 * @param txId the unique ID of the transaction that created this log
 * @param pageId the ID of the page being modified (only for UPDATE)
 * @param beforeImage the content of the page BEFORE the update (for UNDO)
 * @param afterImage the content of the page AFTER the update (for REDO)
 */
public record LogRecord(LogType type, int txId, int pageId, byte[] beforeImage, byte[] afterImage) {
    /** Possible types for a log record in the WAL. */
    public enum LogType {
        /** Marks the start of a transaction. */
        BEGIN((byte) 1),
        /** Records a change to a specific page. */
        UPDATE((byte) 2),
        /** Marks the successful completion of a transaction. */
        COMMIT((byte) 3),
        /** Marks that a transaction was rolled back. */
        ABORT((byte) 4);

        private final byte value;
        LogType(byte value) { this.value = value; }
        public byte getValue() { return value; }
        public static LogType fromValue(byte v) {
            for (LogType t : values()) if (t.value == v) return t;
            return null;
        }
    }

    /**
     * Serializes the log record into a byte array for writing to the WAL file.
     *
     * @return the serialized bytes
     */
    public byte[] serialize() {
        // Calculate total size: 1 byte for type, 4 bytes for transaction ID.
        int size = 1 + 4; 
        // For UPDATE, we also store the page ID and the full before/after images of the page.
        if (type == LogType.UPDATE) {
            size += 4 + Constants.PAGE_SIZE * 2;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(type.getValue());
        buffer.putInt(txId);
        if (type == LogType.UPDATE) {
            buffer.putInt(pageId);
            buffer.put(beforeImage);
            buffer.put(afterImage);
        }
        return buffer.array();
    }
}
