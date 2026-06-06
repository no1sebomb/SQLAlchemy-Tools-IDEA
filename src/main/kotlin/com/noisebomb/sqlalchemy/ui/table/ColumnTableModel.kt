package com.noisebomb.sqlalchemy.ui.table

import com.noisebomb.sqlalchemy.model.ColumnDefinition
import com.noisebomb.sqlalchemy.model.ColumnType
import javax.swing.table.AbstractTableModel

class ColumnTableModel(
    val columns: MutableList<ColumnDefinition>
) : AbstractTableModel() {

    private val columnsMeta = listOf("Name", "Type", "PK", "Nullable", "Unique")

    override fun getRowCount() = columns.size
    override fun getColumnCount() = columnsMeta.size
    override fun getColumnName(col: Int) = columnsMeta[col]

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            1 -> ColumnType::class.java
            2 -> Boolean::class.java  // PK
            3 -> Boolean::class.java  // Nullable
            4 -> Boolean::class.java  // Unique
            else -> String::class.java
        }
    }

    override fun getValueAt(row: Int, col: Int): Any {
        val c = columns[row]
        return when (col) {
            0 -> c.name
            1 -> c.type
            2 -> c.primaryKey
            3 -> c.nullable
            4 -> c.unique
            else -> ""
        }
    }

    override fun isCellEditable(row: Int, col: Int) = true

    override fun setValueAt(value: Any?, row: Int, col: Int) {
        val c = columns[row]

        when (col) {
            0 -> c.name = value.toString()
            1 -> c.type = value as ColumnType
            2 -> c.primaryKey = value as Boolean
            3 -> c.nullable = value as Boolean
            4 -> c.unique = value as Boolean
        }

        fireTableCellUpdated(row, col)
    }
}
