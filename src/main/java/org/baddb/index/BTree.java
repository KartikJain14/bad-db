package org.baddb.index;

import java.util.Map;
import java.util.TreeMap;

/**
 * In-memory B-Tree for indexing Primary Keys to File Offsets.
 */
public class BTree {
    private BTreeNode root;
    private final int t;

    public BTree(int t) {
        this.t = t;
        this.root = new BTreeNode(t, true);
    }

    public Long search(int key) {
        return root == null ? null : root.search(key);
    }

    public void insert(int key, long offset) {
        if (root.getKeys().size() == 2 * t - 1) {
            BTreeNode s = new BTreeNode(t, false);
            s.getChildren().add(root);
            s.splitChild(0, root);
            int i = 0;
            if (s.getKeys().get(0) < key) {
                i++;
            }
            s.getChildren().get(i).insertNonFull(key, offset);
            root = s;
        } else {
            root.insertNonFull(key, offset);
        }
    }

    public boolean contains(int key) {
        return search(key) != null;
    }

    public void upsert(int key, long offset) {
        remove(key);
        insert(key, offset);
    }

    public boolean remove(int key) {
        Map<Integer, Long> entries = snapshot();
        if (entries.remove(key) == null) {
            return false;
        }
        rebuild(entries);
        return true;
    }

    public Map<Integer, Long> snapshot() {
        Map<Integer, Long> entries = new TreeMap<>();
        if (root != null) {
            root.collectEntries(entries);
        }
        return entries;
    }

    public void clear() {
        root = new BTreeNode(t, true);
    }

    private void rebuild(Map<Integer, Long> entries) {
        clear();
        for (Map.Entry<Integer, Long> entry : entries.entrySet()) {
            insert(entry.getKey(), entry.getValue());
        }
    }
}
