package com.noisebomb.sqlalchemy.sql.types

// ---------------------------------------------------------------------------
// String-family parameters
//   String / Text / Unicode / UnicodeText all inherit String in SQLAlchemy and accept
//   the same `length` + `collation` constructor args. We declare the params once and
//   reuse them so the definitions don't duplicate.
// ---------------------------------------------------------------------------

internal val LENGTH_PARAM: TypeParameter = IntParameter(
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
)

internal val COLLATION_PARAM: TypeParameter = StringParameter(
    id = "collation",
    label = "Collation",
    description = """
        Optional, a column-level collation for use in DDL and CAST expressions.
        Renders using the COLLATE keyword supported by SQLite, MySQL, and PostgreSQL.
    """.trimIndent(),
    positional = false,
    optional = true,
)

internal val STRING_LIKE_PARAMS: List<TypeParameter> = listOf(LENGTH_PARAM, COLLATION_PARAM)

// ---------------------------------------------------------------------------
// Numeric-family parameters
//   Numeric/Float/Double share precision + decimal_return_scale and accept asdecimal,
//   but with different defaults. `asDecimalParam(default)` is a factory so each definition
//   can declare the right default in one line.
// ---------------------------------------------------------------------------

internal val PRECISION_PARAM: TypeParameter = IntParameter(
    id = "precision",
    label = "Precision",
    description = """
        The numeric precision for use in DDL `CREATE TABLE`.
    """.trimIndent(),
    min = 1,
    optional = true,
)

internal val SCALE_PARAM: TypeParameter = IntParameter(
    id = "scale",
    label = "Scale",
    description = """
        The numeric scale for use in DDL `CREATE TABLE`.
    """.trimIndent(),
    min = 0,
    optional = true,
)

internal val DECIMAL_RETURN_SCALE_PARAM: TypeParameter = IntParameter(
    id = "decimal_return_scale",
    label = "Decimal return scale",
    description = """
        Default scale to use when converting from floats to Python decimals. Floating point values
        will typically be much longer due to decimal inaccuracy, and most floating point database
        types don't have a notion of "scale", so by default the float type looks for the first ten
        decimal places when converting. Specifying this value will override that length.
    """.trimIndent(),
    positional = false,
    min = 0,
    optional = true,
)

internal fun asDecimalParam(default: Boolean): TypeParameter = BooleanParameter(
    id = "asdecimal",
    label = "As decimal",
    description = """
        Return whether or not values should be sent as Python Decimal objects, or as floats.
        Different DBAPIs send one or the other based on datatypes — the type will ensure that
        return values are one or the other across DBAPIs consistently.
    """.trimIndent(),
    defaultValue = default,
)

// ---------------------------------------------------------------------------
// Shared annotation resolvers
// ---------------------------------------------------------------------------

internal val BOOL_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("bool") }
internal val STR_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("str") }
internal val INT_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("int") }
internal val BYTES_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("bytes") }
internal val DICT_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("dict") }

/**
 * Common `asdecimal`-aware resolver shared by Numeric/Float/Double: returns `Decimal` when
 * `asdecimal=True`, else `float`.
 */
internal val DECIMAL_OR_FLOAT_ANNOTATION = AnnotationResolver { instance, _ ->
    if (instance.bool("asdecimal") == true) {
        AnnotationSpec(
            type = "Decimal",
            imports = listOf(ImportDefinition("decimal", "Decimal")),
        )
    } else {
        AnnotationSpec("float")
    }
}
