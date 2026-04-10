# BQL (Bad Query Language) Tutorial

Welcome to BQL! Standard SQL wasn't doing it for us, so we created our own custom query language for the BadDB framework. This language is tailored directly to the features made available by the BadDB library.

## Getting Started

To enter the BQL Interactive Prompt, run `Main.java`, and select option `5` from the Main Menu.

## Database Initialization & Loading

### INIT
Creates and loads a new database schema. This overwrites any existing database file at the specified path.
**Syntax:** `INIT <tableName> <dbPath> <colName1>:<type1> [<colName2>:<type2> ...]`
**Supported Types:** `INT`, `FLOAT`, `STRING`, `BOOLEAN`
**Example:** `INIT Students student_records.bad id:INT name:STRING grade:FLOAT is_active:BOOLEAN`

### OPEN
Opens an existing database file based on the given path.
**Syntax:** `OPEN <dbPath>`
**Example:** `OPEN student_records.bad`

> Note: You must `INIT` or `OPEN` a database before running any of the data modification or querying commands.

## Data Modification

### INSERT
Inserts a new record. The values must match the column types exactly in the order they were provided in the `INIT` schema. The first value determines the primary key. String values with spaces are not supported natively without advanced token parsing, so use single-word strings or `"quoted strings"`.
**Syntax:** `INSERT <value1> <value2> ...`
**Example:** `INSERT 101 "Alice_Smith" 88.5 true`

### UPSERT
Inserts a new record, or updates the existing record if a record with the same primary key already exists.
**Syntax:** `UPSERT <value1> <value2> ...`
**Example:** `UPSERT 101 "Alice_Smith" 95.0 true`

### UPDATE
Updates an existing record given its primary key. The rest of the values represent the entirety of the updated record.
**Syntax:** `UPDATE <primaryKey> <value1> <value2> ...`
**Example:** `UPDATE 101 101 "Alice_Smith" 92.5 true`

### DELETE
Deletes an existing record by its primary key.
**Syntax:** `DELETE <primaryKey>`
**Example:** `DELETE 101`

## Data Querying

### SEARCH
Fetches a single record by its primary key.
**Syntax:** `SEARCH <primaryKey>`
**Example:** `SEARCH 101`

### EXISTS
Checks whether a record with the given primary key exists in the current active table.
**Syntax:** `EXISTS <primaryKey>`
**Example:** `EXISTS 101`

### COUNT
Returns the total number of non-deleted records in the database.
**Syntax:** `COUNT`
**Example:** `COUNT`

### SELECT
Returns all records where the value for `<columnName>` exactly matches `<value>`.
**Syntax:** `SELECT <columnName> <value>` OR `SELECT *` OR `SELECT ALL`
**Example:** `SELECT is_active true`

### SELECT_ALL
Returns all non-deleted records in the database.
**Syntax:** `SELECT_ALL`
**Example:** `SELECT_ALL`

## Maintenance

### COMPACT
Triggers the storage engine to reclaim deleted space in the database file and re-write the active records seamlessly.
**Syntax:** `COMPACT`
**Example:** `COMPACT`

### CLOSE
Closes the currently active database and flushes it to disk. You must run `INIT` or `OPEN` again to fire more queries.
**Syntax:** `CLOSE`
**Example:** `CLOSE`

### EXIT
Exits the BQL Interactive mode and returns safely to the Main Menu.
**Syntax:** `EXIT`
**Example:** `EXIT`
