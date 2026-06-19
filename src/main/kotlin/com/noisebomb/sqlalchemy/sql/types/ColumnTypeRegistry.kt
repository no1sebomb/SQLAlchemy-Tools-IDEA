package com.noisebomb.sqlalchemy.sql.types

import com.noisebomb.sqlalchemy.sql.SqlDialect

/**
 * In-memory index over [ColumnTypeDefinition]s.
 *
 * Built once from a fixed input list (one per dialect file), then queried by
 *  - id (the canonical identity used by [TypeInstance.definitionId]),
 *  - active dialect (with generic types suppressed when a dialect-specific equivalent
 *    declares [ColumnTypeDefinition.supersedes]),
 *  - raw SQL DDL type name (for the SQL importer; not wired in yet).
 */
class ColumnTypeRegistry(definitions: List<ColumnTypeDefinition>) {

    val all: List<ColumnTypeDefinition> = definitions
    private val byId: Map<String, ColumnTypeDefinition> = definitions.associateBy { it.id }

    init {
        require(byId.size == definitions.size) {
            val dupes = definitions.groupBy { it.id }.filter { it.value.size > 1 }.keys
            "Duplicate ColumnTypeDefinition ids: $dupes"
        }
        // supersedes is easy to typo; catch dangling/wrong references at plugin load instead of
        // silently producing an empty type list for some dialect.
        for (def in definitions) {
            val target = def.supersedes ?: continue
            val targetDef = byId[target]
                ?: error("'${def.id}' supersedes unknown type id '$target'")
            require(targetDef.dialect == SqlDialect.GENERIC) {
                "'${def.id}' supersedes '$target', but '$target' is dialect-specific (${targetDef.dialect}); only generic types may be superseded"
            }
        }
    }

    fun findById(id: String): ColumnTypeDefinition? = byId[id]

    fun getById(id: String): ColumnTypeDefinition =
        byId[id] ?: error("Unknown ColumnTypeDefinition id: $id")

    /**
     * Returns the types that should be offered when [dialect] is active: every dialect-specific
     * type for that dialect plus every generic type not superseded by one of them. Order is
     * dialect-specific first, then generics, preserving the input order within each group.
     */
    fun forDialect(dialect: SqlDialect): List<ColumnTypeDefinition> {
        if (dialect == SqlDialect.GENERIC) return all.filter { it.dialect == SqlDialect.GENERIC }
        val dialectTypes = all.filter { it.dialect == dialect }
        val superseded = dialectTypes.mapNotNull { it.supersedes }.toSet()
        val generics = all.filter { it.dialect == SqlDialect.GENERIC && it.id !in superseded }
        return dialectTypes + generics
    }

    /**
     * Finds the best [ColumnTypeDefinition] for a raw SQL type, preferring the active [dialect]
     * over generic definitions. Returns `null` if no alias matches.
     */
    fun findForSqlType(
        rawType: String,
        args: List<String>,
        dialect: SqlDialect,
    ): ColumnTypeDefinition? {
        val target = rawType.trim().uppercase()
        if (dialect != SqlDialect.GENERIC) {
            all.firstOrNull { it.dialect == dialect && it.matchesSql(target, args) }?.let { return it }
        }
        return all.firstOrNull { it.dialect == SqlDialect.GENERIC && it.matchesSql(target, args) }
    }

    private fun ColumnTypeDefinition.matchesSql(target: String, args: List<String>): Boolean =
        sqlAliases.any { alias ->
            alias.rawType.equals(target, ignoreCase = true) &&
                (alias.argsMatcher?.invoke(args) ?: true)
        }
}
