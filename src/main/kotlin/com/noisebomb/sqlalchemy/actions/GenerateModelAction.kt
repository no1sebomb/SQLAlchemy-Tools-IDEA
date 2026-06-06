package com.noisebomb.sqlalchemy.actions

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.noisebomb.sqlalchemy.generation.SqlAlchemyCodeGenerator
import com.noisebomb.sqlalchemy.ui.GenerateModelDialog

class GenerateModelAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && resolveTargetDirectory(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val targetDirectory = resolveTargetDirectory(e) ?: run {
            Messages.showWarningDialog(project, "Select a target directory first.", "SQLAlchemy Model")
            return
        }

        val dialog = GenerateModelDialog(project)
        if (!dialog.showAndGet()) {
            return
        }

        val spec = dialog.getModelSpec()
        val fileName = "${spec.tableName}.py"
        if (targetDirectory.findFile(fileName) != null) {
            Messages.showErrorDialog(project, "File '$fileName' already exists in the selected directory.", "SQLAlchemy Model")
            return
        }

        val code = SqlAlchemyCodeGenerator.generate(spec)
        WriteCommandAction.writeCommandAction(project).withName("Create SQLAlchemy Model").run<RuntimeException> {
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension("py")
            val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, fileType, code)
            val createdFile = targetDirectory.add(psiFile).containingFile
            createdFile?.virtualFile?.let { virtualFile ->
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }
    }

    private fun resolveTargetDirectory(e: AnActionEvent): PsiDirectory? {
        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        if (ideView is IdeView) {
            ideView.directories.firstOrNull()?.let { return it }
        }

        return CommonDataKeys.PSI_FILE.getData(e.dataContext)?.containingDirectory
    }
}


