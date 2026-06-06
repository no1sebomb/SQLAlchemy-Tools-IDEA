package com.noisebomb.sqlalchemy.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.noisebomb.sqlalchemy.ui.dialogs.GenerateModelDialog

class GenerateModelAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        GenerateModelDialog(project).show()
    }
}
