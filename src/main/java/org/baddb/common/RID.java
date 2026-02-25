package org.baddb.common;

public record RID(int pageId, int slotId) {
    @Override
    public String toString() {
        return "(" + pageId + "," + slotId + ")";
    }
    
    public static RID fromString(String s) {
        String[] parts = s.substring(1, s.length() - 1).split(",");
        return new RID(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
