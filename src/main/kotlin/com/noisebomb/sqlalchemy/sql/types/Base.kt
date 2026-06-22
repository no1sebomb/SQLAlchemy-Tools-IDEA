package com.noisebomb.sqlalchemy.sql.types

import com.noisebomb.sqlalchemy.sql.SqlDialect

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Wraps [inner] in [depth] layers of [container], e.g. `("list", "int", 2) -> "list[list[int]]"`. */
fun generateNested(container: String, inner: String, depth: Int): String {
    var result = inner
    repeat(depth) { result = "$container[$result]" }
    return result
}

// ---------------------------------------------------------------------------
// Type parameters
// ---------------------------------------------------------------------------

/**
 * One configurable knob of a [ColumnTypeDefinition], e.g. `precision` of `Numeric` or
 * `timezone` of `DateTime`.
 *
 * [positional] declares how the value should be threaded into the generated SQLAlchemy
 * constructor call: `true` -> `Numeric(10, 2)`, `false` -> `Numeric(asdecimal=True)`.
 * That can't be inferred at codegen time because SQLAlchemy mixes the two styles in a
 * single constructor.
 */
sealed interface TypeParameter {
    val id: String
    val label: String
    val description: String?
    val positional: Boolean
}

data class IntParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val positional: Boolean = true,
    val min: Int? = null,
    val max: Int? = null,
    val defaultValue: Int? = null,
    val optional: Boolean = true,
) : TypeParameter

data class BooleanParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val positional: Boolean = false,
    val defaultValue: Boolean = false,
) : TypeParameter

data class EnumParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val positional: Boolean = false,
    val values: List<String>,
    val defaultValue: String? = null,
) : TypeParameter

data class StringParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val positional: Boolean = false,
    val defaultValue: String? = null,
    val optional: Boolean = true,
) : TypeParameter

/**
 * A nested type — the user picks another [ColumnTypeDefinition] (recursively configured)
 * as the value. Used by container-shaped types like `ARRAY(item_type=...)`.
 *
 * [allowedTypeIds] restricts which definition ids may fill the slot; empty means any
 * type in the registry is allowed.
 */
data class TypeRefParameter(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val positional: Boolean = true,
    val allowedTypeIds: Set<String> = emptySet(),
) : TypeParameter

// ---------------------------------------------------------------------------
// Imports / annotations / instances
// ---------------------------------------------------------------------------

data class ImportDefinition(
    val module: String,
    val name: String,
)

/**
 * The user-configured values for an instance of a [ColumnTypeDefinition].
 *
 * [values] keys are [TypeParameter.id]s; values are loosely typed because SQLAlchemy params
 * are heterogeneous. [children] holds nested [TypeInstance]s for [TypeRefParameter] slots.
 */
data class TypeInstance(
    val definitionId: String,
    val values: Map<String, Any?> = emptyMap(),
    val children: Map<String, TypeInstance> = emptyMap(),
)

/**
 * Strict typed accessors for parameter values.
 *
 * Contract: `null` means *the parameter was not set* (callers should fall through to a default).
 * If the parameter is set but the value's runtime type doesn't match, we throw — silently
 * picking the wrong branch in an [AnnotationResolver] is a worse failure mode than crashing
 * loudly, because the bad codegen would only surface much later (e.g. as a Python TypeError
 * in the user's project).
 */
fun TypeInstance.bool(id: String): Boolean? = typedValue(id)
fun TypeInstance.int(id: String): Int? = typedValue(id)
fun TypeInstance.string(id: String): String? = typedValue(id)

private inline fun <reified T> TypeInstance.typedValue(id: String): T? {
    val raw = values[id] ?: return null
    return raw as? T
        ?: error("Parameter '$id' on '$definitionId' is ${raw::class.simpleName}, expected ${T::class.simpleName}")
}

/** A Python type annotation plus the imports it needs (e.g. `Decimal` -> `from decimal import Decimal`). */
data class AnnotationSpec(
    val type: String,
    val imports: List<ImportDefinition> = emptyList(),
)

/**
 * Resolves the Python annotation for an instantiated [ColumnTypeDefinition].
 *
 * The [registry] is passed so nested types (e.g. ARRAY's `item_type`) can look up their
 * inner definition without holding a reference to a global singleton.
 */
fun interface AnnotationResolver {
    fun resolve(instance: TypeInstance, registry: ColumnTypeRegistry): AnnotationSpec
}

// ---------------------------------------------------------------------------
// SQL DDL alias
// ---------------------------------------------------------------------------

/**
 * Declares that a raw SQL type name (case-insensitive, before any `(args)`) should be
 * parsed into a particular [ColumnTypeDefinition].
 *
 * [argsMatcher] is an optional predicate on the parser's argument list, used to
 * disambiguate flag-style overloads like MySQL `TINYINT(1)` (boolean) vs `TINYINT(3)`
 * (small integer).
 */
data class SqlAlias(
    val rawType: String,
    val argsMatcher: ((List<String>) -> Boolean)? = null,
)

// ---------------------------------------------------------------------------
// Docs wrapper
// ---------------------------------------------------------------------------


data class Docs(
    val cls: String,
    val text: String? = null,
)

// ---------------------------------------------------------------------------
// Column type definition
// ---------------------------------------------------------------------------

/**
 * One column type a user can pick in the model dialog (e.g. `Integer`, `Numeric`,
 * `PostgreSQL JSONB`).
 *
 * Dialect-specific definitions can declare [supersedes] = `id` of a generic definition
 * they should be preferred over when the matching dialect is active, so the type combo
 * shows e.g. `JSONB` *instead of* `JSON` when PostgreSQL is selected.
 */
data class ColumnTypeDefinition(
    val id: String,
    val name: String,
    val parameters: List<TypeParameter> = emptyList(),

    val sqlalchemyTypeName: String,

    val annotation: AnnotationResolver,

    val dialect: SqlDialect = SqlDialect.GENERIC,
    /** SQL DDL names this definition parses from; consumed by the SQL importer. */
    val sqlAliases: List<SqlAlias> = emptyList(),
    /**
     * Id of a generic [ColumnTypeDefinition] this dialect-specific type takes precedence
     * over when [dialect] is active. Only meaningful when [dialect] != GENERIC.
     */
    val supersedes: String? = null,

    /** HTML docs body used by tooltips/popovers. */
    val docs: Docs,
)

fun ColumnTypeDefinition.resolveAnnotation(
    instance: TypeInstance,
    registry: ColumnTypeRegistry,
): AnnotationSpec = annotation.resolve(instance, registry)

/**
 * Derived SQLAlchemy import for this type: `from <dialect.sqlalchemyModule> import <sqlalchemyTypeName>`.
 *
 * Previously every [ColumnTypeDefinition] had to spell its `sqlalchemyImports = listOf(...)`
 * out by hand; now the module comes from [SqlDialect.sqlalchemyModule] and the symbol from
 * [ColumnTypeDefinition.sqlalchemyTypeName] — the codegen / scraper just concatenates them.
 */
fun ColumnTypeDefinition.sqlalchemyImport(): ImportDefinition =
    ImportDefinition(module = dialect.sqlalchemyModule, name = sqlalchemyTypeName)
