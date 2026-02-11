package org.bad.db.common;


/**
 * Record Identifier (RID) uniquely identifies a record's location
 * using it's page ID and slot index within that page.
 */
public record RID(int pageId, int slotId) {
    @Override
    public String toString() {
        return "RID(Page:" + pageId + ", Slot:" + slotId + ")";
    }
}
