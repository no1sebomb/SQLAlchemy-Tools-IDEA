package com.noisebomb.sqlalchemy.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.noisebomb.sqlalchemy.generator.SqlAlchemyRenderer
import com.noisebomb.sqlalchemy.ui.panels.SourceSelectionPanel
import javax.swing.JComponent

class GenerateModelDialog(
    private val project: Project
) : DialogWrapper(project) {

    private val panel = SourceSelectionPanel()

    init {
        title = "Generate SQLAlchemy Model"
        setSize(600, 400)
        isResizable = false

        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel
    }

    override fun doOKAction() {
        val model = panel.buildModelDefinition()

        if (model != null) {
            val code = SqlAlchemyRenderer.render(model)
            println(code)
        }
        super.doOKAction()
    }
}
