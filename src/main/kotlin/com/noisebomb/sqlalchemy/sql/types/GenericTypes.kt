package com.noisebomb.sqlalchemy.sql.types

/**
 * Generic SQLAlchemy column types (the "CamelCase" set in the SQLAlchemy docs plus the
 * cross-vendor UPPERCASE types). These are dialect-agnostic and act as the fallback when no
 * dialect-specific equivalent exists.
 *
 * Each definition declares [SqlAlias]es covering the raw DDL type names commonly used by SQL
 * dialects — those are consumed by the SQL importer when it's wired in.
 *
 * Doc bodies are stored verbatim and rendered as Markdown by the description popover.
 */
object GenericColumnTypes {

    // ---- Generic CamelCase --------------------------------------------------
    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#generic-camelcase-types

    val BIGINTEGER = ColumnTypeDefinition(
        id = "biginteger",
        name = "BigInteger",
        description = """
            A type for bigger `int` integers.

            Typically generates a `BIGINT` in DDL, and otherwise acts like a normal Integer on the Python side.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.BigInteger",
        sqlalchemyTypeName = "BigInteger",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "BigInteger")),
        annotation = INT_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("BIGINT"),
            SqlAlias("INT8"),
            SqlAlias("BIGSERIAL"),
            SqlAlias("SERIAL8"),
        ),
    )

    val BOOLEAN = ColumnTypeDefinition(
        id = "boolean",
        name = "Boolean",
        description = """
            A bool datatype.

            Boolean typically uses `BOOLEAN` or `SMALLINT` on the DDL side,
            and on the Python side deals in `True` or `False`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Boolean",
        sqlalchemyTypeName = "Boolean",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Boolean")),
        annotation = BOOL_ANNOTATION,
        sqlAliases = listOf(SqlAlias("BOOLEAN"), SqlAlias("BOOL"), SqlAlias("BIT")),
    )

    val DATE = ColumnTypeDefinition(
        id = "date",
        name = "Date",
        description = """
            A type for `datetime.date()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Date",
        sqlalchemyTypeName = "Date",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Date")),
        annotation = DATE_ANNOTATION,
        sqlAliases = listOf(SqlAlias("DATE")),
    )

    val DATETIME = ColumnTypeDefinition(
        id = "datetime",
        name = "DateTime",
        parameters = listOf(
            TIMEZONE_PARAM,
        ),
        description = """
            A type for `datetime.datetime()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.DateTime",
        sqlalchemyTypeName = "DateTime",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "DateTime")),
        annotation = DATETIME_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("DATETIME"),
            SqlAlias("DATETIME2"),
            SqlAlias("SMALLDATETIME"),
            SqlAlias("TIMESTAMP"),
        ),
    )

    val DOUBLE = ColumnTypeDefinition(
        id = "double",
        name = "Double",
        parameters = listOf(
            PRECISION_PARAM,
            asDecimalParam(default = false),
            DECIMAL_RETURN_SCALE_PARAM,
        ),
        description = """
            A type for double `FLOAT` floating point types.

            Typically generates a `DOUBLE` or `DOUBLE_PRECISION` in DDL,
            and otherwise acts like a normal Float on the Python side.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Double",
        sqlalchemyTypeName = "Double",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Double")),
        annotation = DECIMAL_OR_FLOAT_ANNOTATION,
        sqlAliases = listOf(SqlAlias("DOUBLE"), SqlAlias("DOUBLE PRECISION"), SqlAlias("FLOAT8")),
    )

    val FLOAT = ColumnTypeDefinition(
        id = "float",
        name = "Float",
        parameters = listOf(
            PRECISION_PARAM,
            asDecimalParam(default = false),
            DECIMAL_RETURN_SCALE_PARAM,
        ),
        description = """
            Type representing floating point types, such as `FLOAT` or `REAL`.

            This type returns Python `float` objects by default, unless the `Float.asdecimal`
            flag is set to True, in which case they are coerced to `decimal.Decimal` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Float",
        sqlalchemyTypeName = "Float",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Float")),
        annotation = DECIMAL_OR_FLOAT_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("FLOAT"),
            SqlAlias("REAL"),
            SqlAlias("FLOAT4"),
            SqlAlias("BINARY_FLOAT"),
            SqlAlias("BINARY_DOUBLE"),
        ),
    )

    val INTEGER = ColumnTypeDefinition(
        id = "integer",
        name = "Integer",
        description = """
            A type for `int` integers.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Integer",
        sqlalchemyTypeName = "Integer",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Integer")),
        annotation = INT_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("INT"),
            SqlAlias("INTEGER"),
            SqlAlias("INT4"),
            SqlAlias("SERIAL"),
            SqlAlias("MEDIUMINT"),
        ),
    )

    val INTERVAL = ColumnTypeDefinition(
        id = "interval",
        name = "Interval",
        parameters = listOf(
            BooleanParameter(
                id = "native",
                label = "Native",
                description = """
                    When True, use the actual INTERVAL type provided by the database, if supported
                    (currently PostgreSQL, Oracle Database). Otherwise, represent the interval data
                    as an epoch value regardless.
                """.trimIndent(),
                defaultValue = true,
            ),
            IntParameter(
                id = "second_precision",
                label = "Second precision",
                description = "Fractional seconds precision (PostgreSQL / Oracle).",
                positional = false,
                min = 0,
                optional = true,
            ),
            IntParameter(
                id = "day_precision",
                label = "Day precision",
                description = "Day precision (Oracle only).",
                positional = false,
                min = 0,
                optional = true,
            ),
        ),
        description = """
            A type for `datetime.timedelta()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Interval",
        sqlalchemyTypeName = "Interval",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Interval")),
        annotation = AnnotationResolver { _, _ ->
            AnnotationSpec(type = "timedelta", imports = listOf(ImportDefinition("datetime", "timedelta")))
        },
        sqlAliases = listOf(SqlAlias("INTERVAL")),
    )

    val LARGE_BINARY = ColumnTypeDefinition(
        id = "large_binary",
        name = "LargeBinary",
        parameters = listOf(
            IntParameter(
                id = "length",
                label = "Length",
                description = """
                    A length for the column for use in DDL statements,
                    for those binary types that accept a length, such as the MySQL BLOB type.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
        ),
        description = """
            A type for large binary byte data.

            The `LargeBinary` type corresponds to a large and/or unlengthed binary type for the
            target platform, such as BLOB on MySQL and BYTEA for PostgreSQL.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.LargeBinary",
        sqlalchemyTypeName = "LargeBinary",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "LargeBinary")),
        annotation = BYTES_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("BLOB"),
            SqlAlias("BINARY"),
            SqlAlias("VARBINARY"),
            SqlAlias("BYTEA"),
        ),
    )

    val NUMERIC = ColumnTypeDefinition(
        id = "numeric",
        name = "Numeric",
        parameters = listOf(
            PRECISION_PARAM,
            SCALE_PARAM,
            asDecimalParam(default = true),
            DECIMAL_RETURN_SCALE_PARAM,
        ),
        description = """
            Base for non-integer numeric types, such as `NUMERIC`, `FLOAT`, `DECIMAL`, and other variants.

            `Numeric` returns Python `decimal.Decimal` objects by default. If `asdecimal` is set to
            False, returned values are coerced to Python `float` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Numeric",
        sqlalchemyTypeName = "Numeric",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Numeric")),
        annotation = DECIMAL_OR_FLOAT_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("NUMERIC"),
            SqlAlias("DECIMAL"),
            SqlAlias("DEC"),
            SqlAlias("NUMBER"),
            SqlAlias("MONEY"),
            SqlAlias("SMALLMONEY"),
        ),
    )

    val PICKLE_TYPE = ColumnTypeDefinition(
        id = "pickle",
        name = "PickleType",
        // Constructor parameters (`protocol`, `pickler`, `comparator`, ...) take Python
        // callables and class references, which the param model can't usefully express today.
        description = """
            Holds Python objects, which are serialized using pickle.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.PickleType",
        sqlalchemyTypeName = "PickleType",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "PickleType")),
        annotation = AnnotationResolver { _, _ ->
            AnnotationSpec(type = "Any", imports = listOf(ImportDefinition("typing", "Any")))
        },
    )

    val SMALL_INTEGER = ColumnTypeDefinition(
        id = "small_integer",
        name = "SmallInteger",
        description = """
            A type for smaller `int` integers.

            Typically generates a `SMALLINT` in DDL, and otherwise acts like a normal Integer on the Python side.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.SmallInteger",
        sqlalchemyTypeName = "SmallInteger",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "SmallInteger")),
        annotation = INT_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("SMALLINT"),
            SqlAlias("INT2"),
            SqlAlias("SMALLSERIAL"),
            // TINYINT(1) is the MySQL/MariaDB boolean idiom; the dialect-specific Boolean overrides this.
            SqlAlias("TINYINT"),
        ),
    )

    val STRING = ColumnTypeDefinition(
        id = "string",
        name = "String",
        parameters = STRING_LIKE_PARAMS,
        description = """
            The base for all string and character types.

            In SQL, corresponds to VARCHAR.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.String",
        sqlalchemyTypeName = "String",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "String")),
        annotation = STR_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("VARCHAR"),
            SqlAlias("VARCHAR2"),
            SqlAlias("CHAR"),
            SqlAlias("STRING"),
        ),
    )

    val TEXT = ColumnTypeDefinition(
        id = "text",
        name = "Text",
        parameters = STRING_LIKE_PARAMS,
        description = """
            A variably sized string type.

            In SQL, usually corresponds to CLOB or TEXT.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Text",
        sqlalchemyTypeName = "Text",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Text")),
        annotation = STR_ANNOTATION,
        sqlAliases = listOf(SqlAlias("TEXT"), SqlAlias("CLOB")),
    )

    val TIME = ColumnTypeDefinition(
        id = "time",
        name = "Time",
        parameters = listOf(
            TIMEZONE_PARAM,
        ),
        description = """
            A type for `datetime.time()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Time",
        sqlalchemyTypeName = "Time",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Time")),
        annotation = TIME_ANNOTATION,
        sqlAliases = listOf(SqlAlias("TIME"), SqlAlias("TIMETZ")),
    )

    val UNICODE = ColumnTypeDefinition(
        id = "unicode",
        name = "Unicode",
        parameters = STRING_LIKE_PARAMS,
        description = """
            A variable length Unicode string type.

            The `Unicode` type is a `String` subclass that assumes input and output strings that may
            contain non-ASCII characters, and for some backends implies an underlying column type
            explicitly supporting of non-ASCII data, such as NVARCHAR on Oracle Database and SQL Server.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Unicode",
        sqlalchemyTypeName = "Unicode",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Unicode")),
        annotation = STR_ANNOTATION,
        sqlAliases = listOf(SqlAlias("NVARCHAR"), SqlAlias("NCHAR")),
    )

    val UNICODE_TEXT = ColumnTypeDefinition(
        id = "unicode_text",
        name = "UnicodeText",
        parameters = STRING_LIKE_PARAMS,
        description = """
            An unbounded-length Unicode string type.

            Like `Unicode`, usage the `UnicodeText` type implies a unicode-capable type
            being used on the backend, such as `NCLOB`, `NTEXT`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.UnicodeText",
        sqlalchemyTypeName = "UnicodeText",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "UnicodeText")),
        annotation = STR_ANNOTATION,
        sqlAliases = listOf(SqlAlias("NCLOB"), SqlAlias("NTEXT")),
    )

    val UUID = ColumnTypeDefinition(
        id = "uuid",
        name = "Uuid",
        parameters = listOf(
            BooleanParameter(
                id = "as_uuid",
                label = "As UUID",
                description = """
                    If True, values will be interpreted as Python uuid objects, 
                    converting to/from string via the DBAPI.
                """.trimIndent(),
                defaultValue = true,
            )
        ),
        description = """
            Represent a database agnostic UUID datatype.

            For backends that have no "native" UUID datatype, the value will make use of `CHAR(32)`
            and store the UUID as a 32-character alphanumeric hex string.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Uuid",
        sqlalchemyTypeName = "Uuid",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "Uuid")),
        annotation = AnnotationResolver { _, _ ->
            AnnotationSpec(type = "UUID", imports = listOf(ImportDefinition("uuid", "UUID")))
        },
        sqlAliases = listOf(SqlAlias("UUID"), SqlAlias("UNIQUEIDENTIFIER")),
    )

    // TODO: Enum

    // ---- Multiple-vendor UPPERCASE types -----------------------------------
    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sql-standard-and-multiple-vendor-uppercase-types

    val ARRAY = ColumnTypeDefinition(
        id = "array",
        name = "Array",
        parameters = ARRAY_LIKE_PARAMS,
        description = """
            Represent a SQL Array type.

            Note: only the PostgreSQL backend has built-in support for SQL arrays in SQLAlchemy.
            Prefer `sqlalchemy.dialects.postgresql.ARRAY` directly when targeting PostgreSQL.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY",
        sqlalchemyTypeName = "ARRAY",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "ARRAY")),
        annotation = ARRAY_ANNOTATION,
    )

    val JSON = ColumnTypeDefinition(
        id = "json",
        name = "JSON",
        parameters = listOf(
            NONE_AS_NULL_PARAM,
        ),
        description = """
            Represent a SQL JSON type.

            `JSON` is part of the Core in support of the growing popularity of native `JSON` datatypes.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.JSON",
        sqlalchemyTypeName = "JSON",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "JSON")),
        annotation = DICT_ANNOTATION,
        sqlAliases = listOf(SqlAlias("JSON")),
    )

    val ALL: List<ColumnTypeDefinition> = listOf(
        // Generic CamelCase
        BIGINTEGER, BOOLEAN, DATE, DATETIME, DOUBLE, FLOAT, INTEGER, INTERVAL,
        LARGE_BINARY, NUMERIC, PICKLE_TYPE, SMALL_INTEGER, STRING, TEXT, TIME,
        UNICODE, UNICODE_TEXT, UUID,
        // Generic UPPERCASE
        ARRAY, JSON,
    )
}
