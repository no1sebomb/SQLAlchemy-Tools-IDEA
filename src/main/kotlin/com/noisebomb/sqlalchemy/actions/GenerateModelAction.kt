package com.noisebomb.sqlalchemy.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class GenerateModelAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val modelName = Messages.showInputDialog(
            project,
            "Model name (snake_case):",
            "Generate SQLAlchemy Model",
            Messages.getQuestionIcon()
        ) ?: return

        Messages.showInfoMessage(
            project,
            "Would generate model: $modelName\n\n(Next step: file creation logic)",
            "SQLAlchemy Generator"
        )
    }
}
