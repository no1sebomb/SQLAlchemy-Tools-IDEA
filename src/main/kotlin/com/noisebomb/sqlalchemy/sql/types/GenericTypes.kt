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
    val DOCS_URL = "https://docs.sqlalchemy.org/en/21/core/type_basics.html"

    // TODO: Fix breaks in docs.text

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.BigInteger
    val BIGINTEGER = ColumnTypeDefinition(
        id = "biginteger",
        name = "BigInteger",

        sqlalchemyTypeName = "BigInteger",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "BigInteger"),
        ),

        annotation = INT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("BIGINT"),
            SqlAlias("INT8"),
            SqlAlias("BIGSERIAL"),
            SqlAlias("SERIAL8"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.BigInteger",
            text = """
                A type for bigger `int` integers.

                Typically generates a `BIGINT` in DDL, and otherwise acts like a normal [`Integer`](#integer) on the Python side.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Boolean
    val BOOLEAN = ColumnTypeDefinition(
        id = "boolean",
        name = "Boolean",

        sqlalchemyTypeName = "Boolean",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Boolean"),
        ),

        annotation = BOOL_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("BOOLEAN"),
            SqlAlias("BOOL"),
            SqlAlias("BIT"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Boolean",
            text = """
                A bool datatype.

                [`Boolean`](#boolean) typically uses BOOLEAN or SMALLINT on the DDL side, and on the Python side deals in `True` or `False`.
                
                The [`Boolean`](#boolean) datatype currently has two levels of assertion that the values persisted are simple true/false values. 
                For all backends, only the Python values `None`, `True`, `False`, `1` or `0` are accepted as parameter values. 
                For those backends that don’t support a “native boolean” datatype, an option exists to also create a CHECK constraint on the target column
            """
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Date
    val DATE = ColumnTypeDefinition(
        id = "date",
        name = "Date",

        sqlalchemyTypeName = "Date",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Date"),
        ),

        annotation = DATE_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("DATE"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Date",
            text = """
                A type for `datetime.date()` objects.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.DateTime
    val DATETIME = ColumnTypeDefinition(
        id = "datetime",
        name = "DateTime",
        parameters = listOf(
            TIMEZONE_PARAM,
        ),

        sqlalchemyTypeName = "DateTime",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "DateTime"),
        ),

        annotation = DATETIME_ANNOTATION,
        sqlAliases = listOf(
            SqlAlias("DATETIME"),
            SqlAlias("DATETIME2"),
            SqlAlias("SMALLDATETIME"),
            SqlAlias("TIMESTAMP"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.DateTime",
            text = """
                A type for `datetime.datetime()` objects.
                
                Date and time types return objects from the Python `datetime` module. 
                Most DBAPIs have built in support for the datetime module, with the noted exception of SQLite. 
                In the case of SQLite, date and time types are stored as strings which are then converted back to datetime objects when rows are returned.

                For the time representation within the datetime type, some backends include additional options, such as 
                timezone support and fractional seconds support. For fractional seconds, use the dialect-specific datatype, 
                such as [`TIME`](#time). For timezone support, use at least the [`TIMESTAMP`](#timestamp) datatype, if not the dialect-specific datatype object.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Double
    val DOUBLE = ColumnTypeDefinition(
        id = "double",
        name = "Double",
        parameters = listOf(
            PRECISION_PARAM,
            asDecimalParam(default = false),
            DECIMAL_RETURN_SCALE_PARAM,
        ),

        sqlalchemyTypeName = "Double",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Double"),
        ),

        annotation = DECIMAL_OR_FLOAT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("DOUBLE"),
            SqlAlias("DOUBLE PRECISION"),
            SqlAlias("FLOAT8"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Double",
            text = """
                A type for double `FLOAT` floating point types.
                
                Typically generates a `DOUBLE` or `DOUBLE_PRECISION` in DDL,
                and otherwise acts like a normal [`Float`](#float) on the Python side.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Float
    val FLOAT = ColumnTypeDefinition(
        id = "float",
        name = "Float",
        parameters = listOf(
            PRECISION_PARAM,
            asDecimalParam(default = false),
            DECIMAL_RETURN_SCALE_PARAM,
        ),

        sqlalchemyTypeName = "Float",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Float"),
        ),

        annotation = DECIMAL_OR_FLOAT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("FLOAT"),
            SqlAlias("REAL"),
            SqlAlias("FLOAT4"),
            SqlAlias("BINARY_FLOAT"),
            SqlAlias("BINARY_DOUBLE"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Float",
            text = """
                Type representing floating point types, such as `FLOAT` or `REAL`.
    
                This type returns Python `float` objects by default, unless the [`Float.asdecimal`](#float.params.asdecimal)
                flag is set to `True`, in which case they are coerced to `decimal.Decimal` objects.
                
                When a [`Float.precision`](#float.params.precision) is not provided in a [`Float`](#float) type some backend may compile this type as an 8 bytes / 64 bit float datatype. 
                To use a 4 bytes / 32 bit float datatype a precision <= 24 can usually be provided or the [`REAL`](#real) type can be used. 
                This is known to be the case in the PostgreSQL and MSSQL dialects that render the type as FLOAT that’s in both an alias of `DOUBLE PRECISION`. Other third party dialects may have similar behavior.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Integer
    val INTEGER = ColumnTypeDefinition(
        id = "integer",
        name = "Integer",

        sqlalchemyTypeName = "Integer",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Integer"),
        ),

        annotation = INT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("INT"),
            SqlAlias("INTEGER"),
            SqlAlias("INT4"),
            SqlAlias("SERIAL"),
            SqlAlias("MEDIUMINT"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Integer",
            text = """
                A type for `int` integers.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Interval
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

        sqlalchemyTypeName = "Interval",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Interval"),
        ),

        annotation = TIMEDELTA_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("INTERVAL"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Interval",
            text = """
                A type for `datetime.timedelta()` objects.

                The Interval type deals with `datetime.timedelta` objects. In PostgreSQL and Oracle Database, the native `INTERVAL` type is used; 
                for others, the value is stored as a date which is relative to the “epoch” (Jan. 1, 1970).

                Note that the `Interval` type does not currently provide date arithmetic operations on platforms which do not support interval types natively. 
                Such operations usually require transformation of both sides of the expression (such as, conversion of both sides into integer epoch values first) which currently is a manual procedure 
                (such as via [`expression.func`](https://docs.sqlalchemy.org/en/21/core/sqlelement.html#sqlalchemy.sql.expression.func)).
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.LargeBinary
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

        sqlalchemyTypeName = "LargeBinary",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "LargeBinary"),
        ),

        annotation = BYTES_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("BLOB"),
            SqlAlias("BINARY"),
            SqlAlias("VARBINARY"),
            SqlAlias("BYTEA"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.LargeBinary",
            text = """
                A type for large binary byte data.

                The [`LargeBinary`](#large_binary) type corresponds to a large and/or unlengthed binary type for the
                target platform, such as BLOB on MySQL and BYTEA for PostgreSQL.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Numeric
    val NUMERIC = ColumnTypeDefinition(
        id = "numeric",
        name = "Numeric",
        parameters = listOf(
            PRECISION_PARAM,
            SCALE_PARAM,
            asDecimalParam(default = true),
            DECIMAL_RETURN_SCALE_PARAM,
        ),

        sqlalchemyTypeName = "Numeric",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Numeric"),
        ),

        annotation = DECIMAL_OR_FLOAT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("NUMERIC"),
            SqlAlias("DECIMAL"),
            SqlAlias("DEC"),
            SqlAlias("NUMBER"),
            SqlAlias("MONEY"),
            SqlAlias("SMALLMONEY"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Numeric",
            text = """
                Base for non-integer numeric types, such as `NUMERIC`, `FLOAT`, `DECIMAL`, and other variants.

                The [`Numeric`](#numeric) datatype when used directly will render DDL corresponding to precision numerics if available, 
                such as `NUMERIC(precision, scale)`. The [`Float`](#float) subclass will attempt to render a floating-point datatype such as `FLOAT(precision)`.
                
                [`Numeric`](#numeric) returns Python `decimal.Decimal` objects by default, based on the default value of `True` for the [`Numeric.asdecimal`](#numeric.params.asdecimal) parameter. 
                If this parameter is set to False, returned values are coerced to Python `float` objects.
                
                The [`Float`](#float) subtype, being more specific to floating point, defaults the [`Float.asdecimal`](#float.params.asdecimal) flag to False so that the default Python datatype is `float`.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.PickleType
    val PICKLE_TYPE = ColumnTypeDefinition(
        id = "pickle",
        name = "PickleType",
        // Constructor parameters (`protocol`, `pickler`, `comparator`, ...) take Python
        // callables and class references, which the param model can't usefully express today.

        sqlalchemyTypeName = "PickleType",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "PickleType"),
        ),

        annotation = AnnotationResolver { _, _ ->
            AnnotationSpec(
                type = "Any",
                imports = listOf(ImportDefinition("typing", "Any")),
            )
        },

        docs = Docs(
            cls = "sqlalchemy.types.PickleType",
            text = """
                Holds Python objects, which are serialized using pickle.

                PickleType builds upon the Binary type to apply Python’s `pickle.dumps()` to incoming objects, 
                and `pickle.loads()` on the way out, allowing any pickleable Python object to be stored as a serialized binary field.

                To allow ORM change events to propagate for elements associated with [`PickleType`](#pickle), 
                see [Mutation Tracking](https://docs.sqlalchemy.org/en/21/orm/extensions/mutable.html).
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.SmallInteger
    val SMALL_INTEGER = ColumnTypeDefinition(
        id = "small_integer",
        name = "SmallInteger",

        sqlalchemyTypeName = "SmallInteger",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "SmallInteger"),
        ),

        annotation = INT_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("SMALLINT"),
            SqlAlias("INT2"),
            SqlAlias("SMALLSERIAL"),
            // TINYINT(1) is the MySQL/MariaDB boolean idiom; the dialect-specific Boolean overrides this.
            SqlAlias("TINYINT"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.SmallInteger",
            text = """
                A type for smaller `int` integers.

                Typically generates a `SMALLINT` in DDL, and otherwise acts like a normal [`Integer`](#integer) on the Python side.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.String
    val STRING = ColumnTypeDefinition(
        id = "string",
        name = "String",
        parameters = STRING_LIKE_PARAMS,

        sqlalchemyTypeName = "String",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "String"),
        ),

        annotation = STR_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("VARCHAR"),
            SqlAlias("VARCHAR2"),
            SqlAlias("CHAR"),
            SqlAlias("STRING"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.String",
            text = """
                The base for all string and character types.

                In SQL, corresponds to VARCHAR.
                
                The _length_ field is usually required when the String type is used within a CREATE TABLE statement, 
                as VARCHAR requires a length on most databases.
            """
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Text
    val TEXT = ColumnTypeDefinition(
        id = "text",
        name = "Text",
        parameters = STRING_LIKE_PARAMS,

        sqlalchemyTypeName = "Text",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Text"),
        ),

        annotation = STR_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("TEXT"),
            SqlAlias("CLOB"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Text",
            text = """
                A variably sized string type.

                In SQL, usually corresponds to CLOB or TEXT. In general, TEXT objects do not have a length; 
                while some databases will accept a length argument here, it will be rejected by others.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Time
    val TIME = ColumnTypeDefinition(
        id = "time",
        name = "Time",
        parameters = listOf(
            TIMEZONE_PARAM,
        ),

        sqlalchemyTypeName = "Time",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Time"),
        ),

        annotation = TIME_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("TIME"),
            SqlAlias("TIMETZ"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Time",
            text = """
                A type for `datetime.time()` objects.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Unicode
    val UNICODE = ColumnTypeDefinition(
        id = "unicode",
        name = "Unicode",
        parameters = STRING_LIKE_PARAMS,

        sqlalchemyTypeName = "Unicode",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Unicode"),
        ),

        annotation = STR_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("NVARCHAR"),
            SqlAlias("NCHAR"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Unicode",
            text = """
                A variable length Unicode string type.

                The [`Unicode`](#unicode) type is a [`String`](#string) subclass that assumes input and output strings that may contain non-ASCII characters, 
                and for some backends implies an underlying column type that is explicitly supporting of non-ASCII data, such as `NVARCHAR` on Oracle Database and SQL Server. 
                This will impact the output of `CREATE TABLE` statements and `CAST` functions at the dialect level.

                The character encoding used by the [`Unicode`](#unicode) type that is used to transmit and receive data to the database is usually determined by the DBAPI itself. 
                All modern DBAPIs accommodate non-ASCII strings but may have different methods of managing database encodings; 
                if necessary, this encoding should be configured as detailed in the notes for the target DBAPI in the [Dialects](https://docs.sqlalchemy.org/en/21/dialects/index.html) section.

                In modern SQLAlchemy, use of the [`Unicode`](#unicode) datatype does not imply any encoding/decoding behavior within SQLAlchemy itself. 
                In Python 3, all string objects are inherently Unicode capable, and SQLAlchemy does not produce bytestring objects nor does it accommodate a DBAPI that does not return Python Unicode objects in result sets for string values.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.UnicodeText
    val UNICODE_TEXT = ColumnTypeDefinition(
        id = "unicode_text",
        name = "UnicodeText",
        parameters = STRING_LIKE_PARAMS,

        sqlalchemyTypeName = "UnicodeText",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "UnicodeText"),
        ),

        annotation = STR_ANNOTATION,

        sqlAliases = listOf(
            SqlAlias("NCLOB"),
            SqlAlias("NTEXT"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.UnicodeText",
            text = """
                An unbounded-length Unicode string type.

                See [`Unicode`](#unicode) for details on the unicode behavior of this object.
                
                Like [`Unicode`](#unicode), usage the [`UnicodeText`](#unicode_text) type implies a unicode-capable type being used on the backend, such as `NCLOB`, `NTEXT`.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Uuid
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

        sqlalchemyTypeName = "Uuid",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Uuid"),
        ),

        annotation = { _, _ ->
            AnnotationSpec(type = "UUID", imports = listOf(ImportDefinition("uuid", "UUID")))
        },

        sqlAliases = listOf(
            SqlAlias("UUID"),
            SqlAlias("UNIQUEIDENTIFIER"),
        ),

        docs = Docs(
            cls = "sqlalchemy.types.Uuid",
            text = """
                Represent a database agnostic UUID datatype.

                For backends that have no “native” UUID datatype, the value will make use of `CHAR(32)` and store the UUID as a 32-character alphanumeric hex string.

                For backends which are known to support `UUID` directly or a similar uuid-storing datatype such as SQL Server’s 
                `UNIQUEIDENTIFIER`, a “native” mode enabled by default allows these types will be used on those backends.

                In its default mode of use, the [`Uuid`](#uuid) datatype expects Python uuid objects, 
                from the Python [uuid](https://docs.python.org/3/library/uuid.html) module:

                ```python
                import uuid

                from sqlalchemy import Uuid
                from sqlalchemy import Table, Column, MetaData, String

                metadata_obj = MetaData()

                t = Table(
                    "t",
                    metadata_obj,
                    Column("uuid_data", Uuid, primary_key=True),
                    Column("other_data", String),
                )

                with engine.begin() as conn:
                    conn.execute(
                        t.insert(), {"uuid_data": uuid.uuid4(), "other_data": "some data"}
                    )
                ```

                To have the [`Uuid`](#uuid) datatype work with string-based Uuids (e.g. 32 character hexadecimal strings), 
                pass the [`Uuid.as_uuid`](#uuid.params.as_uuid) parameter with the value `False`.
            """.trimIndent(),
        ),
    )

    // TODO: Enum

    // ---- Multiple-vendor UPPERCASE types -----------------------------------
    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sql-standard-and-multiple-vendor-uppercase-types

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY
    val ARRAY = ColumnTypeDefinition(
        id = "array",
        name = "Array",
        parameters = ARRAY_LIKE_PARAMS,

        sqlalchemyTypeName = "ARRAY",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "ARRAY"),
        ),

        annotation = ARRAY_ANNOTATION,

        docs = Docs(
            cls = "sqlalchemy.types.ARRAY",
            text = """
                Represent a SQL Array type.
                
                [`ARRAY`](#array) is part of the Core in support of various SQL standard functions such as [`array_agg`](#https://docs.sqlalchemy.org/en/21/core/functions.html#sqlalchemy.sql.functions.array_agg) 
                which explicitly involve arrays; however, with the exception of the PostgreSQL backend and possibly some third-party dialects, no other SQLAlchemy built-in dialect has support for this type.

                An ARRAY type is constructed given the “type” of element:
                ```python
                mytable = Table("mytable", metadata, Column("data", ARRAY(Integer)))
                ```
                
                The above type represents an N-dimensional array, meaning a supporting backend such as PostgreSQL will interpret values with any number of dimensions automatically. 
                To produce an INSERT construct that passes in a 1-dimensional array of integers:
                ```python
                connection.execute(mytable.insert(), {"data": [1, 2, 3]})
                ```
                
                The [`ARRAY`](#array) type can be constructed given a fixed number of dimensions:
                
                ```python
                mytable = Table(
                    "mytable", metadata, Column("data", ARRAY(Integer, dimensions=2))
                )
                ```
                
                Sending a number of dimensions is optional, but recommended if the datatype is to represent arrays of more than one dimension. This number is used:
                - When emitting the type declaration itself to the database, e.g. `INTEGER[][]`
                - When translating Python values to database values, and vice versa, e.g. an ARRAY of [`Unicode`](#unicode) objects uses this number to efficiently 
                  access the string values inside of array structures without resorting to per-row type inspection
                - When used with the Python `getitem` accessor, the number of dimensions serves to define the kind of type that the `[]` operator should return, e.g. for an ARRAY of INTEGER with two dimensions:
                  ```python
                  >>> expr = table.c.column[5]  # returns ARRAY(Integer, dimensions=1)
                  >>> expr = expr[6]  # returns Integer
                  ```
                
                For 1-dimensional arrays, an [`ARRAY`](#array) instance with no dimension parameter will generally assume single-dimensional behaviors.
                
                SQL expressions of type [`ARRAY`](#array) have support for “index” and “slice” behavior. 
                The `[]` operator produces expression constructs which will produce the appropriate SQL, both for SELECT statements:
                ```python
                select(mytable.c.data[5], mytable.c.data[2:7])
                ```
                
                as well as UPDATE statements when the [`Update.values()`](#https://docs.sqlalchemy.org/en/21/core/dml.html#sqlalchemy.sql.expression.Update.values) method is used:
                ```python
                mytable.update().values(
                    {mytable.c.data[5]: 7, mytable.c.data[2:7]: [1, 2, 3]}
                )
                ```
                
                Indexed access is one-based by default; for zero-based index conversion, set [`ARRAY.zero_indexes`](#array.params.zero_indexes).
                
                The [`ARRAY`](#array) type also provides for the operators 
                [`Comparator.any()`](https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY.Comparator.any) and 
                [`Comparator.all()`](https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY.Comparator.any). 
                The PostgreSQL-specific version of [`ARRAY`](#array) also provides additional operators.
            """.trimIndent(),
        ),
    )

    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.JSON
    val JSON = ColumnTypeDefinition(
        id = "json",
        name = "JSON",
        parameters = listOf(
            NONE_AS_NULL_PARAM,
        ),

        sqlalchemyTypeName = "JSON",
        sqlalchemyImports = listOf(ImportDefinition("sqlalchemy.types", "JSON")),

        annotation = DICT_ANNOTATION,
        sqlAliases = listOf(SqlAlias("JSON")),

        docs = Docs(
            cls = "sqlalchemy.types.JSON",
            text = """
                Represent a SQL JSON type.
                
                [`JSON`](#json) is part of the Core in support of the growing popularity of native JSON datatypes.
                
                The [`JSON`](#json) type stores arbitrary JSON format data, e.g.:
                ```python
                data_table = Table(
                    "data_table",
                    metadata,
                    Column("id", Integer, primary_key=True),
                    Column("data", JSON),
                )
                
                with engine.connect() as conn:
                    conn.execute(
                        data_table.insert(), {"data": {"key1": "value1", "key2": "value2"}}
                    )
                ```
            """.trimIndent(),
        ),
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
