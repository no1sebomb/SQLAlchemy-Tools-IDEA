package com.noisebomb.sqlalchemy.sql.types

import com.noisebomb.sqlalchemy.sql.SqlDialect

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

    /**
     * Root SQLAlchemy docs page for [dialect]. Used to build per-type anchor URLs of the
     * shape `<DOCS_URL>#<def.docs.cls>` for the hint popup and inline `#id` markdown links.
     *
     * Dialects that don't yet have their own docs page return `null` — callers should treat
     * that as "no link" and gracefully degrade to plain text.
     */
    fun docsUrlFor(dialect: SqlDialect): String? = when (dialect) {
        SqlDialect.GENERIC -> GenericColumnTypes.DOCS_URL
        SqlDialect.POSTGRESQL -> PostgresqlColumnTypes.DOCS_URL
        else -> null
    }
}
