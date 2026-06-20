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
    val DOCS_URL = "https://docs.sqlalchemy.org/en/21/dialects/postgresql.html"

    // https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.ARRAY
    val ARRAY = ColumnTypeDefinition(
        id = "pg_array",
        name = "Array",
        parameters = ARRAY_LIKE_PARAMS,

        sqlalchemyTypeName = "ARRAY",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.dialects.postgresql", "ARRAY"),
        ),

        annotation = ARRAY_ANNOTATION,

        supersedes = "array",
        dialect = SqlDialect.POSTGRESQL,

        docs = Docs(
            cls = "sqlalchemy.dialects.postgresql.ARRAY",
            text = """
                The [`ARRAY`](#pg_array) type is constructed in the same way as the core [`ARRAY`](#array) type; 
                a member type is required, and a number of dimensions is recommended if the type is to be used for more than one dimension:
                ```python
                from sqlalchemy.dialects import postgresql

                mytable = Table(
                    "mytable",
                    metadata,
                    Column("data", postgresql.ARRAY(Integer, dimensions=2)),
                )
                ```
                
                The [`ARRAY`](#pg_array) type provides all operations defined on the core [`ARRAY`](#array) type, including support for “dimensions”, indexed access, and simple matching such as 
                [`Comparator.any()`](#array.Comparator.any) and 
                [`Comparator.all()`](#array.Comparator.all). 
                ARRAY class also provides PostgreSQL-specific methods for containment operations, including 
                [`Comparator.contains()`](#pg_array.Comparator.contains),
                [`Comparator.contained_by()`](#pg_array.Comparator.contained_by), and 
                [`Comparator.overlap()`](#pg_array.Comparator.overlap), e.g.:
                ```python
                mytable.c.data.contains([1, 2])
                ```

                Indexed access is one-based by default, to match that of PostgreSQL; for zero-based indexed access, 
                set [`ARRAY.zero_indexes`](#pg_array.params.zero_indexes).

                Additionally, the [`ARRAY`](#pg_array) type does not work directly in conjunction with the [`ENUM`](#enum) type. 
                For a workaround, see the special type at [Using ENUM with ARRAY](https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#postgresql-array-of-enum).
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.BIT
    val BIT = ColumnTypeDefinition(
        id = "pg_bit",
        name = "BIT",

        sqlalchemyTypeName = "BIT",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.dialects.postgresql", "BIT"),
        ),

        annotation = STR_ANNOTATION,

        dialect = SqlDialect.POSTGRESQL,

        sqlAliases = listOf(
            SqlAlias("BIT"),
        ),

        docs = Docs(
            cls = "sqlalchemy.dialects.postgresql.BIT",
            text = """
                Represent the PostgreSQL BIT type.

                The [`BIT`](#pg_bit) type yields values in the form of the [`BitString`](#pg_bit_string) Python value type.
            """.trimIndent()
        ),
    )

    // https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.JSONB
    val JSONB = ColumnTypeDefinition(
        id = "pg_jsonb",
        name = "JSONB",
        parameters = listOf(
            NONE_AS_NULL_PARAM,
        ),

        sqlalchemyTypeName = "JSONB",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.dialects.postgresql", "JSONB"),
        ),

        annotation = DICT_ANNOTATION,

        dialect = SqlDialect.POSTGRESQL,

        sqlAliases = listOf(
            SqlAlias("JSONB"),
        ),

        docs = Docs(
            cls = "sqlalchemy.dialects.postgresql.JSONB",
            text = """
                Represent the PostgreSQL JSONB type.

                The [`JSONB`](#pg_jsonb) type stores arbitrary JSONB format data, e.g.:
                ```python
                data_table = Table(
                    "data_table",
                    metadata,
                    Column("id", Integer, primary_key=True),
                    Column("data", JSONB),
                )
                
                with engine.connect() as conn:
                    conn.execute(
                        data_table.insert(), data={"key1": "value1", "key2": "value2"}
                    )
                ```
                
                The [`JSONB`](#pg_jsonb) type includes all operations provided by [`JSON`](#json), including the same behaviors for indexing operations. 
                It also adds additional operators specific to JSONB, including 
                [`Comparator.has_key()`](#pg_jsonb.Comparator.has_key), [`Comparator.has_all()`](#pg_jsonb.Comparator.has_all), 
                [`Comparator.has_any()`](#pg_jsonb.Comparator.has_any), [`Comparator.contains()`](#pg_jsonb.Comparator.contains), 
                [`Comparator.contained_by()`](#pg_jsonb.Comparator.contained_by), [`Comparator.delete_path()`](#pg_jsonb.Comparator.delete_path), 
                [`Comparator.path_exists()`](#pg_jsonb.Comparator.path_exists) and [`Comparator.path_match()`](#pg_jsonb.Comparator.path_match).
                
                Like the [`JSON`](#json) type, the [`JSONB`](#pg_jsonb) type does not detect in-place changes when used with the ORM, 
                unless the [`sqlalchemy.ext.mutable`](https://docs.sqlalchemy.org/en/21/orm/extensions/mutable.html#module-sqlalchemy.ext.mutable) extension is used.
                
                Custom serializers and deserializers are shared with the [`JSON`](#json) class, using the `json_serializer` and `json_deserializer` keyword arguments. 
                These must be specified at the dialect level using [`create_engine()`](https://docs.sqlalchemy.org/en/21/core/engines.html#sqlalchemy.create_engine). 
                When using psycopg2, the serializers are associated with the jsonb type using `psycopg2.extras.register_default_jsonb` on a per-connection basis, 
                in the same way that `psycopg2.extras.register_default_json` is used to register these handlers with the json type.
            """,
        ),
    )

    val ALL: List<ColumnTypeDefinition> = listOf(
        ARRAY, BIT, JSONB,
    )
}
