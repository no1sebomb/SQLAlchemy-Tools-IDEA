package com.noisebomb.sqlalchemy.ui.table

import javax.swing.JCheckBox
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

class BooleanCellRenderer : DefaultTableCellRenderer() {

    private val checkBox = JCheckBox()

    override fun getTableCellRendererComponent(
        table: javax.swing.JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {

        checkBox.isSelected = value as? Boolean ?: false
        checkBox.horizontalAlignment = JCheckBox.CENTER

        checkBox.isEnabled = false
        checkBox.isOpaque = true

        if (isSelected) {
            checkBox.background = table.selectionBackground
        } else {
            checkBox.background = table.background
        }

        return checkBox
    }
}
