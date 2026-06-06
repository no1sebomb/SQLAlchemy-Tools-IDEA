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
import com.intellij.ui.JBColor
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.openapi.util.Disposer
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
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Graphics
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
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

    // Type section
    private val manualRadio = JBRadioButton("Manual", true)
    private val dataSourceRadio = JBRadioButton("From Data Source (coming soon)")
    private val sqlRadio = JBRadioButton("From SQL (coming soon)")

    // Table section
    private val modelNameField = JTextField()
    private val tableNameDiffersCheckBox = JBCheckBox("Different table name")
    private val tableNameLabel = JLabel("Table:")
    private val tableNameField = JTextField()
    private val modelCommentField = JBTextArea(3, 20)

    // File section
    private val fileNameField = JTextField()
    private val attributeTypesMappingCheckBox = JBCheckBox("Attribute types mapping", true)
    private val useLegacyColumnsCheckBox = JBCheckBox("Use legacy columns", false)

    // Attributes / options
    private val listModel = DefaultListModel<ModelListItem>()
    private val itemList = JBList(listModel)

    private val columnNameField = JTextField()
    private val columnTypeCombo = ComboBox(SqlAlchemyColumnType.entries.toTypedArray())
    private val primaryKeyCheckbox = JBCheckBox("Primary key")
    private val nullableCheckbox = JBCheckBox("Nullable")
    private val uniqueCheckbox = JBCheckBox("Unique")
    private val defaultExpressionField = EditorTextField("", project, pythonFileType)
    private val commentField = JTextField()

     // Preview
     private val previewFactory = EditorFactory.getInstance()
     private val previewDocument = previewFactory.createDocument("")
     private val previewEditor: Editor = previewFactory.createViewer(previewDocument, project).also { editor ->
         editor.settings.apply {
             isLineNumbersShown = true
             isFoldingOutlineShown = false
             isRightMarginShown = false
             isVirtualSpace = false
         }
         (editor as? EditorEx)?.highlighter = EditorHighlighterFactory.getInstance()
             .createEditorHighlighter(project, pythonFileType)
     }
     private val previewCardLayout = CardLayout()
     private val previewCardPanel = JPanel(previewCardLayout)
     private val previewPlaceholderCard = "placeholder"
     private val previewEditorCard = "editor"
     private val listenersDisposable = Disposer.newDisposable()
     private var previewVisible = true
     private var previewArrowLabel: JLabel? = null
     private var previewContentPanel: JPanel? = null

     // Split panes (for deferred layout)
     private var attrsOptionsSplit: JSplitPane? = null
     private var verticalSplit: JSplitPane? = null

     // Sync state
     private var syncingFields = false
     private var fileNameUserEdited = false
     private var loadingColumnDetails = false

    init {
        title = "Create SQLAlchemy Model"
        setOKButtonText("Create")
        initListModel()
        initTypeSection()
        initTableSection()
        initTableDescription()
        initFileSection()
        initAttributesSection()
        applyMonospaceFontToInputs()
        updatePreview()
        init()
        startTrackingValidation()
    }

    override fun dispose() {
        Disposer.dispose(listenersDisposable)
        previewFactory.releaseEditor(previewEditor)
        super.dispose()
    }

    fun getModelSpec(): SqlAlchemyModelSpec = SqlAlchemyModelSpec(
        mode = selectedMode(),
        modelName = modelNameField.text.trim(),
        tableName = effectiveTableName(),
        fileName = normalizedFileName(fileNameField.text),
        modelComment = modelCommentField.text.trim(),
        attributeTypesMapping = attributeTypesMappingCheckBox.isSelected,
        useLegacyColumns = useLegacyColumnsCheckBox.isSelected,
        columns = columnEntries()
    )

    override fun createCenterPanel(): JComponent {
        val root = JPanel(BorderLayout(0, JBUIScale.scale(10)))
        root.border = JBUI.Borders.empty(8)
        root.add(buildTopSectionsPanel(), BorderLayout.NORTH)
        root.add(buildMainSplitPanel(), BorderLayout.CENTER)
        // Defer divider locations until after layout
        SwingUtilities.invokeLater {
            deferredSetDividerLocations()
        }
        return root
    }

    override fun getPreferredSize(): Dimension = Dimension(JBUIScale.scale(1600), JBUIScale.scale(800))

    override fun doValidate(): ValidationInfo? {
        val modelName = modelNameField.text.trim()
        if (modelName.isEmpty()) return ValidationInfo("Model name is required", modelNameField)
        if (!modelName.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
            return ValidationInfo("Model name should look like a Python class name", modelNameField)
        }

        if (tableNameDiffersCheckBox.isSelected) {
            val tableName = tableNameField.text.trim()
            if (tableName.isEmpty()) return ValidationInfo("Table name is required", tableNameField)
            if (!tableName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
                return ValidationInfo("Table name should contain only letters, digits and underscore", tableNameField)
            }
        }

        val fileName = normalizedFileName(fileNameField.text)
        if (fileName == ".py") return ValidationInfo("Filename is required", fileNameField)
        val baseName = fileName.removeSuffix(".py")
        if (!baseName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
            return ValidationInfo("Filename should contain only letters, digits and underscore", fileNameField)
        }
        if (targetDirectory?.findFile(fileName) != null) {
            return ValidationInfo("File '$fileName' already exists in the selected directory", fileNameField)
        }

        val cols = columnEntries()
        if (cols.isEmpty()) return ValidationInfo("At least one column is required", itemList)
        if (cols.any { it.name.isBlank() }) return ValidationInfo("All columns must have a name", columnNameField)
        val dup = cols.groupBy { it.name.trim() }.entries.firstOrNull { it.key.isNotEmpty() && it.value.size > 1 }
        if (dup != null) return ValidationInfo("Duplicate column name: ${dup.key}", itemList)
        return null
    }

    // -----------------------------------------------------------------------
    // Top sections
    // -----------------------------------------------------------------------

    private fun buildTopSectionsPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.isOpaque = false
        panel.add(buildFixedSection("Type", buildTypeSectionBody(), 130))
        panel.add(Box.createVerticalStrut(4))
        panel.add(buildFixedSection("Table", buildTableSectionBody(), 210))
        panel.add(Box.createVerticalStrut(4))
        panel.add(buildFixedSection("File", buildFileSectionBody(), 180))
        return panel
    }

    private fun buildFixedSection(title: String, body: JComponent, fixedHeight: Int): JComponent {
        val wrapper = JPanel(BorderLayout())
        wrapper.border = JBUI.Borders.empty()
        wrapper.add(buildSectionHeader(title), BorderLayout.NORTH)
        wrapper.add(body, BorderLayout.CENTER)
        wrapper.maximumSize = Dimension(Int.MAX_VALUE, JBUIScale.scale(fixedHeight))
        wrapper.minimumSize = Dimension(0, JBUIScale.scale(fixedHeight))
        wrapper.preferredSize = Dimension(0, JBUIScale.scale(fixedHeight))
        return wrapper
    }

    private fun buildTypeSectionBody(): JComponent {
        val radios = panel {
            buttonsGroup {
                row { cell(manualRadio) }
                row { cell(dataSourceRadio) }
                row { cell(sqlRadio) }
            }
        }
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8, 8, 8)
            add(radios, BorderLayout.NORTH)
        }
    }

    private fun buildTableSectionBody(): JComponent {
        modelCommentField.lineWrap = true
        modelCommentField.wrapStyleWord = true
        modelCommentField.rows = 2
        modelCommentField.border = JBUI.Borders.empty(4, 6)

        // Slightly widen nested label so the Table field lines up with Model field.
        val baseLabelSize = tableNameLabel.preferredSize
        tableNameLabel.preferredSize = Dimension(baseLabelSize.width + JBUIScale.scale(8), baseLabelSize.height)
        tableNameLabel.minimumSize = tableNameLabel.preferredSize
        tableNameLabel.border = JBUI.Borders.emptyRight(6)

        val descriptionScroll = JBScrollPane(modelCommentField).apply {
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(0, JBUIScale.scale(48))
        }

        // Large-input style: label on top, field below
        val descriptionPanel = JPanel(BorderLayout(0, JBUIScale.scale(4))).apply {
            isOpaque = false
            add(JLabel("Description:").apply {
                foreground = UIUtil.getLabelForeground()
            }, BorderLayout.NORTH)
            add(descriptionScroll, BorderLayout.CENTER)
        }

        // Use IntelliJ UI DSL so indent{} aligns the sub-row exactly with checkbox text
        return panel {
            row("Model name:") {
                cell(modelNameField).resizableColumn().align(AlignX.FILL)
            }
            row {
                cell(tableNameDiffersCheckBox)
            }
            indent {
                row {
                    cell(tableNameLabel)
                    cell(tableNameField).resizableColumn().align(AlignX.FILL)
                }
            }
            row {
                cell(descriptionPanel).resizableColumn().align(AlignX.FILL)
            }
        }.apply {
            border = JBUI.Borders.empty(4, 8, 8, 8)
        }
    }

    private fun buildFileSectionBody(): JComponent {
        val form = FormBuilder.createFormBuilder()
            .setVerticalGap(6)
            .addLabeledComponent("Filename:", fileNameField)
            .addComponent(attributeTypesMappingCheckBox)
            .addComponent(useLegacyColumnsCheckBox)
            .panel

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8, 8, 8)
            add(form, BorderLayout.NORTH)
        }
    }

    // -----------------------------------------------------------------------
    // Main attributes/options + preview split
    // -----------------------------------------------------------------------

    private fun buildMainSplitPanel(): JComponent {
        val attributesPanel = buildAttributesPanel()
        val optionsPanel = buildOptionsPanel()
        attrsOptionsSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, attributesPanel, optionsPanel).apply {
            resizeWeight = 0.25
            dividerSize = 8
            border = JBUI.Borders.empty()
            // Don't set divider location here; defer to after layout
            installInvisibleDivider()
        }

        val previewPanel = buildPreviewPanel()
        verticalSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, true, attrsOptionsSplit, previewPanel).apply {
            resizeWeight = 0.0
            dividerSize = 8
            border = JBUI.Borders.empty()
            installInvisibleDivider()
        }

        return verticalSplit!!
    }

    private fun deferredSetDividerLocations() {
        attrsOptionsSplit?.setDividerLocation(0.25)
        verticalSplit?.setDividerLocation(0.55)
    }

    private fun buildAttributesPanel(): JComponent {
        itemList.selectionModel = object : DefaultListSelectionModel() {
            init { selectionMode = SINGLE_SELECTION }
            override fun setSelectionInterval(i0: Int, i1: Int) {
                if (i0 in 0 until listModel.size() && listModel.getElementAt(i0) is ColumnEntry) {
                    super.setSelectionInterval(i0, i1)
                }
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
                    if (newSel >= 0) itemList.selectedIndex = newSel else loadSelectedColumn()
                    updatePreview()
                }
            }
            .setMoveUpAction { moveColumn(-1) }
            .setMoveDownAction { moveColumn(1) }
            .setMoveUpActionUpdater {
                val idx = itemList.selectedIndex
                idx > 0 && idx < listModel.size() && listModel.getElementAt(idx) is ColumnEntry && listModel.getElementAt(idx - 1) is ColumnEntry
            }
            .setMoveDownActionUpdater {
                val idx = itemList.selectedIndex
                idx >= 0 && idx < listModel.size() - 1 && listModel.getElementAt(idx) is ColumnEntry && listModel.getElementAt(idx + 1) is ColumnEntry
            }
            .addExtraAction(object : AnAction("Duplicate", "Duplicate selected column", AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) = duplicateSelectedColumn()
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })

        val listPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4)
            add(decorator.createPanel(), BorderLayout.CENTER)
            preferredSize = Dimension(JBUIScale.scale(260), JBUIScale.scale(320))
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0)
            add(buildSectionHeader("Attributes"), BorderLayout.NORTH)
            add(listPanel, BorderLayout.CENTER)
        }
    }

    private fun buildOptionsPanel(): JComponent {
        defaultExpressionField.setOneLineMode(true)

        val form = FormBuilder.createFormBuilder()
            .setVerticalGap(6)
            .addLabeledComponent("Column name:", columnNameField)
            .addLabeledComponent("Column type:", columnTypeCombo)
            .addComponent(primaryKeyCheckbox)
            .addComponent(nullableCheckbox)
            .addComponent(uniqueCheckbox)
            .addLabeledComponent("Default expression:", defaultExpressionField)
            .addLabeledComponent("Comment:", commentField)
            .panel

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0)
            add(buildSectionHeader("Options"), BorderLayout.NORTH)
            add(form, BorderLayout.CENTER)
        }
    }

    private fun buildPreviewPanel(): JComponent {
        val placeholderPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(JLabel("Fill in required values", SwingConstants.CENTER).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                font = font.deriveFont(Font.ITALIC)
            }, BorderLayout.CENTER)
            preferredSize = Dimension(0, JBUIScale.scale(200))
        }

        val editorComponent = previewEditor.component.also {
            it.preferredSize = Dimension(0, JBUIScale.scale(200))
        }

        previewCardPanel.add(placeholderPanel, previewPlaceholderCard)
        previewCardPanel.add(editorComponent, previewEditorCard)

        val arrowLabel = JLabel(UIUtil.getTreeExpandedIcon()).also { previewArrowLabel = it }
        val header = buildSectionHeader("Preview", arrowLabel).apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = togglePreview()
            })
        }
        val content = JPanel(BorderLayout()).apply {
            add(previewCardPanel, BorderLayout.CENTER)
            isVisible = previewVisible
        }.also { previewContentPanel = it }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            add(header, BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }
    }

    private fun buildSectionHeader(title: String, leadingIcon: JComponent? = null): JComponent {
        val label = JLabel(title).apply {
            font = font.deriveFont(Font.PLAIN)
            foreground = UIUtil.getLabelForeground()
            alignmentY = Component.CENTER_ALIGNMENT
        }

        val separator = javax.swing.JSeparator(SwingConstants.HORIZONTAL).apply {
            foreground = UIUtil.getLabelDisabledForeground()
            alignmentY = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 2)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(0, 0, 4, 0)
            alignmentX = Component.LEFT_ALIGNMENT
            if (leadingIcon != null) {
                add(leadingIcon)
                add(Box.createHorizontalStrut(4))
            }
            add(label)
            add(Box.createHorizontalStrut(8))
            add(separator)
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

    private fun initTypeSection() {
        dataSourceRadio.isEnabled = false
        sqlRadio.isEnabled = false
    }

    private fun initTableSection() {
        tableNameDiffersCheckBox.isSelected = false
        tableNameField.text = toSnakeCase(modelNameField.text)
        updateTableNameFieldState()

        modelNameField.document.addDocumentListener(simpleListener {
            if (syncingFields) return@simpleListener
            val text = modelNameField.text
            if (!tableNameDiffersCheckBox.isSelected) {
                syncTextLater(tableNameField, toSnakeCase(text))
            }
            if (!fileNameUserEdited) {
                syncTextLater(fileNameField, normalizedFileName(toSnakeCase(text)))
            }
            updatePreview()
        })

        tableNameDiffersCheckBox.addActionListener {
            updateTableNameFieldState()
            if (!tableNameDiffersCheckBox.isSelected) {
                syncTextLater(tableNameField, toSnakeCase(modelNameField.text))
            }
            updatePreview()
        }

        tableNameField.document.addDocumentListener(simpleListener {
            if (syncingFields) return@simpleListener
            if (tableNameDiffersCheckBox.isSelected) updatePreview()
        })
    }

    private fun initFileSection() {
        fileNameField.text = normalizedFileName(toSnakeCase(modelNameField.text))
        attributeTypesMappingCheckBox.isSelected = true
        useLegacyColumnsCheckBox.isSelected = false

        fileNameField.document.addDocumentListener(simpleListener {
            if (syncingFields) return@simpleListener
            val normalized = normalizedFileName(fileNameField.text)
            fileNameUserEdited = normalized != normalizedFileName(toSnakeCase(modelNameField.text))
            if (fileNameField.text != normalized) {
                syncTextLater(fileNameField, normalized)
                return@simpleListener
            }
            updatePreview()
        })

        attributeTypesMappingCheckBox.addActionListener { updatePreview() }
        useLegacyColumnsCheckBox.addActionListener { updatePreview() }

        applyMonospaceFontToInputs()
    }

    private fun initTableDescription() {
        modelCommentField.foreground = UIUtil.getTextFieldForeground()
        modelCommentField.background = UIUtil.getTextFieldBackground()
        modelCommentField.document.addDocumentListener(simpleListener { updatePreview() })
    }

    private fun updateTableNameFieldState() {
        val enabled = tableNameDiffersCheckBox.isSelected
        if (enabled) {
            tableNameField.isEnabled = true
            tableNameField.isEditable = true
            tableNameField.isFocusable = true
            tableNameField.foreground = UIUtil.getTextFieldForeground()
            tableNameField.background = UIUtil.getTextFieldBackground()
            tableNameField.disabledTextColor = UIUtil.getTextFieldForeground()
            tableNameLabel.foreground = UIUtil.getLabelForeground()
        } else {
            // Keep component enabled to apply custom palette while preventing edits/focus.
            tableNameField.isEnabled = true
            tableNameField.isEditable = false
            tableNameField.isFocusable = false
            tableNameField.foreground = UIUtil.getLabelDisabledForeground()
            tableNameField.background = UIUtil.getPanelBackground()
            tableNameField.disabledTextColor = UIUtil.getLabelDisabledForeground()
            tableNameLabel.foreground = UIUtil.getLabelDisabledForeground()
        }
    }

    private fun initAttributesSection() {
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
                if (primaryKey) {
                    nullable = false
                    nullableCheckbox.isSelected = false
                }
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
        }, listenersDisposable)
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
            name = nextColumnName(src.name),
            type = src.type,
            primaryKey = src.primaryKey,
            nullable = src.nullable,
            unique = src.unique,
            defaultExpression = src.defaultExpression,
            comment = src.comment
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
        previewVisible = !previewVisible
        previewArrowLabel?.icon = if (previewVisible) UIUtil.getTreeExpandedIcon() else UIUtil.getTreeCollapsedIcon()
        previewContentPanel?.let {
            it.isVisible = previewVisible
            it.parent?.revalidate()
            it.parent?.repaint()
        }
    }

    private fun updatePreview() {
        val isReady = isPreviewReady()
        previewCardLayout.show(previewCardPanel, if (isReady) previewEditorCard else previewPlaceholderCard)
        if (!isReady) return

        val spec = SqlAlchemyModelSpec(
            mode = selectedMode(),
            modelName = modelNameField.text.trim(),
            tableName = effectiveTableName(),
            fileName = normalizedFileName(fileNameField.text),
            modelComment = modelCommentField.text.trim(),
            attributeTypesMapping = attributeTypesMappingCheckBox.isSelected,
            useLegacyColumns = useLegacyColumnsCheckBox.isSelected,
            columns = columnEntries()
        )
        val code = SqlAlchemyCodeGenerator.generate(spec)
        ApplicationManager.getApplication().runWriteAction {
            previewDocument.setText(code)
        }
    }

    private fun isPreviewReady(): Boolean {
        val modelName = modelNameField.text.trim()
        if (modelName.isBlank()) return false
        if (tableNameDiffersCheckBox.isSelected && tableNameField.text.trim().isBlank()) return false
        val cols = columnEntries()
        if (cols.isEmpty()) return false
        if (cols.any { it.name.isBlank() }) return false
        return true
    }

    // -----------------------------------------------------------------------
    // Misc
    // -----------------------------------------------------------------------

    private fun selectedMode() = when {
        dataSourceRadio.isSelected -> SqlAlchemyGenerationMode.DATA_SOURCE
        sqlRadio.isSelected -> SqlAlchemyGenerationMode.SQL
        else -> SqlAlchemyGenerationMode.MANUAL
    }

    private fun effectiveTableName(): String = if (tableNameDiffersCheckBox.isSelected) {
        tableNameField.text.trim()
    } else {
        toSnakeCase(modelNameField.text.trim())
    }

    private fun toSnakeCase(value: String): String = value
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("\\s+"), "_")
        .lowercase()
        .trim('_')

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

    private fun syncTextLater(field: JTextField, value: String) {
        if (field.text == value) return
        SwingUtilities.invokeLater {
            if (!field.isDisplayable) return@invokeLater
            if (field.text == value) return@invokeLater
            syncingFields = true
            field.text = value
            syncingFields = false
            updatePreview()
        }
    }

    private fun applyMonospaceFontToInputs() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val mono = Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
        listOf(modelNameField, tableNameField, fileNameField, columnNameField, commentField).forEach {
            it.font = mono
        }
        modelCommentField.font = mono
        columnTypeCombo.font = mono
        defaultExpressionField.font = mono
        previewEditor.component.font = mono
        commentField.foreground = UIUtil.getLabelDisabledForeground()
        modelCommentField.foreground = UIUtil.getTextFieldForeground()
        modelCommentField.background = UIUtil.getTextFieldBackground()
    }

    private fun JSplitPane.installInvisibleDivider() {
        setUI(object : BasicSplitPaneUI() {
            override fun createDefaultDivider(): BasicSplitPaneDivider {
                return object : BasicSplitPaneDivider(this) {
                    override fun paint(g: Graphics?) {
                        // Draw nothing - divider is invisible
                    }
                }
            }
        })
    }
}

// ---------------------------------------------------------------------------
// Cell renderer
// ---------------------------------------------------------------------------
private class ModelListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>, value: Any?, index: Int,
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
            val displayName = if (col.name.isBlank()) "(unnamed)" else col.name
            text = "  $displayName: ${col.type.displayName}${if (col.primaryKey) " [PK]" else ""}"
            icon = AllIcons.Nodes.Field
            border = JBUI.Borders.empty(2, 4)
            this
        }
        else -> super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    }
}
