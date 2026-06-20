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

    // PostgreSQL Data Types
    // https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#postgresql-data-types

    val ARRAY = ColumnTypeDefinition(
        id = "pg_array",
        name = "Array",
        parameters = ARRAY_LIKE_PARAMS,
        description = """
            PostgreSQL ARRAY type.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY",
        sqlalchemyTypeName = "ARRAY",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "ARRAY")),
        annotation = ARRAY_ANNOTATION,
        supersedes = "array",
    )

    val BIT = ColumnTypeDefinition(
        id = "pg_bit",
        name = "BIT",
        description = """
            Represent the PostgreSQL BIT type.

            The `BIT` type yields values in the form of the `BitString` Python value type.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.BIT",
        sqlalchemyTypeName = "BIT",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.dialects.postgresql", "BIT")),
        annotation = STR_ANNOTATION,
        dialect = SqlDialect.POSTGRESQL,
        sqlAliases = listOf(SqlAlias("BIT")),
    )

    val JSONB = ColumnTypeDefinition(
        id = "pg_jsonb",
        name = "JSONB",
        parameters = listOf(
            NONE_AS_NULL_PARAM,
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

    val ALL: List<ColumnTypeDefinition> = listOf(
        ARRAY, BIT, JSONB,
    )
}
