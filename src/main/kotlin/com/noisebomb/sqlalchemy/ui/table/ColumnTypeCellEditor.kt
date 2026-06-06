package com.noisebomb.sqlalchemy.ui.table

import com.noisebomb.sqlalchemy.model.ColumnType
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox

class ColumnTypeCellEditor : DefaultCellEditor(
    JComboBox(ColumnType.entries.toTypedArray())
)