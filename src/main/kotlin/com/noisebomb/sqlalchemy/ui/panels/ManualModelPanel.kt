package com.noisebomb.sqlalchemy.ui.panels

import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.noisebomb.sqlalchemy.generator.SqlAlchemyRenderer
import com.noisebomb.sqlalchemy.model.*
import com.noisebomb.sqlalchemy.ui.table.ColumnTableModel
import com.noisebomb.sqlalchemy.ui.table.ColumnTypeCellEditor
import com.noisebomb.sqlalchemy.ui.table.BooleanCellRenderer
import com.noisebomb.sqlalchemy.util.NamingUtils
import com.noisebomb.sqlalchemy.util.SimpleDocListener
import java.awt.BorderLayout
import javax.swing.*

class ManualModelPanel : JPanel(BorderLayout()) {

    private val modelNameField = JBTextField()
    private val tableNameField = JBTextField()
    private var modelManuallyEdited = false
    private var tableManuallyEdited = false

    private val columns = mutableListOf<ColumnDefinition>()
    private val tableModel = ColumnTableModel(columns)
    private val table = JBTable(tableModel)

    private val previewArea = JTextArea().apply {
        isEditable = false
    }

    init {
        layout = BorderLayout()

        // Customize table
        table.setDefaultEditor(ColumnType::class.java, ColumnTypeCellEditor())
        table.setDefaultEditor(Boolean::class.java, DefaultCellEditor(JCheckBox()))
        table.setDefaultRenderer(Boolean::class.java, BooleanCellRenderer())

        table.showVerticalLines = false
        table.showHorizontalLines = true
        table.tableHeader.reorderingAllowed = false
        table.fillsViewportHeight = true

        val header = buildHeader()
        modelNameField.document.addDocumentListener(SimpleDocListener {
            modelManuallyEdited = true
            syncNamesFromModel()
        })
        tableNameField.document.addDocumentListener(SimpleDocListener {
            tableManuallyEdited = true
            syncNamesFromTable()
        })

        val tablePanel = JScrollPane(table)
        val addButton = JButton("+ Add Column")
        addButton.addActionListener {
            columns.add(
                ColumnDefinition(
                    name = "new_column",
                    type = ColumnType.STRING,
                    nullable = false,
                    primaryKey = false,
                    unique = false
                )
            )

            tableModel.fireTableDataChanged()
            updatePreview()
        }
        tableModel.addTableModelListener {
            updatePreview()
        }

        val previewPanel = JScrollPane(previewArea)

        val center = JPanel(BorderLayout())
        center.add(tablePanel, BorderLayout.CENTER)
        center.add(addButton, BorderLayout.SOUTH)

        val main = JPanel(BorderLayout())
        main.add(header, BorderLayout.NORTH)
        main.add(center, BorderLayout.CENTER)
        main.add(previewPanel, BorderLayout.SOUTH)

        add(main)
    }

    private fun buildHeader(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Model Name:", modelNameField)
            .addLabeledComponent("Table Name:", tableNameField)
            .panel
    }

    private fun syncNamesFromModel() {
        val model = modelNameField.text.trim()
        if (model.isEmpty()) return

        if (!tableManuallyEdited) {
            tableNameField.text = NamingUtils.toSnakeCase(model)
        }

        updatePreview()
    }

    private fun syncNamesFromTable() {
        val table = tableNameField.text.trim()
        if (table.isEmpty()) return

        if (!modelManuallyEdited) {
            modelNameField.text = NamingUtils.toCamelCase(table)
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