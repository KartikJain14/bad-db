# Implementation Plan: Bad Database Storage Engine

This plan outlines the design and implementation of a university-level database storage engine using Java. The focus is on clean OOP principles, binary file storage using `RandomAccessFile`, and a basic in-memory B-Tree index for performance.

## 1. System Architecture

The project will follow a modular design to ensure explainability and clarity.

### Data Model
* **`DataType`**: Enum for supported types (INT, FLOAT, STRING, BOOLEAN).
* **`Column`**: Defines column properties (name, type).
* **`Schema`**: Collection of columns defining table structure.
* **`Record`**: Holds field values corresponding to a schema.

### Storage Engine
* **`DatabaseFileManager`**: Handles raw binary I/O using `RandomAccessFile`. Implements the file structure:
    * Header (Magic Number, Version, Table Count)
    * Schema Section (Metadata for each table)
    * Data Section (Sequential records)
* **`Table`**: High-level manager connecting the schema to the data. Uses `BTree` for primary key lookups.

### Indexing
* **`BTree`** & **`BTreeNode`**: In-memory structure mapping Primary Key -> File Offset.

## 2. File Format Specification

The binary file will be structured as follows:

| Section | Content | Bytes/Format |
| :--- | :--- | :--- |
| **Header** | Magic Number | 4 bytes (e.g., 'DB01') |
| | Version | 4 bytes (int) |
| | Table Count | 4 bytes (int) |
| **Schema** | Table Name | `writeUTF` |
| | Column Count | `writeInt` |
| | [Column Name] | `writeUTF` |
| | [Data Type] | `writeInt` (Ordinal) |
| **Data** | [Record Data] | Sequential `writeInt`, `writeFloat`, etc. |

## 3. Implementation Steps

1. **Step 1: Core Data Classes**: Implement `DataType`, `Column`, `Schema`, and `Record`.
2. **Step 2: Indexing System**: Implement a simple `BTree` (order 3 or 4) that stores key-offset pairs.
3. **Step 3: File Management**: Implement `DatabaseFileManager` for low-level byte manipulation.
4. **Step 4: Table Orchestration**: Implement `Table` class to handle `insertRecord`, `getAllRecords`, and `searchByPrimaryKey`.
5. **Step 5: Demonstration**: Create a `Main` class to showcase the features:
    * Create a database.
    * Create a table.
    * Insert records.
    * Sequential scan.
    * Fast search using B-Tree index.

## 4. Key OOP Design Principles

* **Encapsulation**: All file-specific logic is hidden within `DatabaseFileManager`.
* **Abstraction**: `Table` provides a simple interface for database operations.
* **Maintainability**: Clear separation between storage format and logical data representation.
