package com.noisebomb.sqlalchemy.generator

import com.noisebomb.sqlalchemy.model.ColumnType
import com.noisebomb.sqlalchemy.model.ModelDefinition

object SqlAlchemyRenderer {

    fun render(model: ModelDefinition): String {
        val className = model.modelName

        val columns = model.columns.joinToString("\n\n") { renderColumn(it) }

        return """
            |from sqlalchemy.orm import Mapped, mapped_column
            |from sqlalchemy import String, Integer, Boolean, DateTime, Float, Text
            |
            |from db.base import Base
            |
            |
            |class $className(Base):
            |    __tablename__ = "${model.tableName}"
            |
            |    $columns
        """.trimMargin()
    }

    private fun renderColumn(col: com.noisebomb.sqlalchemy.model.ColumnDefinition): String {
        val pythonType = when (col.type) {
            ColumnType.INTEGER -> "int"
            ColumnType.BIG_INTEGER -> "int"
            ColumnType.STRING -> "str"
            ColumnType.TEXT -> "str"
            ColumnType.BOOLEAN -> "bool"
            ColumnType.DATE -> "datetime.date"
            ColumnType.DATETIME -> "datetime.datetime"
            ColumnType.UUID -> "uuid.UUID"
            ColumnType.FLOAT -> "float"
            ColumnType.DECIMAL -> "float"
        }

        val saType = when (col.type) {
            ColumnType.INTEGER -> "Integer"
            ColumnType.BIG_INTEGER -> "Integer"
            ColumnType.STRING -> "String(255)"
            ColumnType.TEXT -> "Text"
            ColumnType.BOOLEAN -> "Boolean"
            ColumnType.DATE -> "DateTime"
            ColumnType.DATETIME -> "DateTime"
            ColumnType.UUID -> "String(36)"
            ColumnType.FLOAT -> "Float"
            ColumnType.DECIMAL -> "Float"
        }

        val attrs = mutableListOf<String>()

        attrs.add(saType)

        if (col.primaryKey) attrs.add("primary_key=True")
        if (col.nullable) attrs.add("nullable=True")
        if (col.unique) attrs.add("unique=True")

        val attrString = attrs.joinToString(",\n        ")

        return """
        |${col.name}: Mapped[$pythonType] = mapped_column(
        |        $attrString
        |    )
        """.trimMargin()
    }
}