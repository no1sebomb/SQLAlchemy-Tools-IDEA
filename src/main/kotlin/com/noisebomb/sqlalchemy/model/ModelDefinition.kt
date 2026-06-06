package com.noisebomb.sqlalchemy.model

data class ModelDefinition(
    var modelName: String,
    var tableName: String,
    val elements: MutableList<ModelElement>
)