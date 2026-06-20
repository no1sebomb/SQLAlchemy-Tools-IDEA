package com.noisebomb.sqlalchemy.model

import com.noisebomb.sqlalchemy.sql.types.BooleanParameter
import com.noisebomb.sqlalchemy.sql.types.ColumnTypeDefinition
import com.noisebomb.sqlalchemy.sql.types.EnumParameter
import com.noisebomb.sqlalchemy.sql.types.IntParameter
import com.noisebomb.sqlalchemy.sql.types.StringParameter
import com.noisebomb.sqlalchemy.sql.types.TypeInstance
import com.noisebomb.sqlalchemy.sql.types.TypeRefParameter

/**
 * Compatibility bridge between the new rich [ColumnTypeDefinition] / [TypeInstance] model and the
 * legacy [SqlAlchemyColumnType] enum that
 * [com.noisebomb.sqlalchemy.generation.SqlAlchemyCodeGenerator] still reads.
 *
 * When the user picks a type in the dialog or the SQL parser maps a DDL alias, we update both
 * [SqlAlchemyColumnSpec.typeInstance] and [SqlAlchemyColumnSpec.type] through this table so the
 * generator keeps emitting the right Python imports/types until it's rewritten to consume
 * `typeInstance` directly.
 */

/**
 * Maps each [ColumnTypeDefinition] id we ship to its closest legacy enum. Definitions with no
 * good legacy equivalent fall back to [SqlAlchemyColumnType.STRING] (matches the existing SQL
 * parser's "unknown type" default).
 */
private val DEFINITION_ID_TO_LEGACY: Map<String, SqlAlchemyColumnType> = mapOf(
    "biginteger" to SqlAlchemyColumnType.BIG_INTEGER,
    "boolean" to SqlAlchemyColumnType.BOOLEAN,
    "date" to SqlAlchemyColumnType.DATE,
    "datetime" to SqlAlchemyColumnType.DATETIME,
    "double" to SqlAlchemyColumnType.FLOAT,
    "float" to SqlAlchemyColumnType.FLOAT,
    "integer" to SqlAlchemyColumnType.INTEGER,
    "numeric" to SqlAlchemyColumnType.NUMERIC,
    "small_integer" to SqlAlchemyColumnType.INTEGER,
    "string" to SqlAlchemyColumnType.STRING,
    "text" to SqlAlchemyColumnType.TEXT,
    "time" to SqlAlchemyColumnType.TIME,
    "unicode" to SqlAlchemyColumnType.STRING,
    "unicode_text" to SqlAlchemyColumnType.TEXT,
    "uuid" to SqlAlchemyColumnType.UUID,
    "json" to SqlAlchemyColumnType.JSON,
    "pg_jsonb" to SqlAlchemyColumnType.JSON,
    // interval / large_binary / pickle / array fall back to STRING until codegen migrates.
)

fun legacyEnumFor(definitionId: String): SqlAlchemyColumnType =
    DEFINITION_ID_TO_LEGACY[definitionId] ?: SqlAlchemyColumnType.STRING

/**
 * Builds a fresh [TypeInstance] for this definition, pre-populating each parameter's default so
 * the annotation resolver sees a consistent state (e.g. `Numeric.asdecimal=true` by default).
 *
 * Optional parameters with no declared default are left absent — the SQL parser fills them in
 * from parsed args, or the user types a value; `null` consistently means "use the SQLAlchemy
 * default" downstream.
 */
fun ColumnTypeDefinition.newInstance(): TypeInstance {
    val values = mutableMapOf<String, Any?>()
    for (param in parameters) {
        when (param) {
            is BooleanParameter -> values[param.id] = param.defaultValue
            is IntParameter -> param.defaultValue?.let { values[param.id] = it }
            is StringParameter -> param.defaultValue?.let { values[param.id] = it }
            is EnumParameter -> param.defaultValue?.let { values[param.id] = it }
            is TypeRefParameter -> Unit // nested instances stay absent until the user picks one
        }
    }
    return TypeInstance(definitionId = id, values = values)
}
