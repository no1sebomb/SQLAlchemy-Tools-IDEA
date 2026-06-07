package com.noisebomb.sqlalchemy.generation

import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnSpec
import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnType
import com.noisebomb.sqlalchemy.model.SqlAlchemyModelSpec



object SqlAlchemyCodeGenerator {

    fun generate(spec: SqlAlchemyModelSpec): String {
        // Prepare SQLAlchemy imports
        val sqlAlchemyImports = buildList {
            if (spec.useLegacyColumns) add("Column")
        }
        val sqlAlchemyOrmImports = buildList {
            if (spec.attributeTypesMapping) add("Mapped")
            if (!spec.useLegacyColumns) add("mapped_column")
        }
        val sqlAlchemyTypes = spec.columns.map { it.type.sqlalchemyType }.toSortedSet()

        // Prepare additional imports (for column typing)
        val additionalImports = buildAdditionalImports(spec.columns)

        return buildString {
            // File header
            if (spec.fileCodingHeader) {
                append("# coding=utf-8\n\n")
            }

            // Built-in imports
            if (additionalImports.isNotEmpty()) {
                additionalImports.forEach { append(it).append("\n") }
                append("\n")
            }

            // Library imports (SQLAlchemy)
            if (sqlAlchemyImports.isNotEmpty()) {
                append("from sqlalchemy import " + sqlAlchemyImports.joinToString(", ") + "\n")
            }
            if (sqlAlchemyOrmImports.isNotEmpty()) {
                append("from sqlalchemy.orm import " + sqlAlchemyOrmImports.joinToString(", ") + "\n")
            }
            if (sqlAlchemyTypes.isNotEmpty()) {
                append("from sqlalchemy.types import " + sqlAlchemyTypes.joinToString(", ") + "\n")
            }

            // There should be at least one SQLAlchemy import, so we need to add a blank line after them
            append("\n")

            // Add base mapper import
            append("from db.base import Base\n")

            // Two blank lines
            append("\n")
            append("\n")

            // Render class
            append(renderBody(spec))

            // Render each column
            spec.columns.forEach { append(renderColumn(it, spec)) }
        }
    }

    private fun renderBody(spec: SqlAlchemyModelSpec): String {
        return buildString {
            // Class header
            append("class ${spec.modelName}(Base):\n")

            if (spec.modelComment.isNotBlank() or spec.columns.any {it.comment.isNotBlank()}) {
                // Render class docstring
                append("    \"\"\"\n")

                if (spec.modelComment.isNotBlank()) {
                    // Add docstring body
                    for (line in spec.modelComment.trim().lines()) {
                        append("    ")
                        append(line.trimEnd())
                        append("\n")
                    }
                }

                if (spec.columns.any {it.comment.isNotBlank()}) {
                    // Add Attributes
                    if (spec.modelComment.isNotBlank()) {
                        // We should add blank line
                        append("\n")
                    }

                    append("    Attributes:\n")

                    for (column in spec.columns) {
                        if (column.comment.isNotBlank()) {
                            // Add attribute docstring
                            append("        ${column.name}:")

                            if (Regex("\\R").containsMatchIn(column.comment.trimEnd())) {
                                // Multiline comment => add indentation
                                for (line in column.comment.trim().lines()) {
                                    append("\n            ${escapeString(line.trimEnd())}")
                                }
                            }
                            else {
                                // Single line comment
                                append(" ${escapeDocString(column.comment.trim())}")
                            }

                            // Line break
                            append("\n")
                        }
                    }
                }

                // Close docstring + blank line
                append("    \"\"\"\n")
                append("\n")
            }

            append("    __tablename__ = \"${spec.tableName}\"\n\n")
        }
    }

    private fun renderColumn(column: SqlAlchemyColumnSpec, spec: SqlAlchemyModelSpec): String {
        val annotation = if (spec.attributeTypesMapping) ": Mapped[${column.type.pythonType}]" else ""
        val columnClass = if (spec.useLegacyColumns) "Column" else "mapped_column"
        val args = buildList {
            if (column.columnName.isNotBlank()) { add(column.columnName.trim()) }
            add(column.type.sqlalchemyType)
            if (column.primaryKey) add("primary_key=True")
            if (column.unique) add("unique=True")
            if (!column.nullable || column.primaryKey) add("nullable=False")
            if (column.defaultExpression.isNotBlank()) {
                add("default=${escapeString(column.defaultExpression.trim())}")
            }
        }
        val argsString = if (spec.wrapColumns) "\n        ${args.joinToString(",\n        ")}\n    " else args.joinToString(", ")

        return "    ${column.name}${annotation} = ${columnClass}($argsString)\n"
    }

    private fun buildAdditionalImports(columns: List<SqlAlchemyColumnSpec>): List<String> {
        val needsDecimal = columns.any { it.type == SqlAlchemyColumnType.NUMERIC }
        val needsDate = columns.any { it.type == SqlAlchemyColumnType.DATE }
        val needsDateTime = columns.any { it.type == SqlAlchemyColumnType.DATETIME }
        val needsTime = columns.any { it.type == SqlAlchemyColumnType.TIME }
        val needsUuid = columns.any { it.type == SqlAlchemyColumnType.UUID }

        return buildList {
            if (needsUuid) add("from uuid import UUID")
            if (needsDecimal) add("from decimal import Decimal")
            if (needsTime or needsDate or needsDateTime) {
                // Construct import for all specified types
                val dateTimeTypes = buildList {
                    if (needsTime) add("time")
                    if (needsDate) add("date")
                    if (needsDateTime) add("datetime")
                }
                add("from datetime import " + dateTimeTypes.joinToString(", "))
            }
        }
    }

    private fun escapeString(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun escapeDocString(value: String): String = value.replace("\"\"\"", "\\\"\\\"\\\"")
}
