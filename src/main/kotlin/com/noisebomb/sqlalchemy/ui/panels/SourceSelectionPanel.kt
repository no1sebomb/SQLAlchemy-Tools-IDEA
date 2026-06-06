package com.noisebomb.sqlalchemy.ui.panels

import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import com.noisebomb.sqlalchemy.model.ModelDefinition
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*

class SourceSelectionPanel : JPanel(BorderLayout()) {

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val manualPanel = ManualModelPanel()

    private val manualRadio = JBRadioButton("Manual")
    private val dsRadio = JBRadioButton("From Data Source (coming soon)")
    private val sqlRadio = JBRadioButton("From SQL (coming soon)")

    init {
        val group = ButtonGroup()
        group.add(manualRadio)
        group.add(sqlRadio)
        group.add(dsRadio)

        manualRadio.isSelected = true
        sqlRadio.isEnabled = false
        dsRadio.isEnabled = false
        manualRadio.addActionListener { cardLayout.show(cardPanel, "MANUAL") }
        sqlRadio.addActionListener { cardLayout.show(cardPanel, "SQL") }
        dsRadio.addActionListener { cardLayout.show(cardPanel, "DS") }

        // Cards
        cardPanel.add(manualPanel, "MANUAL")

        // Radio panel
        val selector = FormBuilder.createFormBuilder()
            .addComponent(manualRadio)
            .addComponent(sqlRadio)
            .addComponent(dsRadio)
            .panel

        add(selector, BorderLayout.NORTH)
        add(cardPanel, BorderLayout.CENTER)

        // switching
        manualRadio.addActionListener { cardLayout.show(cardPanel, "MANUAL") }
    }

    fun buildModelDefinition(): ModelDefinition? {
        return manualPanel.buildModelDefinition()
    }
}
