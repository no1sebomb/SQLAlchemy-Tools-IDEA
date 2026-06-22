package com.noisebomb.sqlalchemy.tools

import com.noisebomb.sqlalchemy.sql.SqlDialect
import com.noisebomb.sqlalchemy.sql.types.BooleanParameter
import com.noisebomb.sqlalchemy.sql.types.ColumnTypeDefinition
import com.noisebomb.sqlalchemy.sql.types.ColumnTypes
import com.noisebomb.sqlalchemy.sql.types.EnumParameter
import com.noisebomb.sqlalchemy.sql.types.IntParameter
import com.noisebomb.sqlalchemy.sql.types.StringParameter
import com.noisebomb.sqlalchemy.sql.types.TypeParameter
import com.noisebomb.sqlalchemy.sql.types.TypeRefParameter

/**
 * Dumps the column-type registry as JSON to stdout. Intended to be run from Gradle:
 *
 *     ./gradlew dumpTypeManifest        →  build/types-manifest.json
 *
 * The manifest is the contract between the Kotlin plugin (source of truth) and an external
 * Python docs-scraper. Python reads it, fetches the matching SQLAlchemy docs anchor for each
 * type, and writes `src/main/resources/sqlalchemy-docs/<dialect>/<id>.html`. The plugin then
 * loads those HTML files at runtime via `Docs.fromResource(...)`.
 *
 * Shape:
 * ```json
 * {
 *   "dialects": [
 *     {
 *       "id": "GENERIC",
 *       "docsUrl": "https://docs.sqlalchemy.org/en/21/core/type_basics.html",
 *       "types": [
 *         {
 *           "id": "numeric",
 *           "name": "Numeric",
 *           "sqlalchemyTypeName": "Numeric",
 *           "class": "sqlalchemy.types.Numeric",
 *           "resourcePath": "generic/numeric.html",
 *           "parameters": [
 *             { "id": "precision", "label": "Precision", "kind": "Int",
 *               "positional": true, "optional": true, "min": 1 },
 *             { "id": "asdecimal", "label": "As decimal", "kind": "Boolean",
 *               "positional": false, "default": true }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * ```
 */
fun main() {
    val byDialect: Map<SqlDialect, List<ColumnTypeDefinition>> =
        ColumnTypes.REGISTRY.all.groupBy { it.dialect }
    // Stable ordering — keep the dialect enum order, not the hashmap iteration order, so diffs
    // on regeneration stay readable.
    val dialectsOrdered = SqlDialect.entries.filter { byDialect.containsKey(it) }

    val root = linkedMapOf<String, Any?>(
        "dialects" to dialectsOrdered.map { dialect ->
            linkedMapOf<String, Any?>(
                "id" to dialect.name,
                "docsUrl" to ColumnTypes.docsUrlFor(dialect).orEmpty(),
                // Per-dialect Python module — each type's SQLAlchemy import is now derived as
                // `from $sqlalchemyModule import $sqlalchemyTypeName` instead of being repeated
                // on every ColumnTypeDefinition.
                "sqlalchemyModule" to dialect.sqlalchemyModule,
                "types" to byDialect.getValue(dialect).map(::serializeType),
            )
        },
    )
    println(writeJson(root, indent = 0))
}

private fun serializeType(def: ColumnTypeDefinition): Map<String, Any?> = linkedMapOf(
    "id" to def.id,
    "name" to def.name,
    "sqlalchemyTypeName" to def.sqlalchemyTypeName,
    "class" to def.docs.cls,
    "resourcePath" to "${folderFor(def.dialect)}/${def.id}.html",
    "parameters" to def.parameters.map(::serializeParameter),
)

private fun serializeParameter(param: TypeParameter): Map<String, Any?> {
    val base = linkedMapOf<String, Any?>(
        "id" to param.id,
        "label" to param.label,
        "kind" to when (param) {
            is IntParameter -> "Int"
            is BooleanParameter -> "Boolean"
            is StringParameter -> "String"
            is EnumParameter -> "Enum"
            is TypeRefParameter -> "TypeRef"
        },
        "positional" to param.positional,
    )
    when (param) {
        is IntParameter -> {
            base["optional"] = param.optional
            param.min?.let { base["min"] = it }
            param.max?.let { base["max"] = it }
            param.defaultValue?.let { base["default"] = it }
        }
        is BooleanParameter -> {
            base["default"] = param.defaultValue
        }
        is StringParameter -> {
            base["optional"] = param.optional
            param.defaultValue?.let { base["default"] = it }
        }
        is EnumParameter -> {
            base["values"] = param.values
            param.defaultValue?.let { base["default"] = it }
        }
        is TypeRefParameter -> {
            if (param.allowedTypeIds.isNotEmpty()) base["allowedTypeIds"] = param.allowedTypeIds.toList()
        }
    }
    return base
}

/**
 * Per-dialect resource subfolder. The Python scraper writes the scraped HTML to this folder
 * and the plugin loads it from the same path via [com.noisebomb.sqlalchemy.sql.types.Docs.fromResource].
 */
private fun folderFor(d: SqlDialect): String = when (d) {
    SqlDialect.GENERIC -> "generic"
    SqlDialect.POSTGRESQL -> "postgresql"
    SqlDialect.MYSQL -> "mysql"
    SqlDialect.MARIADB -> "mariadb"
    SqlDialect.SQL_SERVER -> "sqlserver"
    SqlDialect.ORACLE -> "oracle"
    SqlDialect.SQLITE -> "sqlite"
}

// ---------------------------------------------------------------------------
// Tiny JSON emitter — no runtime dep, no Jackson, just enough for the manifest.
// ---------------------------------------------------------------------------

private fun writeJson(value: Any?, indent: Int): String {
    val sb = StringBuilder()
    appendJson(sb, value, indent)
    return sb.toString()
}

private fun appendJson(sb: StringBuilder, value: Any?, indent: Int) {
    when (value) {
        null -> sb.append("null")
        is Boolean -> sb.append(value.toString())
        is Number -> sb.append(value.toString())
        is String -> appendString(sb, value)
        is Map<*, *> -> appendObject(sb, value, indent)
        is Collection<*> -> appendArray(sb, value, indent)
        else -> appendString(sb, value.toString())
    }
}

private fun appendString(sb: StringBuilder, s: String) {
    sb.append('"')
    for (c in s) {
        when {
            c == '\\' -> sb.append("\\\\")
            c == '"' -> sb.append("\\\"")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c.code < 0x20 -> sb.append("\\u").append("%04x".format(c.code))
            else -> sb.append(c)
        }
    }
    sb.append('"')
}

private fun appendObject(sb: StringBuilder, map: Map<*, *>, indent: Int) {
    if (map.isEmpty()) {
        sb.append("{}")
        return
    }
    sb.append("{\n")
    val pad = "  ".repeat(indent + 1)
    val entries = map.entries.toList()
    entries.forEachIndexed { i, (k, v) ->
        sb.append(pad)
        appendString(sb, k.toString())
        sb.append(": ")
        appendJson(sb, v, indent + 1)
        if (i < entries.size - 1) sb.append(",")
        sb.append('\n')
    }
    sb.append("  ".repeat(indent)).append("}")
}

private fun appendArray(sb: StringBuilder, items: Collection<*>, indent: Int) {
    if (items.isEmpty()) {
        sb.append("[]")
        return
    }
    sb.append("[\n")
    val pad = "  ".repeat(indent + 1)
    val list = items.toList()
    list.forEachIndexed { i, v ->
        sb.append(pad)
        appendJson(sb, v, indent + 1)
        if (i < list.size - 1) sb.append(",")
        sb.append('\n')
    }
    sb.append("  ".repeat(indent)).append("]")
}
