package com.noisebomb.sqlalchemy.model

import com.noisebomb.sqlalchemy.sql.SqlDialect


enum class SqlAlchemyGenerationMode {
    MANUAL,
    DATA_SOURCE,
    SQL
}

enum class SqlAlchemyColumnType(
    val displayName: String,
    val sqlalchemyType: String,
    val pythonType: String
) {
    INTEGER("Integer", "Integer", "int"),
    BIG_INTEGER("BigInteger", "BigInteger", "int"),
    STRING("String", "String", "str"),
    TEXT("Text", "Text", "str"),
    BOOLEAN("Boolean", "Boolean", "bool"),
    FLOAT("Float", "Float", "float"),
    NUMERIC("Numeric", "Numeric", "Decimal"),
    DATE("Date", "Date", "date"),
    DATETIME("DateTime", "DateTime", "datetime"),
    TIME("Time", "Time", "time"),
    JSON("JSON", "JSON", "dict"),
    UUID("UUID", "UUID", "UUID");

    override fun toString(): String = displayName
}

class SqlAlchemyColumnSpec(
    /** Python attribute name. */
    var name: String = "id",
    var type: SqlAlchemyColumnType = SqlAlchemyColumnType.INTEGER,
    var primaryKey: Boolean = false,
    var nullable: Boolean = true,
    var unique: Boolean = false,
    var defaultExpression: String = "",
    var comment: String = "",
    /** When true the database column name differs from the attribute name. */
    var columnNameDiffers: Boolean = false,
    /** Explicit database column name (used only when [columnNameDiffers] is true). */
    var columnName: String = ""
)

data class SqlAlchemyModelSpec(
    val mode: SqlAlchemyGenerationMode,
    val modelName: String,
    val tableName: String,
    val fileName: String,
    val modelComment: String,
    val wrapColumns: Boolean = true,
    val attributeTypesMapping: Boolean = true,
    val useLegacyColumns: Boolean = false,
    val fileCodingHeader: Boolean = false,
    /** Dialect the SQL (DDL) source was interpreted as; irrelevant outside [SqlAlchemyGenerationMode.SQL]. */
    val sqlDialect: SqlDialect = SqlDialect.GENERIC,
    val columns: List<SqlAlchemyColumnSpec>
)
