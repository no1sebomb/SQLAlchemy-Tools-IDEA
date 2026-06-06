package com.noisebomb.sqlalchemy.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiDirectory
import com.intellij.ui.AnActionButtonUpdater
import com.intellij.ui.EditorTextField
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.noisebomb.sqlalchemy.generation.SqlAlchemyCodeGenerator
import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnSpec
import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnType
import com.noisebomb.sqlalchemy.model.SqlAlchemyGenerationMode
import com.noisebomb.sqlalchemy.model.SqlAlchemyModelSpec
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// ---------------------------------------------------------------------------
// List item types
// ---------------------------------------------------------------------------
private sealed class ModelListItem
private data class GroupHeader(val label: String, val active: Boolean) : ModelListItem()
private data class ColumnEntry(val spec: SqlAlchemyColumnSpec) : ModelListItem()

// ---------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------
class GenerateModelDialog(project: Project, private val targetDirectory: PsiDirectory? = null) : DialogWrapper(project) {

    private val pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py")

    private val manualRadio = JBRadioButton("Manual", true)
    private val dataSourceRadio = JBRadioButton("From Data Source (coming soon)")
    private val sqlRadio = JBRadioButton("From SQL (coming soon)")

    private val modelNameField = JTextField()
    private val tableNameField = JTextField()
    private val fileNameField = JTextField()
    private val modelCommentField = JTextField()

    private val listModel = DefaultListModel<ModelListItem>()
    private val itemList = JBList(listModel)

    private val columnNameField = JTextField()
    private val columnTypeCombo: JComboBox<SqlAlchemyColumnType> = ComboBox(SqlAlchemyColumnType.entries.toTypedArray())
    private val primaryKeyCheckbox = JBCheckBox("Primary key")
    private val nullableCheckbox = JBCheckBox("Nullable")
    private val uniqueCheckbox = JBCheckBox("Unique")
    private val defaultExpressionField = EditorTextField("", project, pythonFileType)
    private val commentField = JTextField()

    // Preview — real Editor so its component is a native, scrollable JScrollPane
    private val previewEditorFactory = EditorFactory.getInstance()
    private val previewDocument = previewEditorFactory.createDocument("")
    private val previewEditorImpl: Editor = previewEditorFactory.createViewer(previewDocument, project).also { editor ->
        editor.settings.apply {
            isLineNumbersShown = false
            isFoldingOutlineShown = false
            isRightMarginShown = false
            isVirtualSpace = false
        }
        (editor as? EditorEx)?.highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, pythonFileType)
    }

    private val previewCardLayout = CardLayout()
    private val previewCardPanel = JPanel(previewCardLayout)
    private val CARD_PLACEHOLDER = "placeholder"
    private val CARD_EDITOR = "editor"

    private var previewExpanded = true
    private var previewArrowLabel: JLabel? = null
    private var previewContentPanel: JPanel? = null

    private var syncingNames = false
    private var modelNameUserEdited = false
    private var tableNameUserEdited = false
    private var fileNameUserEdited = false
    private var loadingColumnDetails = false

    init {
        title = "Create SQLAlchemy Model"
        setOKButtonText("Create")
        initListModel()
        initRadioButtons()
        initNamesSync()
        initColumnsUiState()
        updatePreview()
        init()
        startTrackingValidation()
    }

    override fun dispose() {
        previewEditorFactory.releaseEditor(previewEditorImpl)
        super.dispose()
    }

    fun getModelSpec(): SqlAlchemyModelSpec = SqlAlchemyModelSpec(
        mode = selectedMode(),
        modelName = modelNameField.text.trim(),
        tableName = tableNameField.text.trim(),
        fileName = normalizedFileName(fileNameField.text),
        modelComment = modelCommentField.text.trim(),
        columns = columnEntries()
    )

    override fun createCenterPanel(): JComponent {
        val root = JBPanel<JBPanel<*>>(BorderLayout(0, JBUIScale.scale(10)))
        root.border = JBUI.Borders.empty(8)
        root.add(buildOptionsPanel(), BorderLayout.NORTH)
        val center = JBPanel<JBPanel<*>>(BorderLayout(0, JBUIScale.scale(8)))
        center.add(buildColumnsPanel(), BorderLayout.CENTER)
        center.add(buildPreviewPanel(), BorderLayout.SOUTH)
        root.add(center, BorderLayout.CENTER)
        return root
    }

    override fun doValidate(): ValidationInfo? {
        val modelName = modelNameField.text.trim()
        val tableName = tableNameField.text.trim()
        if (modelName.isEmpty()) return ValidationInfo("Model name is required", modelNameField)
        if (!modelName.matches(Regex("[A-Za-z][A-Za-z0-9_]*")))
            return ValidationInfo("Model name should look like a Python class name", modelNameField)
        if (tableName.isEmpty()) return ValidationInfo("Table name is required", tableNameField)
        if (!tableName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")))
            return ValidationInfo("Table name should contain only letters, digits and underscore", tableNameField)
        val fileName = normalizedFileName(fileNameField.text)
        if (fileName == ".py") return ValidationInfo("Filename is required", fileNameField)
        val baseName = fileName.removeSuffix(".py")
        if (!baseName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")))
            return ValidationInfo("Filename should contain only letters, digits and underscore", fileNameField)
        if (targetDirectory?.findFile(fileName) != null)
            return ValidationInfo("File '$fileName' already exists in the selected directory", fileNameField)
        val cols = columnEntries()
        if (cols.isEmpty()) return ValidationInfo("At least one column is required", itemList)
        val emptyName = cols.firstOrNull { it.name.isBlank() }
        if (emptyName != null) return ValidationInfo("All columns must have a name", columnNameField)
        val dup = cols.groupBy { it.name.trim() }.entries.firstOrNull { it.key.isNotEmpty() && it.value.size > 1 }
        if (dup != null) return ValidationInfo("Duplicate column name: ${dup.key}", itemList)
        return null
    }

    // -----------------------------------------------------------------------
    // Panel builders
    // -----------------------------------------------------------------------

    private fun buildOptionsPanel(): JComponent {
        val radios = panel {
            buttonsGroup {
                row { cell(manualRadio) }
                row { cell(dataSourceRadio) }
                row { cell(sqlRadio) }
            }
        }
        val names = FormBuilder.createFormBuilder()
            .setVerticalGap(6)
            .addLabeledComponent("Table name:", tableNameField)
            .addLabeledComponent("Model name:", modelNameField)
            .addLabeledComponent("Filename:", fileNameField)
            .addLabeledComponent("Comment / docs:", modelCommentField)
            .panel
        return JBPanel<JBPanel<*>>(BorderLayout(0, JBUIScale.scale(12))).apply {
            add(radios, BorderLayout.NORTH)
            add(names, BorderLayout.SOUTH)
            border = JBUI.Borders.emptyBottom(8)
        }
    }

    private fun buildColumnsPanel(): JComponent {
        itemList.selectionModel = object : DefaultListSelectionModel() {
            init { selectionMode = ListSelectionModel.SINGLE_SELECTION }
            override fun setSelectionInterval(i0: Int, i1: Int) {
                if (i0 in 0 until listModel.size() && listModel.getElementAt(i0) is ColumnEntry)
                    super.setSelectionInterval(i0, i1)
            }
        }
        itemList.cellRenderer = ModelListCellRenderer()

        val decorator = ToolbarDecorator.createDecorator(itemList)
            .setAddAction {
                val newCol = SqlAlchemyColumnSpec(name = "", type = SqlAlchemyColumnType.STRING, nullable = true)
                val insertAt = columnsEndIndex()
                listModel.insertElementAt(ColumnEntry(newCol), insertAt)
                itemList.selectedIndex = insertAt
                updatePreview()
                SwingUtilities.invokeLater { columnNameField.requestFocusInWindow() }
            }
            .setRemoveAction {
                val idx = itemList.selectedIndex
                if (idx >= 0 && listModel.getElementAt(idx) is ColumnEntry) {
                    listModel.remove(idx)
                    val newSel = findNearestColumn(idx)
                    if (newSel >= 0) itemList.selectedIndex = newSel
                    updatePreview()
                }
            }
            .setMoveUpAction { moveColumn(-1) }
            .setMoveDownAction { moveColumn(1) }
            .setMoveUpActionUpdater(AnActionButtonUpdater {
                val idx = itemList.selectedIndex
                idx > 0 && listModel.getElementAt(idx) is ColumnEntry
                        && listModel.getElementAt(idx - 1) is ColumnEntry
            })
            .setMoveDownActionUpdater(AnActionButtonUpdater {
                val idx = itemList.selectedIndex
                idx >= 0 && idx < listModel.size() - 1
                        && listModel.getElementAt(idx) is ColumnEntry
                        && listModel.getElementAt(idx + 1) is ColumnEntry
            })
            .addExtraAction(object : AnAction("Duplicate", "Duplicate selected column", AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) = duplicateSelectedColumn()
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })

        val listWrapper = JPanel(BorderLayout()).apply {
            border = IdeBorderFactory.createTitledBorder("Attributes", false)
            add(decorator.createPanel(), BorderLayout.CENTER)
            preferredSize = Dimension(JBUIScale.scale(320), JBUIScale.scale(280))
        }
        defaultExpressionField.setOneLineMode(true)
        val details = FormBuilder.createFormBuilder()
            .setVerticalGap(6)
            .addLabeledComponent("Column name:", columnNameField)
            .addLabeledComponent("Column type:", columnTypeCombo)
            .addComponent(primaryKeyCheckbox)
            .addComponent(nullableCheckbox)
            .addComponent(uniqueCheckbox)
            .addLabeledComponent("Default expression:", defaultExpressionField)
            .addLabeledComponent("Comment:", commentField)
            .panel
            .also { it.border = IdeBorderFactory.createTitledBorder("Options", false) }

        val detailsWrapper = JPanel(BorderLayout()).apply {
            add(details, BorderLayout.NORTH)
            isOpaque = false
        }

        return JPanel(BorderLayout(JBUIScale.scale(8), 0)).apply {
            add(listWrapper, BorderLayout.WEST)
            add(detailsWrapper, BorderLayout.CENTER)
        }
    }

    private fun buildPreviewPanel(): JComponent {
        // Placeholder shown when content is not ready yet
        val placeholderPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            preferredSize = Dimension(0, JBUIScale.scale(200))
            add(JLabel("Fill in required values", SwingConstants.CENTER).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                font = font.deriveFont(Font.ITALIC)
            }, BorderLayout.CENTER)
        }

        // Real editor component — already a scroll pane internally
        val editorComponent = previewEditorImpl.component.also {
            it.preferredSize = Dimension(0, JBUIScale.scale(200))
        }

        previewCardPanel.add(placeholderPanel, CARD_PLACEHOLDER)
        previewCardPanel.add(editorComponent, CARD_EDITOR)

        val arrowLabel = JLabel(UIUtil.getTreeExpandedIcon()).also { previewArrowLabel = it }
        val titleLabel = JLabel("Preview").apply {
            font = font.deriveFont(Font.BOLD)
            border = JBUI.Borders.emptyLeft(4)
        }
        val header = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(arrowLabel)
            add(titleLabel)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(4, 0)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = togglePreview()
            })
        }
        val content = JPanel(BorderLayout()).apply {
            add(previewCardPanel, BorderLayout.CENTER)
            isVisible = previewExpanded
        }.also { previewContentPanel = it }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            add(header, BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }
    }

    // -----------------------------------------------------------------------
    // Init helpers
    // -----------------------------------------------------------------------

    private fun initListModel() {
        listModel.addElement(GroupHeader("Columns", true))
        listModel.addElement(ColumnEntry(SqlAlchemyColumnSpec("id", SqlAlchemyColumnType.INTEGER, primaryKey = true, nullable = false)))
        listModel.addElement(GroupHeader("Indexes", false))
        listModel.addElement(GroupHeader("Relationships", false))
        itemList.selectedIndex = 1
    }

    private fun initRadioButtons() {
        dataSourceRadio.isEnabled = false
        sqlRadio.isEnabled = false
    }

    private fun initNamesSync() {
        fileNameField.text = normalizedFileName(toSnakeCase(modelNameField.text))

        modelNameField.document.addDocumentListener(simpleListener {
            if (syncingNames) return@simpleListener
            val text = modelNameField.text
            modelNameUserEdited = text.isNotEmpty()
            if (!tableNameUserEdited) {
                setTextAfterNotification(tableNameField, toSnakeCase(text))
            }
            if (!fileNameUserEdited) {
                setTextAfterNotification(fileNameField, normalizedFileName(toSnakeCase(text)))
            }
            updatePreview()
        })
        tableNameField.document.addDocumentListener(simpleListener {
            if (syncingNames) return@simpleListener
            val text = tableNameField.text
            tableNameUserEdited = text.isNotEmpty()
            if (!modelNameUserEdited) {
                setTextAfterNotification(modelNameField, toCamelCase(text))
            }
            updatePreview()
        })
        fileNameField.document.addDocumentListener(simpleListener {
            if (syncingNames) return@simpleListener
            val normalized = normalizedFileName(fileNameField.text)
            fileNameUserEdited = normalized != normalizedFileName(toSnakeCase(modelNameField.text))
            if (fileNameField.text != normalized) {
                setTextAfterNotification(fileNameField, normalized)
                return@simpleListener
            }
            updatePreview()
        })
        modelCommentField.document.addDocumentListener(simpleListener { updatePreview() })
    }

    private fun initColumnsUiState() {
        applyMonospaceInputs()
        modelCommentField.foreground = UIUtil.getLabelDisabledForeground()
        commentField.foreground = UIUtil.getLabelDisabledForeground()
        itemList.addListSelectionListener { loadSelectedColumn() }
        columnNameField.document.addDocumentListener(simpleListener {
            updateSelectedColumn { name = columnNameField.text.trim() }
        })
        columnTypeCombo.addActionListener {
            updateSelectedColumn { type = columnTypeCombo.selectedItem as SqlAlchemyColumnType }
        }
        primaryKeyCheckbox.addActionListener {
            updateSelectedColumn {
                primaryKey = primaryKeyCheckbox.isSelected
                if (primaryKey) { nullable = false; nullableCheckbox.isSelected = false }
            }
            nullableCheckbox.isEnabled = !primaryKeyCheckbox.isSelected
        }
        nullableCheckbox.addActionListener {
            updateSelectedColumn { nullable = nullableCheckbox.isSelected }
        }
        uniqueCheckbox.addActionListener {
            updateSelectedColumn { unique = uniqueCheckbox.isSelected }
        }
        defaultExpressionField.document.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                updateSelectedColumn { defaultExpression = defaultExpressionField.text }
            }
        })
        commentField.document.addDocumentListener(simpleListener {
            updateSelectedColumn { comment = commentField.text }
        })
        loadSelectedColumn()
    }

    // -----------------------------------------------------------------------
    // Column helpers
    // -----------------------------------------------------------------------

    private fun loadSelectedColumn() {
        val entry = selectedColumnEntry()
        loadingColumnDetails = true
        listOf(columnNameField, columnTypeCombo, primaryKeyCheckbox, nullableCheckbox, uniqueCheckbox, defaultExpressionField, commentField)
            .forEach { it.isEnabled = entry != null }
        if (entry == null) {
            columnNameField.text = ""
            defaultExpressionField.text = ""
            commentField.text = ""
            uniqueCheckbox.isSelected = false
            loadingColumnDetails = false
            return
        }
        val col = entry.spec
        columnNameField.text = col.name
        columnTypeCombo.selectedItem = col.type
        primaryKeyCheckbox.isSelected = col.primaryKey
        nullableCheckbox.isSelected = col.nullable
        uniqueCheckbox.isSelected = col.unique
        nullableCheckbox.isEnabled = !col.primaryKey
        defaultExpressionField.text = col.defaultExpression
        commentField.text = col.comment
        loadingColumnDetails = false
    }

    private fun updateSelectedColumn(update: SqlAlchemyColumnSpec.() -> Unit) {
        if (loadingColumnDetails) return
        val entry = selectedColumnEntry() ?: return
        entry.spec.update()
        itemList.repaint()
        updatePreview()
    }

    private fun selectedColumnEntry(): ColumnEntry? {
        val idx = itemList.selectedIndex
        return if (idx >= 0) listModel.getElementAt(idx) as? ColumnEntry else null
    }

    private fun columnsEndIndex(): Int {
        for (i in 0 until listModel.size()) {
            val item = listModel.getElementAt(i)
            if (item is GroupHeader && item.label != "Columns") return i
        }
        return listModel.size()
    }

    private fun findNearestColumn(removedIdx: Int): Int {
        for (i in removedIdx - 1 downTo 0) if (listModel.getElementAt(i) is ColumnEntry) return i
        for (i in removedIdx until listModel.size()) if (listModel.getElementAt(i) is ColumnEntry) return i
        return -1
    }

    private fun moveColumn(delta: Int) {
        val from = itemList.selectedIndex
        if (from < 0) return
        val to = from + delta
        if (to < 0 || to >= listModel.size()) return
        if (listModel.getElementAt(from) !is ColumnEntry || listModel.getElementAt(to) !is ColumnEntry) return
        val item = listModel.getElementAt(from)
        listModel.remove(from)
        listModel.add(to, item)
        itemList.selectedIndex = to
        updatePreview()
    }

    private fun duplicateSelectedColumn() {
        val selIdx = itemList.selectedIndex
        val entry = selectedColumnEntry() ?: return
        val src = entry.spec
        val copy = SqlAlchemyColumnSpec(
            name = nextColumnName(src.name), type = src.type,
            primaryKey = src.primaryKey, nullable = src.nullable, unique = src.unique,
            defaultExpression = src.defaultExpression, comment = src.comment
        )
        val insertAt = (selIdx + 1).coerceAtMost(columnsEndIndex())
        listModel.insertElementAt(ColumnEntry(copy), insertAt)
        itemList.selectedIndex = insertAt
        updatePreview()
    }

    private fun nextColumnName(base: String = "column"): String {
        val existing = columnEntries().map { it.name }.toSet()
        if (base !in existing) return base
        var i = 2
        while ("${base}_$i" in existing) i++
        return "${base}_$i"
    }

    private fun columnEntries(): List<SqlAlchemyColumnSpec> =
        (0 until listModel.size()).mapNotNull { listModel.getElementAt(it) as? ColumnEntry }.map { it.spec }

    // -----------------------------------------------------------------------
    // Preview
    // -----------------------------------------------------------------------

    private fun togglePreview() {
        previewExpanded = !previewExpanded
        previewArrowLabel?.icon = if (previewExpanded) UIUtil.getTreeExpandedIcon() else UIUtil.getTreeCollapsedIcon()
        previewContentPanel?.let {
            it.isVisible = previewExpanded
            it.parent?.revalidate()
            it.parent?.repaint()
        }
    }

    private fun updatePreview() {
        val modelName = modelNameField.text.trim()
        val tableName = tableNameField.text.trim()
        val cols = columnEntries()
        val isReady = modelName.isNotEmpty() && tableName.isNotEmpty() && cols.isNotEmpty()

        previewCardLayout.show(previewCardPanel, if (isReady) CARD_EDITOR else CARD_PLACEHOLDER)

        if (isReady) {
            val code = SqlAlchemyCodeGenerator.generate(
                SqlAlchemyModelSpec(
                    mode = selectedMode(),
                    modelName = modelName,
                    tableName = tableName,
                    fileName = normalizedFileName(fileNameField.text),
                    modelComment = modelCommentField.text.trim(),
                    columns = cols
                )
            )
            ApplicationManager.getApplication().runWriteAction {
                previewDocument.setText(code)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Misc
    // -----------------------------------------------------------------------

    private fun selectedMode() = when {
        dataSourceRadio.isSelected -> SqlAlchemyGenerationMode.DATA_SOURCE
        sqlRadio.isSelected -> SqlAlchemyGenerationMode.SQL
        else -> SqlAlchemyGenerationMode.MANUAL
    }

    private fun toSnakeCase(value: String) = value
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("\\s+"), "_")
        .lowercase().trim('_')

    private fun toCamelCase(value: String) = value
        .split('_', '-', ' ').filter { it.isNotBlank() }
        .joinToString("") { it.substring(0, 1).uppercase() + it.substring(1).lowercase() }

    private fun normalizedFileName(value: String): String {
        var normalized = value.trim().removeSuffix(".py")
        normalized = toSnakeCase(normalized)
        return if (normalized.isBlank()) ".py" else "$normalized.py"
    }

    private fun simpleListener(callback: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = callback()
        override fun removeUpdate(e: DocumentEvent?) = callback()
        override fun changedUpdate(e: DocumentEvent?) = callback()
    }

    private fun setTextAfterNotification(field: JTextField, value: String) {
        if (field.text == value) return
        SwingUtilities.invokeLater {
            if (!field.isDisplayable) return@invokeLater
            if (field.text == value) return@invokeLater
            syncingNames = true
            field.text = value
            syncingNames = false
            updatePreview()
        }
    }

    private fun applyEditorFont(field: EditorTextField) {
        val scheme = EditorColorsManager.getInstance().globalScheme
        field.font = Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
    }

    private fun applyMonospaceInputs() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val mono = Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
        listOf(modelNameField, tableNameField, fileNameField, modelCommentField, columnNameField, commentField).forEach {
            it.font = mono
        }
        applyEditorFont(defaultExpressionField)
        // Preview editor uses the global color scheme font automatically
    }
}

// ---------------------------------------------------------------------------
// Cell renderer
// ---------------------------------------------------------------------------
private class ModelListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: javax.swing.JList<*>, value: Any?, index: Int,
        isSelected: Boolean, cellHasFocus: Boolean
    ): Component = when (value) {
        is GroupHeader -> {
            super.getListCellRendererComponent(list, value, index, false, false)
            text = value.label
            font = font.deriveFont(Font.BOLD)
            icon = AllIcons.Nodes.Folder
            if (!value.active) foreground = UIUtil.getLabelDisabledForeground()
            border = JBUI.Borders.empty(3, 4)
            this
        }
        is ColumnEntry -> {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            val col = value.spec
            val displayName = if (col.name.isEmpty()) "(unnamed)" else col.name
            text = "  $displayName: ${col.type.displayName}${if (col.primaryKey) " [PK]" else ""}"
            icon = AllIcons.Nodes.Field
            border = JBUI.Borders.empty(2, 4)
            this
        }
        else -> super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    }
}
