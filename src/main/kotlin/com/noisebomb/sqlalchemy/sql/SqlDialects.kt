package com.noisebomb.sqlalchemy.sql


enum class SqlDialect(val displayName: String) {
    GENERIC("Generic SQL"),
    POSTGRESQL("PostgreSQL"),
    MYSQL("MySQL"),
    MARIADB("MariaDB"),
    SQL_SERVER("SQL Server"),
    ORACLE("Oracle"),
    SQLITE("SQLite");

    override fun toString(): String = displayName
}
