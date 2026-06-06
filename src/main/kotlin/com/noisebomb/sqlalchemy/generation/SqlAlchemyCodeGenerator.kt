package com.noisebomb.sqlalchemy.generation

import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnSpec
import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnType
import com.noisebomb.sqlalchemy.model.SqlAlchemyModelSpec

object SqlAlchemyCodeGenerator {
    fun generate(spec: SqlAlchemyModelSpec): String {
        val columnTypes = spec.columns.map { it.type.sqlalchemyType }.toSortedSet()
        val sqlalchemyImports = buildList {
            if (spec.useLegacyColumns) add("Column")
            addAll(columnTypes)
        }
        val importTypes = if (sqlalchemyImports.isEmpty()) "" else "from sqlalchemy import ${sqlalchemyImports.joinToString(", ")}\n"

        val additionalImports = buildAdditionalImports(spec.columns)
        val body = if (spec.columns.isEmpty()) {
            "    pass"
        } else {
            spec.columns.joinToString("\n") { renderColumn(it, spec) }
        }

        return buildString {
            append(importTypes)
            if (spec.useLegacyColumns) {
                // legacy mode uses Column directly
            } else if (spec.attributeTypesMapping) {
                append("from sqlalchemy.orm import Mapped, mapped_column\n")
            } else {
                append("from sqlalchemy.orm import mapped_column\n")
            }
            additionalImports.forEach { append(it).append('\n') }
            append("\n")
            append("from db.base import Base\n\n")
            append("class ${spec.modelName}(Base):\n")
            if (spec.modelComment.isNotBlank()) {
                append("    \"\"\"${escapeDocString(spec.modelComment.trim())}\"\"\"\n")
            }
            append("    __tablename__ = \"${spec.tableName}\"\n\n")
            append(body)
            append('\n')
        }
    }

    private fun renderColumn(column: SqlAlchemyColumnSpec, spec: SqlAlchemyModelSpec): String {
        val args = mutableListOf<String>()
        args += column.type.sqlalchemyType

        if (column.primaryKey) {
            args += "primary_key=True"
        }

        if (!column.nullable || column.primaryKey) {
            args += "nullable=False"
        }

        if (column.unique) {
            args += "unique=True"
        }

        if (column.defaultExpression.isNotBlank()) {
            args += "default=${column.defaultExpression.trim()}"
        }

        if (column.comment.isNotBlank()) {
            args += "comment=\"${escapeString(column.comment.trim())}\""
        }

        return if (column.name.isBlank()) {
            "    # unnamed column"
        } else if (spec.useLegacyColumns) {
            "    ${column.name} = Column(${args.joinToString(", ")})"
        } else if (spec.attributeTypesMapping) {
            "    ${column.name}: Mapped[${column.type.pythonType}] = mapped_column(${args.joinToString(", ")})"
        } else {
            "    ${column.name} = mapped_column(${args.joinToString(", ")})"
        }
    }

    private fun buildAdditionalImports(columns: List<SqlAlchemyColumnSpec>): List<String> {
        val needsDecimal = columns.any { it.type == SqlAlchemyColumnType.NUMERIC }
        val needsDate = columns.any { it.type == SqlAlchemyColumnType.DATE }
        val needsDateTime = columns.any { it.type == SqlAlchemyColumnType.DATETIME }
        val needsTime = columns.any { it.type == SqlAlchemyColumnType.TIME }
        val needsUuid = columns.any { it.type == SqlAlchemyColumnType.UUID }

        return buildList {
            if (needsDecimal) add("from decimal import Decimal")
            if (needsDate) add("from datetime import date")
            if (needsDateTime) add("from datetime import datetime")
            if (needsTime) add("from datetime import time")
            if (needsUuid) add("from uuid import UUID")
        }
    }

    private fun escapeString(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun escapeDocString(value: String): String = value.replace("\"\"\"", "\\\"\\\"\\\"")
}

