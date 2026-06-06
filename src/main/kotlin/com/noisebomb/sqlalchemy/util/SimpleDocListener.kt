package com.noisebomb.sqlalchemy.util

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SimpleDocListener(
    private val onChange: () -> Unit
) : DocumentListener {

    override fun insertUpdate(e: DocumentEvent) = onChange()

    override fun removeUpdate(e: DocumentEvent) = onChange()

    override fun changedUpdate(e: DocumentEvent) = onChange()
}
