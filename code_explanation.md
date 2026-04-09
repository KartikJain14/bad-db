# BadDB: Detailed Code Breakdown for Beginners

This document provides a line-by-line explanation of the most important parts of the code. It is designed to help anyone understand how a binary database engine works from scratch.

---

## 🏗️ 1. DataType.java
`DataType` is an **enum** (short for enumeration). It is a special class that holds a fixed set of constants.
```java
public enum DataType {
    INT,      // Integer (4 bytes)
    FLOAT,    // Fractional numbers (4 bytes)
    STRING,   // Text (Variable size, handled via writeUTF)
    BOOLEAN   // True or False (1 byte)
}
```
**Why do we need this?**
When we read a stream of bytes from a file, we need to know whether the next 4 bytes are supposed to be an `int` or a `float`. The `DataType` tells the program how to interpret the binary data.

---

## 📂 2. DatabaseFileManager.java
This class is the "heart" of the storage engine. It talks directly to the hard drive using `RandomAccessFile`.

### RandomAccessFile vs. FileInputStream
*   `FileInputStream` can only read from the beginning to the end.
*   `RandomAccessFile` can "jump" (seek) to any byte offset in the file instantly. This is crucial for performance.

### Function: `createNewDatabase`
This function formats the file with three sections:
1.  **Header**: Magic Number ('BKJ20'), Version, and Table Count.
2.  **Schema**: The table name, column count, and for each column, its name and type.
3.  **Data**: This section is left empty at the start and grows as we append records.

raf.writeUTF(tableName);      // Stores the name as a string
```

### Function: `readMetadata` (Binary Reconstruction)
This function reads the file header back into memory. It confirms that the file starts with the correct **Magic Number** ('BKJ20'). It then processes the binary data to reconstruct your `Schema` object. This is what allows you to load an existing database without telling the program which columns to expect—it finds them in the file!

### Function: `appendRecord`
This function adds a new row to the end of the file.
```java
long offset = raf.length(); // Get the current size (the END of the file)
raf.seek(offset);           // Move the "pointer" to the end
```
The **offset** is carefully saved. It is the "address" of the record on the disk. We give this offset to the B-Tree for indexing.

---

## 🌳 3. B-Tree (Indexing Logic)
The B-Tree is an **in-memory** data structure that stores `(Key -> Offset)` pairs. 

### Why do we "Rebuild" it?
Since the index is in-memory for performance, it is lost when you close the program. When you re-open a database, the code performs an **Index Rebuild**. It scans every record in the binary file from start to finish, extracts the ID, and re-maps it to its exact byte offset in the B-Tree. This ensures that even after a restart, your primary key searches remain ultra-fast.

---

## 🏛️ 4. Table.java
The `Table` class brings everything together. It handles both the `Schema` and the `B-Tree` index.

### Function: `select(columnName, value)`
This is a high-level function that mimics a SQL `WHERE` clause.
1.  **Find the Column**: It first loops through the schema to find which index the `columnName` refers to.
2.  **Optimize (B-Tree)**: If you are searching for the **Primary Key** (column 0), it asks the B-Tree for the offset. This is instant.
3.  **Fallback (Scan)**: If you search for any other column (like `name`), it must read **all** records from the file one by one until it finds a match. This is called a "Sequential Scan."

```java
// Check if we can use the index
if (colIndex == 0 && value instanceof Integer) {
    Record r = searchByPrimaryKey((Integer) value); // FAST!
    if (r != null) results.add(r);
    return results;
}
```

---

## 📜 5. Main.java
This is the entry point. It demonstrates how to build a table and query it.
```java
Table studentTable = new Table("Students", studentSchema, dbPath);
studentTable.initialize(); // Creates common file format
```
The `student_records.bad` file is generated in the root directory. You can even open it with a hex editor to see the binary data!

---

## 💡 Summary of Operation
1.  **Create**: Set up the file structure.
2.  **Insert**: Write binary data to the disk + Add (ID, Position) to the B-Tree.
3.  **Search**: Look up ID in the B-Tree -> Get Position -> "Seek" directly to that byte -> Read.
