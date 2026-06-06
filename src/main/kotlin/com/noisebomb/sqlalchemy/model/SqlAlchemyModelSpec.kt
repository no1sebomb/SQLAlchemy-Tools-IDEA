package com.noisebomb.sqlalchemy.model

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
    var name: String = "id",
    var type: SqlAlchemyColumnType = SqlAlchemyColumnType.INTEGER,
    var primaryKey: Boolean = false,
    var nullable: Boolean = true,
    var unique: Boolean = false,
    var defaultExpression: String = "",
    var comment: String = ""
)

data class SqlAlchemyModelSpec(
    val mode: SqlAlchemyGenerationMode,
    val modelName: String,
    val tableName: String,
    val fileName: String,
    val modelComment: String,
    val attributeTypesMapping: Boolean = true,
    val useLegacyColumns: Boolean = false,
    val columns: List<SqlAlchemyColumnSpec>
)

