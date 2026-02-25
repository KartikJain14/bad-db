package org.baddb.transaction;

import org.baddb.common.Constants;
import java.nio.ByteBuffer;

public record LogRecord(LogType type, int txId, int pageId, byte[] beforeImage, byte[] afterImage) {
    public enum LogType {
        BEGIN((byte) 1),
        UPDATE((byte) 2),
        COMMIT((byte) 3),
        ABORT((byte) 4);

        private final byte value;
        LogType(byte value) { this.value = value; }
        public byte getValue() { return value; }
        public static LogType fromValue(byte v) {
            for (LogType t : values()) if (t.value == v) return t;
            return null;
        }
    }

    public byte[] serialize() {
        int size = 1 + 4; // type + txId
        if (type == LogType.UPDATE) {
            size += 4 + Constants.PAGE_SIZE * 2; // pageId + before + after
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
