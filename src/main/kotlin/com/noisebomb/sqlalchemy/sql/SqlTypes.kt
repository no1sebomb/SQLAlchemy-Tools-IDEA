package com.noisebomb.sqlalchemy.sql

// Helper function for nested generics, like 'list[list[int]]'
fun generateNested(container: String, inner: String, depth: Int): String {
    var result = inner

    repeat(depth) {
        result = "$container[$result]"
    }

    return result
}

// SQLAlchemy type parameters
sealed interface TypeParameter {
    val id: String
    val label: String
    val description: String?
}

data class IntParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val min: Int? = null,
    val max: Int? = null,
    val optional: Boolean = true
) : TypeParameter

data class BooleanParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val defaultValue: Boolean = false
) : TypeParameter

data class EnumParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val values: List<String>,
    val defaultValue: String? = null
) : TypeParameter

data class StringParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val optional: Boolean = true
) : TypeParameter

data class TypeRefParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val allowedScopes: Set<SqlDialect> = setOf(SqlDialect.GENERIC)
) : TypeParameter

// Imports used for column type
data class ImportDefinition(
    val module: String,
    val name: String
)

data class TypeInstance(
    val definitionId: String,
    val values: Map<String, Any?>,
    val children: Map<String, TypeInstance> = emptyMap()
)
fun TypeInstance.bool(id: String) = values[id] as? Boolean
fun TypeInstance.int(id: String) = values[id] as? Int
fun TypeInstance.string(id: String) = values[id] as? String

data class AnnotationSpec(
    val type: String,
    val imports: List<ImportDefinition> = emptyList()
)

fun interface AnnotationResolver {
    fun resolve(instance: TypeInstance): AnnotationSpec
}

data class ColumnTypeDefinition(
    // Type properties
    val id: String,
    val name: String,
    val parameters: List<TypeParameter> = emptyList(),

    // UI hint for SQlAlchemy docs
    val description: String? = null,
    val docsUrl: String? = null,

    // SQLAlchemy imports
    val sqlalchemyTypeName: String,
    val sqlalchemyImports: List<ImportDefinition>,

    // Python annotation
    val annotation: AnnotationResolver,

    // Dialect
    val dialect: SqlDialect = SqlDialect.GENERIC,
)
fun ColumnTypeDefinition.resolveAnnotation(instance: TypeInstance): AnnotationSpec {
    return annotation.resolve(instance)
}


object ColumnTypes {

    // Generic "CamelCase" types
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
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "BigInteger"),
        ),

        annotation = AnnotationResolver {
            AnnotationSpec("int")
        },
    )

    val BOOLEAN = ColumnTypeDefinition(
        id = "boolean",
        name = "Boolean",

        description = """
            A bool datatype.

            Boolean typically uses `BOOLEAN` or `SMALLINT` on the DDL side, 
            and on the Python side deals in `True` or `False`.

            The Boolean datatype currently has two levels of assertion that the values persisted are 
            simple true/false values. For all backends, only the Python values `None`, `True`, `False`, `1` or `0` 
            are accepted as parameter values. For those backends that don’t support a “native boolean” datatype, 
            an option exists to also create a CHECK constraint on the target column.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Boolean",

        sqlalchemyTypeName = "Boolean",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Boolean")
        ),

        annotation = AnnotationResolver {
            AnnotationSpec("bool")
        },
    )

    val DATE = ColumnTypeDefinition(
        id = "date",
        name = "Date",

        description = """
            A type for `datetime.date()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Date",

        sqlalchemyTypeName = "Date",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Date")
        ),

        annotation = {
            AnnotationSpec(
                type = "date",
                imports = listOf(
                    ImportDefinition("datetime", "date")
                )
            )
        },
    )

    val DATETIME = ColumnTypeDefinition(
        id = "datetime",
        name = "DateTime",
        parameters = listOf(
            BooleanParameter(
                id = "timezone",
                label = "Timezone aware",
                description = """
                    Indicates that the datetime type should enable timezone support, if available on the 
                    **base date/time-holding type only**. It is recommended to make use of the ``TIMESTAMP`` datatype 
                    directly when using this flag, as some databases include separate generic date/time-holding types 
                    distinct from the timezone-capable TIMESTAMP datatype, such as Oracle Database.
                """.trimIndent(),
                defaultValue = false,
            ),
        ),

        description = """
            A type for `datetime.datetime()` objects.

            Date and time types return objects from the Python `datetime` module. 
            Most DBAPIs have built in support for the datetime module, with the noted exception of SQLite. 
            In the case of SQLite, date and time types are stored as strings which are 
            then converted back to datetime objects when rows are returned.

            For the time representation within the datetime type, some backends include additional options, 
            such as timezone support and fractional seconds support. For fractional seconds, 
            use the dialect-specific datatype, such as `TIME`. For timezone support, 
            use at least the `TIMESTAMP` datatype, if not the dialect-specific datatype object.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.DateTime",

        sqlalchemyTypeName = "DateTime",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "DateTime")
        ),

        annotation = {
            AnnotationSpec(
                type = "datetime",
                imports = listOf(
                    ImportDefinition("datetime", "datetime")
                )
            )
        },
    )

    val DOUBLE = ColumnTypeDefinition(
        id = "double",
        name = "Double",
        parameters = listOf(
            IntParameter(
                id = "precision",
                label = "Precision",
                description = """
                    The numeric precision for use in DDL `CREATE TABLE`. Backends **should** attempt 
                    to ensure this precision indicates a number of digits for the generic `Float` datatype.
                """.trimIndent(),
                min = 1,
                optional = true
            ),
            BooleanParameter(
                id = "asdecimal",
                label = "As decimal",
                description = """
                    The same flag as that of `Numeric`, but defaults to `False`. 
                    Note that setting this flag to `True` results in floating point conversion.
                """.trimIndent(),
                defaultValue = false,
            ),
            IntParameter(
                id = "decimal_return_scale",
                label = "Decimal return scale",
                description = """
                     Default scale to use when converting from floats to Python decimals. Floating point values 
                     will typically be much longer due to decimal inaccuracy, and most floating point database 
                     types don’t have a notion of “scale”, so by default the float type looks for the first ten 
                     decimal places when converting. Specifying this value will override that length. 
                     Note that the MySQL float types, which do include “scale”, will use “scale” 
                     as the default for decimal_return_scale, if not otherwise specified.
                """.trimIndent(),
                min = 0,
                optional = true
            ),
        ),

        description = """
            A type for double `FLOAT` floating point types.

            Typically generates a `DOUBLE` or `DOUBLE_PRECISION` in DDL, 
            and otherwise acts like a normal Float on the Python side.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Double",

        sqlalchemyTypeName = "Double",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Double")
        ),

        annotation = { instance ->
            if (instance.bool("asdecimal") == true) {
                AnnotationSpec(
                    type = "Decimal",
                    imports = listOf(
                        ImportDefinition("decimal", "Decimal"),
                    ),
                )
            } else {
                AnnotationSpec("float")
            }
        },
    )

    val FLOAT = ColumnTypeDefinition(
        id = "float",
        name = "Float",
        parameters = listOf(
            IntParameter(
                id = "precision",
                label = "Precision",
                description = """
                    The numeric precision for use in DDL `CREATE TABLE`. Backends **should** attempt 
                    to ensure this precision indicates a number of digits for the generic `Float` datatype.
                """.trimIndent(),
                min = 1,
                optional = true
            ),
            BooleanParameter(
                id = "asdecimal",
                label = "As decimal",
                description = """
                    The same flag as that of `Numeric`, but defaults to `False`. 
                    Note that setting this flag to `True` results in floating point conversion.
                """.trimIndent(),
                defaultValue = false,
            ),
            IntParameter(
                id = "decimal_return_scale",
                label = "Decimal return scale",
                description = """
                     Default scale to use when converting from floats to Python decimals. Floating point values 
                     will typically be much longer due to decimal inaccuracy, and most floating point database 
                     types don’t have a notion of “scale”, so by default the float type looks for the first ten 
                     decimal places when converting. Specifying this value will override that length. 
                     Note that the MySQL float types, which do include “scale”, will use “scale” 
                     as the default for decimal_return_scale, if not otherwise specified.
                """.trimIndent(),
                min = 0,
                optional = true
            ),
        ),

        description = """
            Type representing floating point types, such as `FLOAT` or `REAL`.

            This type returns Python `float` objects by default, unless the `Float.asdecimal` 
            flag is set to True, in which case they are coerced to `decimal.Decimal` objects.
            
            When a `Float.precision` is not provided in a `Float` type some backend may compile this type as 
            an 8 bytes / 64 bit float datatype. To use a 4 bytes / 32 bit float datatype a precision <= 24 can usually 
            be provided or the `REAL` type can be used. This is known to be the case in the PostgreSQL and MSSQL 
            dialects that render the type as `FLOAT` that’s in both an alias of `DOUBLE PRECISION`. 
            Other third party dialects may have similar behavior.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Float",

        sqlalchemyTypeName = "Float",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Float")
        ),

        annotation = { instance ->
            if (instance.bool("asdecimal") == true) {
                AnnotationSpec(
                    type = "Decimal",
                    imports = listOf(
                        ImportDefinition("decimal", "Decimal"),
                    ),
                )
            } else {
                AnnotationSpec("float")
            }
        },
    )

    val INTEGER = ColumnTypeDefinition(
        id = "integer",
        name = "Integer",

        description = """
            A type for `int` integers.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Integer",

        sqlalchemyTypeName = "Integer",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Integer")
        ),

        annotation = AnnotationResolver {
            AnnotationSpec("int")
        },
    )

    val INTERVAL = ColumnTypeDefinition(
        id = "interval",
        name = "Interval",
        parameters = listOf(
            BooleanParameter(
                id = "native",
                label = "Native",
                description = """
                    When True, use the actual INTERVAL type provided by the database, 
                    if supported (currently PostgreSQL, Oracle Database). 
                    Otherwise, represent the interval data as an epoch value regardless.
                """.trimIndent(),
                defaultValue = true,
            ),
            IntParameter(
                id = "second_precision",
                label = "Second precision",
                description = """
                    For native interval types which support a “fractional seconds precision” parameter, 
                    i.e. Oracle Database and PostgreSQL.
                """.trimIndent(),
                min = 0,
                optional = true,
            ),
            IntParameter(
                id = "day_precision",
                label = "Day precision",
                description = """
                    For native interval types which support a “day precision” parameter, 
                    i.e. Oracle Database.
                """.trimIndent(),
                min = 0,
                optional = true,
            )
        ),

        description = """
            A type for `datetime.timedelta()` objects.

            The Interval type deals with `datetime.timedelta` objects. 
            In PostgreSQL and Oracle Database, the native `INTERVAL` type is used; 
            for others, the value is stored as a date which is relative to the “epoch” (Jan. 1, 1970).

            Note that the `Interval` type does not currently provide date arithmetic operations on platforms 
            which do not support interval types natively. Such operations usually require transformation of 
            both sides of the expression (such as, conversion of both sides into integer epoch values first) 
            which currently is a manual procedure (such as via `expression.func`).
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Interval",

        sqlalchemyTypeName = "Interval",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Interval")
        ),

        annotation = {
            AnnotationSpec(
                type = "timedelta",
                imports = listOf(
                    ImportDefinition("datetime", "timedelta"),
                ),
            )
        },
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
            )
        ),

        description = """
            A type for large binary byte data.

            The `LargeBinary` type corresponds to a large and/or unlengthed binary type for the target platform, 
            such as BLOB on MySQL and BYTEA for PostgreSQL. It also handles the necessary conversions for the DBAPI.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.LargeBinary",

        sqlalchemyTypeName = "LargeBinary",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "LargeBinary")
        ),

        annotation = {
            AnnotationSpec("bytes")
        },
    )

    val NUMERIC = ColumnTypeDefinition(
        id = "numeric",
        name = "Numeric",
        parameters = listOf(
            IntParameter(
                id = "precision",
                label = "Precision",
                description = """
                    The numeric precision for use in DDL `CREATE TABLE`.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
            IntParameter(
                id = "scale",
                label = "Scale",
                description = """
                    The numeric scale for use in DDL `CREATE TABLE`.
                """.trimIndent(),
                min = 0,
                optional = true,
            ),
            BooleanParameter(
                id = "asdecimal",
                label = "As decimal",
                description = """
                    Return whether or not values should be sent as Python Decimal objects, or as floats. 
                    Different DBAPIs send one or the other based on datatypes - the Numeric type 
                    will ensure that return values are one or the other across DBAPIs consistently.
                """.trimIndent(),
                defaultValue = true,
            ),
            IntParameter(
                id = "decimal_return_scale",
                label = "Decimal return scale",
                description = """
                     Default scale to use when converting from floats to Python decimals. Floating point values 
                     will typically be much longer due to decimal inaccuracy, and most floating point database types 
                     don’t have a notion of “scale”, so by default the float type looks for the first ten decimal 
                     places when converting. Specifying this value will override that length. Types which do include 
                     an explicit “.scale” value, such as the base `Numeric` as well as the MySQL float types, will 
                     use the value of “.scale” as the default for decimal_return_scale, if not otherwise specified.
                """.trimIndent(),
                min = 0,
                optional = true,
            ),
        ),

        description = """
            Base for non-integer numeric types, such as `NUMERIC`, `FLOAT`, `DECIMAL`, and other variants.

            The `Numeric` datatype when used directly will render DDL corresponding to precision 
            numerics if available, such as `NUMERIC(precision, scale)`. The Float subclass will 
            attempt to render a floating-point datatype such as `FLOAT(precision)`.

            `Numeric` returns Python `decimal.Decimal` objects by default, based on the default value of 
            `True` for the `Numeric.asdecimal` parameter. If this parameter is set to False, 
            returned values are coerced to Python `float` objects.

            The `Float` subtype, being more specific to floating point, 
            defaults the `Float.asdecimal` flag to False so that the default Python datatype is `float`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Numeric",

        sqlalchemyTypeName = "Numeric",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Numeric")
        ),

        annotation = { instance ->
            if (instance.bool("asdecimal") == true) {
                AnnotationSpec(
                    type = "Decimal",
                    imports = listOf(
                        ImportDefinition("decimal", "Decimal"),
                    ),
                )
            } else {
                AnnotationSpec("float")
            }
        }
    )

    val PICKLE_TYPE = ColumnTypeDefinition(
        id = "pickle",
        name = "PickleType",
//        Pretty complex parameters, as it requires specifying custom objects and functions.
//        parameters = listOf()

        description = """
            Holds Python objects, which are serialized using pickle.

            PickleType builds upon the Binary type to apply Python’s `pickle.dumps()` to incoming objects, and 
            `pickle.loads()` on the way out, allowing any pickleable Python object to be stored 
            as a serialized binary field.
            
            To allow ORM change events to propagate for elements associated with `PickleType`, see `Mutation Tracking`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.PickleType",

        sqlalchemyTypeName = "PickleType",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "PickleType")
        ),

        annotation = {
            AnnotationSpec(
                type = "Any",
                imports = listOf(
                    ImportDefinition("typing", "Any")
                ),
            )
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
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "SmallInteger"),
        ),

        annotation = AnnotationResolver {
            AnnotationSpec("int")
        },
    )

    val STRING = ColumnTypeDefinition(
        id = "string",
        name = "String",
        parameters = listOf(
            IntParameter(
                id = "length",
                label = "Length",
                description = """
                    A length for the column for use in DDL and CAST expressions. 
                    May be safely omitted if no `CREATE TABLE` will be issued. 
                    Certain databases may require a length for use in DDL, and will raise an exception 
                    when the `CREATE TABLE` DDL is issued if a `VARCHAR` with no length is included. 
                    Whether the value is interpreted as bytes or characters is database specific.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
            StringParameter(
                id = "collation",
                label = "Collation",
                description = """
                    Optional, a column-level collation for use in DDL and CAST expressions. 
                    Renders using the COLLATE keyword supported by SQLite, MySQL, and PostgreSQL.
                """.trimIndent(),
                optional = true,
            ),
        ),

        description = """
            The base for all string and character types.
    
            In SQL, corresponds to VARCHAR.
    
            The _length_ field is usually required when the String type is used within a 
            CREATE TABLE statement, as VARCHAR requires a length on most databases.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.String",

        sqlalchemyTypeName = "String",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "String")
        ),

        annotation = {
            AnnotationSpec("str")
        },
    )

    val TEXT = ColumnTypeDefinition(
        id = "text",
        name = "Text",
        parameters = listOf(
            IntParameter(
                id = "length",
                label = "Length",
                description = """
                    A length for the column for use in DDL and CAST expressions. 
                    May be safely omitted if no `CREATE TABLE` will be issued. 
                    Certain databases may require a length for use in DDL, and will raise an exception 
                    when the `CREATE TABLE` DDL is issued if a `VARCHAR` with no length is included. 
                    Whether the value is interpreted as bytes or characters is database specific.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
            StringParameter(
                id = "collation",
                label = "Collation",
                description = """
                    Optional, a column-level collation for use in DDL and CAST expressions. 
                    Renders using the COLLATE keyword supported by SQLite, MySQL, and PostgreSQL.
                """.trimIndent(),
                optional = true,
            ),
        ),

        description = """
            A variably sized string type.

            In SQL, usually corresponds to CLOB or TEXT. In general, TEXT objects do not have a length; 
            while some databases will accept a length argument here, it will be rejected by others.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Text",

        sqlalchemyTypeName = "Text",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Text")
        ),

        annotation = {
            AnnotationSpec("str")
        },
    )

    val TIME = ColumnTypeDefinition(
        id = "time",
        name = "Time",
        parameters = listOf(
            BooleanParameter(
                id = "timezone",
                label = "Timezone aware",
                defaultValue = false,
            ),
        ),

        description = """
            A type for `datetime.time()` objects.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Time",

        sqlalchemyTypeName = "Time",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Time")
        ),

        annotation = {
            AnnotationSpec(
                type = "time",
                imports = listOf(
                    ImportDefinition("datetime", "time")
                )
            )
        },
    )

    val UNICODE = ColumnTypeDefinition(
        id = "unicode",
        name = "Unicode",
        parameters = listOf(
            IntParameter(
                id = "length",
                label = "Length",
                description = """
                    A length for the column for use in DDL and CAST expressions. 
                    May be safely omitted if no `CREATE TABLE` will be issued. 
                    Certain databases may require a length for use in DDL, and will raise an exception 
                    when the `CREATE TABLE` DDL is issued if a `VARCHAR` with no length is included. 
                    Whether the value is interpreted as bytes or characters is database specific.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
            StringParameter(
                id = "collation",
                label = "Collation",
                description = """
                    Optional, a column-level collation for use in DDL and CAST expressions. 
                    Renders using the COLLATE keyword supported by SQLite, MySQL, and PostgreSQL.
                """.trimIndent(),
                optional = true,
            ),
        ),

        description = """
            A variable length Unicode string type.

            The `Unicode` type is a `String` subclass that assumes input and output strings that may 
            contain non-ASCII characters, and for some backends implies an underlying column type that is 
            explicitly supporting of non-ASCII data, such as NVARCHAR on Oracle Database and SQL Server. 
            This will impact the output of `CREATE TABLE` statements and CAST functions at the dialect level.
            
            The character encoding used by the `Unicode` type that is used to transmit and receive data to the 
            database is usually determined by the DBAPI itself. All modern DBAPIs accommodate non-ASCII strings 
            but may have different methods of managing database encodings; if necessary, 
            this encoding should be configured as detailed in the notes for the target DBAPI in the `Dialects` section.
            
            In modern SQLAlchemy, use of the `Unicode` datatype does not imply any encoding/decoding behavior within 
            SQLAlchemy itself. In Python 3, all string objects are inherently Unicode capable, and SQLAlchemy does 
            not produce bytestring objects nor does it accommodate a DBAPI that does not 
            return Python Unicode objects in result sets for string values.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Unicode",

        sqlalchemyTypeName = "Unicode",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Unicode")
        ),

        annotation = {
            AnnotationSpec("str")
        },
    )

    val UNICODE_TEXT = ColumnTypeDefinition(
        id = "unicode_text",
        name = "UnicodeText",
        parameters = listOf(
            IntParameter(
                id = "length",
                label = "Length",
                description = """
                    A length for the column for use in DDL and CAST expressions. 
                    May be safely omitted if no `CREATE TABLE` will be issued. 
                    Certain databases may require a length for use in DDL, and will raise an exception 
                    when the `CREATE TABLE` DDL is issued if a `VARCHAR` with no length is included. 
                    Whether the value is interpreted as bytes or characters is database specific.
                """.trimIndent(),
                min = 1,
                optional = true,
            ),
            StringParameter(
                id = "collation",
                label = "Collation",
                description = """
                    Optional, a column-level collation for use in DDL and CAST expressions. 
                    Renders using the COLLATE keyword supported by SQLite, MySQL, and PostgreSQL.
                """.trimIndent(),
                optional = true,
            ),
        ),

        description = """
            An unbounded-length Unicode string type.

            See `Unicode` for details on the unicode behavior of this object.

            Like `Unicode`, usage the `UnicodeText` type implies a unicode-capable type 
            being used on the backend, such as `NCLOB`, `NTEXT`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.UnicodeText",

        sqlalchemyTypeName = "UnicodeText",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "UnicodeText")
        ),

        annotation = {
            AnnotationSpec("str")
        },
    )

    val UUID = ColumnTypeDefinition(
        id = "uuid",
        name = "Uuid",

        description = """
            Represent a database agnostic UUID datatype.

            For backends that have no “native” UUID datatype, the value will make use of `CHAR(32)` 
            and store the UUID as a 32-character alphanumeric hex string.

            For backends which are known to support UUID directly or a similar uuid-storing 
            datatype such as SQL Server’s `UNIQUEIDENTIFIER`, 
            a “native” mode enabled by default allows these types will be used on those backends.

            In its default mode of use, the `Uuid` datatype expects 
            **Python uuid objects**, from the Python `uuid` module:
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.Uuid",

        sqlalchemyTypeName = "Uuid",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "Uuid"),
        ),

        annotation = {
            AnnotationSpec(
                type = "UUID",
                imports = listOf(
                    ImportDefinition("uuid", "UUID"),
                ),
            )
        }
    )

    // TODO: Enum

    // Multiple Vendor "UPPERCASE" Types
    // SQL Standard types that are aliases for CamelCase types are omitted.
    // https://docs.sqlalchemy.org/en/21/core/type_basics.html#sql-standard-and-multiple-vendor-uppercase-types

    val ARRAY = ColumnTypeDefinition(
        id = "array",
        name = "Array",
        parameters = listOf(
            TypeRefParameter(
                id = "item_type",
                label = "Item type",
            ),
            BooleanParameter(
                id = "as_tuple",
                label = "As tuple",
                description = """
                    Specify whether return results should be converted to tuples from lists. 
                    This parameter is not generally needed as a Python list corresponds well to a SQL array.
                """.trimIndent(),
                defaultValue = false,
            ),
            IntParameter(
                id = "dimensions",
                label = "Dimensions",
                description = """
                    If non-None, the ARRAY will assume a fixed number of dimensions. This impacts how the array is 
                    declared on the database, how it goes about interpreting Python and result values, 
                    as well as how expression behavior in conjunction with the “getitem” operator works. 
                    See the description at `ARRAY` for additional detail.
                """.trimIndent(),
                optional = true,
                min = 1,
                max = 10,
            ),
            BooleanParameter(
                id = "zero_indexes",
                label = "Zero indexes",
                description = """
                    When True, index values will be converted between Python zero-based and SQL one-based indexes, 
                    e.g. a value of one will be added to all index values before passing to the database.
                """.trimIndent(),
                defaultValue = false,
            )
        ),

        description = """
            Represent a SQL Array type.

            > Note: This type serves as the basis for all ARRAY operations. 
            > However, currently only the **PostgreSQL backend has support for SQL arrays in SQLAlchemy**. 
            > It is recommended to use the PostgreSQL-specific `sqlalchemy.dialects.postgresql.ARRAY` type directly 
            > when using ARRAY types with PostgreSQL, as it provides additional operators specific to that backend.

            ARR`AY is part of the Core in support of various SQL standard functions such as `array_agg` which 
            explicitly involve arrays; however, with the exception of the PostgreSQL backend and possibly 
            some third-party dialects, no other SQLAlchemy built-in dialect has support for this type.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.ARRAY",

        sqlalchemyTypeName = "ARRAY",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "ARRAY")
        ),

        annotation = {
            AnnotationSpec("list")
//            FIXME
//            val baseContainer = if (instance.bool("as_tuple") == true) {
//                "tuple"
//            } else {
//                "list"
//            }
//
//            val innerInstance = instance.children["item_type"]
//                ?: error("ARRAY requires item_type")
//
//            val innerAnnotation = "..."  // FIXME
//
//            val dimensions = instance.int("dimensions") ?: 1
//
//            val finalType = generateNested(baseContainer, innerAnnotation.type, dimensions)
//
//            AnnotationSpec(
//                type = finalType,
//                imports = innerAnnotation.imports
//            )
        }
    )

    val JSON = ColumnTypeDefinition(
        id = "json",
        name = "JSON",
        parameters = listOf(
            BooleanParameter(
                id = "none_as_null",
                label = "None as null",
                description = """
                    if True, persist the value None as a SQL NULL value, not the JSON encoding of `null`. 
                    Note that when this flag is False, the` null()` construct can still be used to 
                    persist a NULL value, which may be passed directly as a parameter value that is specially 
                    interpreted by the `JSON` type as SQL NULL.
                """.trimIndent(),
                defaultValue = false,
            ),
        ),

        description = """
            Represent a SQL JSON type.
            
            `JSON` is part of the Core in support of the growing popularity of native `JSON` datatypes.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/core/type_basics.html#sqlalchemy.types.JSON",

        sqlalchemyTypeName = "JSON",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.types", "JSON")
        ),

        annotation = {
            AnnotationSpec("dict")
        }
    )

    // PostgreSQL dialect types
    val PG_JSONB = ColumnTypeDefinition(
        id = "pg_jsonb",
        name = "JSONB",
        parameters = listOf(
            BooleanParameter(
                id = "none_as_null",
                label = "None as null",
                description = """
                    if True, persist the value None as a SQL NULL value, not the JSON encoding of `null`. 
                    Note that when this flag is False, the` null()` construct can still be used to 
                    persist a NULL value, which may be passed directly as a parameter value that is specially 
                    interpreted by the `JSON` type as SQL NULL.
                """.trimIndent(),
                defaultValue = false,
            ),
        ),

        description = """
            Represent the PostgreSQL JSONB type.
            
            The JSONB type includes all operations provided by JSON, including the same behaviors for indexing 
            operations. It also adds additional operators specific to JSONB, including `Comparator.has_key()`, 
            `Comparator.has_all()`, `Comparator.has_any()`, `Comparator.contains()`, `Comparator.contained_by()`, 
            `Comparator.delete_path()`, `Comparator.path_exists()` and `Comparator.path_match()`.
        """.trimIndent(),
        docsUrl = "https://docs.sqlalchemy.org/en/21/dialects/postgresql.html#sqlalchemy.dialects.postgresql.JSONB",

        sqlalchemyTypeName = "JSONB",
        sqlalchemyImports = listOf(
            ImportDefinition("sqlalchemy.dialects.postgresql", "JSONB")
        ),

        annotation = {
            AnnotationSpec("dict")
        },

        dialect = SqlDialect.POSTGRESQL,
    )

    // ---------- Registry ----------

    val ALL = listOf(
        // Generic CamelCase
        BIGINTEGER,
        BOOLEAN,
        DATE,
        DATETIME,
        DOUBLE,
        FLOAT,
        INTEGER,
        INTERVAL,
        LARGE_BINARY,
        NUMERIC,
        PICKLE_TYPE,
        SMALL_INTEGER,
        STRING,
        TEXT,
        TIME,
        UNICODE,
        UNICODE_TEXT,
        UUID,

        // Generic UPPERCASE
        ARRAY,
        JSON,

        // PostgreSQL
        PG_JSONB,
    )
}
