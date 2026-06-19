package com.noisebomb.sqlalchemy.sql.types

import com.noisebomb.sqlalchemy.sql.SqlDialect

/**
 * PostgreSQL-specific column types from `sqlalchemy.dialects.postgresql`.
 *
 * When the active dialect is [SqlDialect.POSTGRESQL], the registry shows these in addition
 * to generics — set [ColumnTypeDefinition.supersedes] to hide a generic equivalent when
 * the dialect-specific one is a strict replacement.
 */
object PostgresqlColumnTypes {

    val JSONB = ColumnTypeDefinition(
        id = "pg_jsonb",
        name = "JSONB",
        parameters = listOf(
            BooleanParameter(
                id = "none_as_null",
                label = "None as null",
                description = """
                    If True, persist the value None as a SQL NULL value, not the JSON encoding of `null`.
                """.trimIndent(),
                defaultValue = false,
            ),
        ),
        description = """
            Represent the PostgreSQL JSONB type.

            JSONB includes everything JSON does, plus PostgreSQL-specific operators
            (`has_key`, `has_all`, `has_any`, `contains`, `contained_by`, `delete_path`,
            `path_exists`, `path_match`).
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.JSONB",
        sqlalchemyTypeName = "JSONB",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.dialects.postgresql", "JSONB")),
        annotation = DICT_ANNOTATION,
        dialect = SqlDialect.POSTGRESQL,
        sqlAliases = listOf(SqlAlias("JSONB")),
        // JSONB and generic JSON coexist on PG (different operators), so we do NOT supersede.
    )

    val ALL: List<ColumnTypeDefinition> = listOf(JSONB)
}
