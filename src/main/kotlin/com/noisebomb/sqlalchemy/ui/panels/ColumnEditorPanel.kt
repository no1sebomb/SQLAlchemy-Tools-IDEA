package com.noisebomb.sqlalchemy.ui.panels

import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import com.intellij.openapi.ui.ComboBox
import com.noisebomb.sqlalchemy.model.ColumnDefinition
import com.noisebomb.sqlalchemy.model.ColumnType
import com.noisebomb.sqlalchemy.util.SimpleDocListener
import java.awt.BorderLayout
import javax.swing.*

class ColumnEditorPanel : JPanel(BorderLayout()) {

    private val nameField = JBTextField()
    private val typeBox = ComboBox(ColumnType.values())
    private val notNull = JBCheckBox("NOT NULL")
    private val pk = JBCheckBox("Primary Key")
    private val unique = JBCheckBox("Unique")
    private val defaultField = JBTextField()
    private val commentField = JBTextField()
    private var changeCallback: (() -> Unit)? = null

    init {
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("Name", nameField)
            .addLabeledComponent("Type", typeBox)
            .addComponent(notNull)
            .addComponent(pk)
            .addComponent(unique)
            .addLabeledComponent("Default", defaultField)
            .addLabeledComponent("Comment", commentField)
            .panel

        add(form, BorderLayout.NORTH)

        nameField.document.addDocumentListener(SimpleDocListener {
            changeCallback?.invoke()
        })

        typeBox.addActionListener {
            changeCallback?.invoke()
        }

        notNull.addActionListener { changeCallback?.invoke() }
        pk.addActionListener { changeCallback?.invoke() }
        unique.addActionListener { changeCallback?.invoke() }
        defaultField.document.addDocumentListener(SimpleDocListener {
            changeCallback?.invoke()
        })
    }

    fun onChange(callback: () -> Unit) {
        changeCallback = callback
    }

    fun bind(column: ColumnDefinition?) {
        if (column == null) return

        nameField.text = column.name
        typeBox.selectedItem = column.type
        notNull.isSelected = !column.nullable
        pk.isSelected = column.primaryKey
        unique.isSelected = column.unique
        defaultField.text = column.defaultValue ?: ""
        commentField.text = column.comment ?: ""
    }

    fun applyTo(column: ColumnDefinition) {
        column.name = nameField.text
        column.type = typeBox.selectedItem as ColumnType
        column.nullable = !notNull.isSelected
        column.primaryKey = pk.isSelected
        column.unique = unique.isSelected
        column.defaultValue = defaultField.text.ifBlank { null }
        column.comment = commentField.text.ifBlank { null }
    }
}