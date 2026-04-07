package org.baddb.index;

import java.util.ArrayList;
import java.util.List;

/**
 * Node structure for the B-Tree index.
 * Maps Primary Key (int) -> File Offset (long).
 */
public class BTreeNode {
    private final int t; 
    private final List<Integer> keys; 
    private final List<Long> values; 
    private final List<BTreeNode> children;
    private boolean leaf;

    public BTreeNode(int t, boolean leaf) {
        this.t = t;
        this.leaf = leaf;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public List<Integer> getKeys() { return keys; }
    public List<Long> getValues() { return values; }
    public List<BTreeNode> getChildren() { return children; }
    public boolean isLeaf() { return leaf; }
    public void setLeaf(boolean leaf) { this.leaf = leaf; }

    public Long search(int k) {
        int i = 0;
        while (i < keys.size() && k > keys.get(i)) {
            i++;
        }
        if (i < keys.size() && keys.get(i) == k) {
            return values.get(i);
        }
        if (leaf) {
            return null;
        }
        return children.get(i).search(k);
    }

    public void insertNonFull(int k, long offset) {
        int i = keys.size() - 1;
        if (leaf) {
            while (i >= 0 && keys.get(i) > k) {
                i--;
            }
            keys.add(i + 1, k);
            values.add(i + 1, offset);
        } else {
            while (i >= 0 && keys.get(i) > k) {
                i--;
            }
            i++;
            if (children.get(i).getKeys().size() == 2 * t - 1) {
                splitChild(i, children.get(i));
                if (keys.get(i) < k) {
                    i++;
                }
            }
            children.get(i).insertNonFull(k, offset);
        }
    }

    public void splitChild(int i, BTreeNode y) {
        BTreeNode z = new BTreeNode(y.t, y.leaf);
        for (int j = 0; j < t - 1; j++) {
            z.keys.add(y.keys.remove(t));
            z.values.add(y.values.remove(t));
        }
        if (!y.leaf) {
            for (int j = 0; j < t; j++) {
                z.children.add(y.children.remove(t));
            }
        }
        children.add(i + 1, z);
        keys.add(i, y.keys.remove(t - 1));
        values.add(i, y.values.remove(t - 1));
    }
}
