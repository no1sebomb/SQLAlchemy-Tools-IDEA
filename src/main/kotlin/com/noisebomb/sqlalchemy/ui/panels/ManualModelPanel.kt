package com.noisebomb.sqlalchemy.ui.panels

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.noisebomb.sqlalchemy.generator.SqlAlchemyRenderer
import com.noisebomb.sqlalchemy.model.*
import com.noisebomb.sqlalchemy.ui.table.ColumnTableModel
import com.noisebomb.sqlalchemy.util.NamingUtils
import com.noisebomb.sqlalchemy.util.SimpleDocListener
import java.awt.BorderLayout
import javax.swing.*

class ManualModelPanel : JPanel(BorderLayout()) {

    private val modelNameField = JBTextField()
    private val tableNameField = JBTextField()

    private val columns = mutableListOf<ColumnDefinition>()
    private val tableModel = ColumnTableModel(columns)
    private val table = JBTable(tableModel)

    private val previewArea = JTextArea().apply {
        isEditable = false
    }

    init {
        val top = FormBuilder.createFormBuilder()
            .addLabeledComponent("Model Name:", modelNameField)
            .addLabeledComponent("Table Name:", tableNameField)
            .panel

        val centerSplit = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JScrollPane(table),
            JScrollPane(previewArea)
        )

        val addButton = JButton("+ Add Column")
        addButton.addActionListener {
            columns.add(
                ColumnDefinition(
                    name = "new_column",
                    type = ColumnType.STRING
                )
            )
            tableModel.fireTableDataChanged()
            updatePreview()
        }

        modelNameField.document.addDocumentListener(SimpleDocListener {
            syncNamesFromModel()
            updatePreview()
        })

        tableNameField.document.addDocumentListener(SimpleDocListener {
            syncNamesFromTable()
            updatePreview()
        })

        tableModel.columns.add(ColumnDefinition("id", ColumnType.INTEGER, primaryKey = true))

        add(top, BorderLayout.NORTH)
        add(centerSplit, BorderLayout.CENTER)
        add(addButton, BorderLayout.SOUTH)

        updatePreview()
    }

    private fun syncNamesFromModel() {
        val model = modelNameField.text.trim()
        if (tableNameField.text.isBlank()) {
            tableNameField.text = NamingUtils.toSnakeCase(model)
        }
        updatePreview()
    }

    private fun syncNamesFromTable() {
        val tableName = tableNameField.text.trim()
        if (modelNameField.text.isBlank()) {
            modelNameField.text = NamingUtils.toCamelCase(tableName)
        }
        updatePreview()
    }

    private fun updatePreview() {
        val model = buildModelDefinition() ?: return
        previewArea.text = SqlAlchemyRenderer.render(model)
    }

    fun buildModelDefinition(): ModelDefinition? {
        val name = modelNameField.text.trim()
        if (name.isEmpty()) return null

        return ModelDefinition(
            modelName = name,
            tableName = tableNameField.text.trim().ifEmpty {
                NamingUtils.toSnakeCase(name)
            },
            columns = columns
        )
    }
}