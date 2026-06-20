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
// Date & time parameters
//   Time/DateTime share the same `timezone` parameter.
// ---------------------------------------------------------------------------

internal val TIMEZONE_PARAM: TypeParameter = BooleanParameter(
    id = "timezone",
    label = "Timezone aware",
    description = """
        Indicates that the date/time type should enable timezone support, if available on the
        base date/time-holding type only.
    """.trimIndent(),
    defaultValue = false,
)

// ---------------------------------------------------------------------------
// Array parameters
// ---------------------------------------------------------------------------

internal val NESTED_TYPE_PARAM: TypeParameter = TypeRefParameter(
    id = "item_type",
    label = "Items type",
    description = """
        The data type of items of this array. Note that dimensionality is irrelevant here, so multi-dimensional 
        arrays like `INTEGER[][]`, are constructed as `ARRAY(Integer)`, not as `ARRAY(ARRAY(Integer))` or such.
    """.trimIndent()
)

internal var AS_TUPLE_PARAM: TypeParameter = BooleanParameter(
    id = "as_tuple",
    label = "As tuple",
    description = "Return results as tuples instead of lists.",
    defaultValue = false,
)

internal val DIMENSIONS_PARAM: TypeParameter = IntParameter(
    id = "dimensions",
    label = "Dimensions",
    description = "Fixed number of array dimensions for DDL/codegen.",
    positional = false,
    optional = true,
    min = 1,
    max = 10,  // Sanity limit
)

internal val ZERO_INDEXES_PARAM: TypeParameter = BooleanParameter(
    id = "zero_indexes",
    label = "Zero indexes",
    description = "Convert between Python zero-based and SQL one-based indexes.",
    defaultValue = false,
)

internal val ARRAY_LIKE_PARAMS: List<TypeParameter> = listOf(
    NESTED_TYPE_PARAM,
    AS_TUPLE_PARAM,
    DIMENSIONS_PARAM,
    ZERO_INDEXES_PARAM,
)

// ---------------------------------------------------------------------------
// JSON parameters
// ---------------------------------------------------------------------------

internal val NONE_AS_NULL_PARAM: TypeParameter = BooleanParameter(
    id = "none_as_null",
    label = "None as null",
    description = "If True, persist the value None as a SQL NULL value, not the JSON encoding of `null`",
    defaultValue = false,
)

// ---------------------------------------------------------------------------
// Shared annotation resolvers
// ---------------------------------------------------------------------------

internal val BOOL_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("bool") }
internal val STR_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("str") }
internal val INT_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("int") }
internal val BYTES_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("bytes") }
internal val DICT_ANNOTATION = AnnotationResolver { _, _ -> AnnotationSpec("dict") }
internal val DATE_ANNOTATION = AnnotationResolver { _, _ ->
    AnnotationSpec(type = "date", imports = listOf(ImportDefinition("datetime", "date")))
}
internal val TIME_ANNOTATION = AnnotationResolver { _, _ ->
    AnnotationSpec(type = "time", imports = listOf(ImportDefinition("datetime", "time")))
}
internal val DATETIME_ANNOTATION = AnnotationResolver { _, _ ->
    AnnotationSpec(type = "datetime", imports = listOf(ImportDefinition("datetime", "datetime")))
}

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

/**
 * Common resolver used for ARRAY types, returns generic `list` or `tuple` depending on the
 * `as_tuple` parameter.
 */
internal val ARRAY_ANNOTATION = AnnotationResolver { instance, registry ->
    val itemInstance = instance.children["item_type"]
    val container = if (instance.bool("as_tuple") == true) "tuple" else "list"
    if (itemInstance == null) {
        AnnotationSpec(container)
    } else {
        val itemDef = registry.getById(itemInstance.definitionId)
        val itemAnno = itemDef.resolveAnnotation(itemInstance, registry)
        val dimensions = (instance.int("dimensions") ?: 1).coerceAtLeast(1)
        AnnotationSpec(
            type = generateNested(container, itemAnno.type, dimensions),
            imports = itemAnno.imports,
        )
    }
}
