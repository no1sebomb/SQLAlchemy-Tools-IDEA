package com.noisebomb.sqlalchemy.model

data class ModelDefinition(
    val modelName: String,
    val tableName: String,
    val columns: MutableList<ColumnDefinition>
)

data class ColumnDefinition(
    var name: String,
    var type: ColumnType,
    var nullable: Boolean = true,
    var primaryKey: Boolean = false,
    var unique: Boolean = false,
    var defaultValue: String? = null
)

enum class ColumnType {
    INTEGER,
    BIG_INTEGER,
    STRING,
    TEXT,
    BOOLEAN,
    DATE,
    DATETIME,
    UUID,
    FLOAT,
    DECIMAL
}
