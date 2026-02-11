package org.bad.db.index;

import java.util.ArrayList;
import java.util.List;

public class LeafNode<K extends Comparable<K>, V> extends BNode<K, V> {
    private final List<V> values;

    public LeafNode() {
        super();
        this.isLeaf = true;
        this.values = new ArrayList<>();
    }

    public void insert(K key, V value) {
        int pos = 0;
        while (pos < keys.size() && keys.get(pos).compareTo(key) < 0) {
            pos++;
        }
        keys.add(pos, key);
        values.add(pos, value);
    }

    public V search(K key) {
        int pos = keys.indexOf(key);
        return pos == -1 ? null : values.get(pos);
    }

    public List<V> getValues() { return values; }
}
