package com.noisebomb.sqlalchemy.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.noisebomb.sqlalchemy.model.ModelDefinition
import com.noisebomb.sqlalchemy.ui.panels.SchemaEditorPanel
import javax.swing.JComponent

class GenerateModelDialog(
    project: Project
) : DialogWrapper(project) {

    private val editorPanel = SchemaEditorPanel()

    init {
        title = "Generate SQLAlchemy Model"

        setSize(900, 600)   // important for DataGrip-like UI
        isResizable = true

        init()
    }

    override fun createCenterPanel(): JComponent {
        return editorPanel
    }

    override fun doOKAction() {
        val model = buildModel()
        println(model) // later: pass to renderer/file generator
        super.doOKAction()
    }

    private fun buildModel(): ModelDefinition {
        return editorPanel.buildModelDefinition()
    }
}
