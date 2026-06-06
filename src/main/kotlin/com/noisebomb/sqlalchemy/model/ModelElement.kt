package com.noisebomb.sqlalchemy.model

sealed class ModelElement {
    abstract var name: String
}

data class ColumnDefinition(
    override var name: String,
    var type: ColumnType,
    var nullable: Boolean = false,
    var primaryKey: Boolean = false,
    var unique: Boolean = false,
    var defaultValue: String? = null,
    var comment: String? = null
) : ModelElement()

data class IndexDefinition(
    override var name: String,
    val columns: List<String>
) : ModelElement()
