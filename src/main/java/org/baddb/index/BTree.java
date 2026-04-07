package org.baddb.index;

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
}
