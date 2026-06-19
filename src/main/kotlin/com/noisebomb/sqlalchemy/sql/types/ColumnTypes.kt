package com.noisebomb.sqlalchemy.sql.types

/**
 * Singleton entry point for all built-in [ColumnTypeDefinition]s.
 *
 * Per-dialect definitions live in their own files ([GenericColumnTypes],
 * [PostgresqlColumnTypes], ...). Add a new dialect by:
 *   1. creating a `<Dialect>Types.kt` file that exposes an `ALL` list, and
 *   2. appending that list below.
 */
object ColumnTypes {
    val REGISTRY: ColumnTypeRegistry = ColumnTypeRegistry(
        GenericColumnTypes.ALL +
            PostgresqlColumnTypes.ALL,
    )
}
