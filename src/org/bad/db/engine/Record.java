package org.bad.db.engine;

import org.bad.db.common.RID;
import java.util.Map;

/**
 * High-level representation of a database record.
 */
public class Record {
    private final RID rid;
    private final Map<String, Object> values;

    public Record(RID rid, Map<String, Object> values) {
        this.rid = rid;
        this.values = values;
    }

    public RID getRid() { return rid; }
    public Map<String, Object> getValues() { return values; }
    public Object getValue(String column) { return values.get(column); }

    @Override
    public String toString() {
        return "Record" + (rid != null ? rid : "(new)") + " " + values;
    }
}
