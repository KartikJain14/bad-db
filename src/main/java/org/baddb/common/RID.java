package org.baddb.common;

/**
 * RID (Record Identifier) is a unique reference to a specific record in the database.
 * It consists of:
 * 1. pageId - The ID of the page where the record is located.
 * 2. slotId - The index within that page's slot directory.
 * Together, they provide O(1) direct access to any record on disk.
 */
public record RID(int pageId, int slotId) {
    /** @return a string representation of the RID in (pageId,slotId) format */
    @Override
    public String toString() {
        return "(" + pageId + "," + slotId + ")";
    }
    
    /**
     * Reconstructs an RID from its string representation.
     *
     * @param s the string to parse (e.g., "(10,5)")
     * @return the RID object
     */
    public static RID fromString(String s) {
        String[] parts = s.substring(1, s.length() - 1).split(",");
        return new RID(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
