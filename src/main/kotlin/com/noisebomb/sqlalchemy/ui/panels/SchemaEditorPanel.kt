package com.noisebomb.sqlalchemy.ui.panels

import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import com.noisebomb.sqlalchemy.generator.SqlAlchemyRenderer
import com.noisebomb.sqlalchemy.model.*
import com.noisebomb.sqlalchemy.ui.list.ModelElementListModel
import com.noisebomb.sqlalchemy.util.SimpleDocListener
import java.awt.BorderLayout
import javax.swing.*

class SchemaEditorPanel : JPanel(BorderLayout()) {

    private val elements = mutableListOf<ModelElement>()
    private val listModel = ModelElementListModel(elements)
    private val list = JBList(listModel)

    private val editorPanel = ColumnEditorPanel()
    private val preview = JTextArea().apply { isEditable = false }

    private var selectedColumn: ColumnDefinition? = null
    private val modelNameField = JBTextField()
    private val tableNameField = JBTextField()

    init {
        border = JBUI.Borders.empty(8)

        val leftToolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            add(createBtn("+") { addColumn() })
            add(createBtn("-") { removeSelected() })
            add(createBtn("↑") { moveUp() })
            add(createBtn("↓") { moveDown() })
        }

        val leftPanel = JPanel(BorderLayout()).apply {
            add(leftToolbar, BorderLayout.NORTH)
            add(JScrollPane(list), BorderLayout.CENTER)
        }

        val rightPanel = editorPanel

        val split = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftPanel,
            rightPanel
        )

        val previewPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(8)
            add(JLabel("Preview"), BorderLayout.NORTH)
            add(JScrollPane(preview), BorderLayout.CENTER)
        }

        add(split, BorderLayout.CENTER)
        add(previewPanel, BorderLayout.SOUTH)

        list.addListSelectionListener {
            val selected = list.selectedValue
            if (selected is ColumnDefinition) {
                selectedColumn = selected
                editorPanel.bind(selected)
                editorPanel.revalidate()
                editorPanel.repaint()
                updatePreview()
            }
        }

        editorPanel.onChange {
            selectedColumn?.let {
                editorPanel.applyTo(it)
                updatePreview()
            }
        }

        modelNameField.document.addDocumentListener(SimpleDocListener {
            updatePreview()
        })

        tableNameField.document.addDocumentListener(SimpleDocListener {
            updatePreview()
        })
    }

    private fun createBtn(text: String, action: () -> Unit): JButton {
        return JButton(text).apply { addActionListener { action() } }
    }

    private fun addColumn() {
        val col = ColumnDefinition(
            name = "column",
            type = ColumnType.STRING
        )

        listModel.add(col)
        updatePreview()
    }

    private fun removeSelected() {
        val idx = list.selectedIndex
        if (idx >= 0) listModel.remove(idx)
        updatePreview()
    }

    private fun moveUp() {
        val idx = list.selectedIndex
        listModel.moveUp(idx)
        list.selectedIndex = (idx - 1).coerceAtLeast(0)
        updatePreview()
    }

    private fun moveDown() {
        val idx = list.selectedIndex
        listModel.moveDown(idx)
        list.selectedIndex = idx + 1
        updatePreview()
    }

    private fun updatePreview() {
        val model = ModelDefinition(
            modelName = modelNameField.text.ifBlank { "Model" },
            tableName = tableNameField.text.ifBlank { "model" },
            elements = elements
        )

        preview.text = SqlAlchemyRenderer.render(model)
    }

    fun buildModelDefinition(): ModelDefinition {
        return ModelDefinition(
            modelName = modelNameField.text.ifBlank { "Model" },
            tableName = tableNameField.text.ifBlank { "model" },
            elements = elements
        )
    }
}