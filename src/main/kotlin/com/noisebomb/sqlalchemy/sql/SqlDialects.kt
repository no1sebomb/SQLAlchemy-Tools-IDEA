package com.noisebomb.sqlalchemy.sql


/**
 * @property displayName Human-readable label shown in the dialect combo.
 * @property sqlalchemyModule Python module that exposes this dialect's column types.
 *   Per-type SQLAlchemy imports are derived as `from $sqlalchemyModule import $sqlalchemyTypeName`
 *   so individual type definitions no longer have to repeat the module path.
 */
enum class SqlDialect(val displayName: String, val sqlalchemyModule: String) {
    GENERIC("Generic SQL", "sqlalchemy.types"),
    POSTGRESQL("PostgreSQL", "sqlalchemy.dialects.postgresql"),
    MYSQL("MySQL", "sqlalchemy.dialects.mysql"),
    // MariaDB shares the MySQL dialect's Python module in SQLAlchemy.
    MARIADB("MariaDB", "sqlalchemy.dialects.mysql"),
    SQL_SERVER("SQL Server", "sqlalchemy.dialects.mssql"),
    ORACLE("Oracle", "sqlalchemy.dialects.oracle"),
    SQLITE("SQLite", "sqlalchemy.dialects.sqlite");

    override fun toString(): String = displayName
}
