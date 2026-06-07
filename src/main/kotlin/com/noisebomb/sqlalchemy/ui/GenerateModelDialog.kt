package com.noisebomb.sqlalchemy.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.EditorTextField
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.treeStructure.Tree
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
import java.awt.Graphics
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

// ---------------------------------------------------------------------------
// Tree node data
// ---------------------------------------------------------------------------
private sealed interface TreeData
private object TableData : TreeData
private enum class FolderKind(val label: String, val active: Boolean) {
    COLUMNS("columns", true),
    INDEXES("indexes", false),
    RELATIONSHIPS("relationships", false)
}
private data class FolderData(val kind: FolderKind) : TreeData
private data class ColumnData(val spec: SqlAlchemyColumnSpec) : TreeData

// ---------------------------------------------------------------------------
// Generation source mode
// ---------------------------------------------------------------------------
private enum class EditMode(val label: String, val mode: SqlAlchemyGenerationMode) {
    MANUAL("Manual", SqlAlchemyGenerationMode.MANUAL),
    SQL("SQL", SqlAlchemyGenerationMode.SQL),
    DATA_SOURCE("Data Source", SqlAlchemyGenerationMode.DATA_SOURCE)
}

// ---------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------
class GenerateModelDialog(
    private val project: Project,
    private val targetDirectory: PsiDirectory? = null
) : DialogWrapper(project) {

    private val pythonFileType: FileType = FileTypeManager.getInstance().getFileTypeByExtension("py")
    private val sqlFileType: FileType = FileTypeManager.getInstance().getFileTypeByExtension("sql")

    private val disposable = Disposer.newDisposable()

    // Mono font shared by all inputs
    private val monoFont: Font = EditorColorsManager.getInstance().globalScheme.let {
        Font(it.editorFontName, Font.PLAIN, it.editorFontSize)
    }

    // ---- Mode selection ----
    private val modeProperty = AtomicProperty(EditMode.MANUAL)

    // ---- Tree ----
    private val rootNode = DefaultMutableTreeNode(TableData)
    private val columnsFolder = DefaultMutableTreeNode(FolderData(FolderKind.COLUMNS))
    private val indexesFolder = DefaultMutableTreeNode(FolderData(FolderKind.INDEXES))
    private val relationshipsFolder = DefaultMutableTreeNode(FolderData(FolderKind.RELATIONSHIPS))
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    // ---- Options: selected component header card ----
    private val cardIconLabel = JBLabel()
    private val optionsCardLayout = CardLayout()
    private val optionsCardPanel = JPanel(optionsCardLayout)

    // ---- Table options ----
    private val modelNameField = JBTextField()
    private val tableNameDiffersCheckBox = JBCheckBox("Different table name")
    private val tableNameField = JBTextField()
    private val tableDescriptionArea = JBTextArea(3, 20)

    // ---- Column options ----
    private val attributeNameField = JBTextField()
    private val columnNameDiffersCheckBox = JBCheckBox("Different column name")
    private val columnNameField = JBTextField()
    private val typeCombo = ComboBox(SqlAlchemyColumnType.entries.toTypedArray())
    private val primaryKeyCheckbox = JBCheckBox("Primary key")
    private val uniqueCheckbox = JBCheckBox("Unique")
    private val nullableCheckbox = JBCheckBox("Nullable")
    private val defaultExpressionField = EditorTextField("", project, pythonFileType)
    private val columnDescriptionArea = JBTextArea(3, 20)

    // ---- SQL mode ----
    private val sqlEditor = EditorTextField("", project, sqlFileType).apply { setOneLineMode(false) }

    // ---- Data Source mode ----
    private val dataSourceCombo = ComboBox(arrayOf("(no connected data sources)"))
    private val schemaCombo = ComboBox(arrayOf("public"))
    private val dsTableCombo = ComboBox(arrayOf<String>())

    // ---- Preview ----
    private val editorFactory = EditorFactory.getInstance()
    private val previewDocument = editorFactory.createDocument("")
    private val previewEditor: Editor = editorFactory.createViewer(previewDocument, project).also { editor ->
        editor.settings.apply {
            isLineNumbersShown = true
            isFoldingOutlineShown = false
            isRightMarginShown = false
            isVirtualSpace = false
            isLineMarkerAreaShown = false
            additionalColumnsCount = 0
        }
        (editor as? EditorEx)?.highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, pythonFileType)
    }
    private val previewCardLayout = CardLayout()
    private val previewCardPanel = JPanel(previewCardLayout)
    private val previewPlaceholderCard = "placeholder"
    private val previewEditorCard = "editor"
    private var previewVisible = true
    private var previewArrowLabel: JLabel? = null
    private var previewContentPanel: JPanel? = null
    private var previewExpandedDividerLocation: Int? = null
    private var verticalSplit: JSplitPane? = null
    private var horizontalSplit: JSplitPane? = null

    // Content area that hosts the mode-specific layout above the preview
    private val contentPanel = JPanel(BorderLayout())
    private var treeOptionsPanel: JComponent? = null
    private var sqlSplit: JSplitPane? = null

    // ---- Generation options (behind the preview gear button) ----
    private var attributeTypesMapping = true
    private var useLegacyColumns = false
    private var wrapLines = false

    // ---- State ----
    private var fileName: String = ""
    private var fileNameUserEdited = false
    private var loading = false
    private var syncing = false

    init {
        title = "Create SQLAlchemy Model"
        setOKButtonText("Create")
        buildTreeContent()
        initListeners()
        applyMonospaceFont()
        init()
        loadSelection()
        updatePreview()
    }

    override fun dispose() {
        Disposer.dispose(disposable)
        editorFactory.releaseEditor(previewEditor)
        super.dispose()
    }

    // -----------------------------------------------------------------------
    // Result
    // -----------------------------------------------------------------------
    fun getModelSpec(): SqlAlchemyModelSpec = SqlAlchemyModelSpec(
        mode = modeProperty.get().mode,
        modelName = modelNameField.text.trim(),
        tableName = effectiveTableName(),
        fileName = effectiveFileName(),
        modelComment = tableDescriptionArea.text.trim(),
        attributeTypesMapping = attributeTypesMapping,
        useLegacyColumns = useLegacyColumns,
        columns = columnSpecs()
    )

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------
    override fun createCenterPanel(): JComponent {
        val root = JPanel(BorderLayout(0, JBUIScale.scale(8)))
        root.border = JBUI.Borders.empty(8)
        root.preferredSize = Dimension(JBUIScale.scale(820), JBUIScale.scale(700))

        root.add(buildModeSelector(), BorderLayout.NORTH)

        treeOptionsPanel = buildTreeOptionsSplit()

        verticalSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, true, contentPanel, buildPreviewPanel()).apply {
            resizeWeight = 1.0
            border = JBUI.Borders.empty()
            installInvisibleDivider()
            dividerSize = JBUIScale.scale(7)
        }
        applyModeLayout(modeProperty.get())
        root.add(verticalSplit!!, BorderLayout.CENTER)

        SwingUtilities.invokeLater {
            horizontalSplit?.setDividerLocation(0.33)
            verticalSplit?.setDividerLocation(0.62)
        }
        return root
    }

    override fun getDimensionServiceKey(): String = "com.noisebomb.sqlalchemy.GenerateModelDialog"

    // -----------------------------------------------------------------------
    // Mode selector (segmented button)
    // -----------------------------------------------------------------------
    private fun buildModeSelector(): JComponent {
        val selector = panel {
            row {
                segmentedButton(EditMode.entries.toList()) { text = it.label }
                    .bind(modeProperty)
                    .align(AlignX.CENTER)
            }
        }
        modeProperty.afterChange(disposable) { onModeChanged(it) }
        return selector
    }

    private fun onModeChanged(mode: EditMode) {
        applyModeLayout(mode)
        updatePreview()
    }

    /**
     * Rebuilds the content area so the tree/options section stays visible in every mode:
     *  - Manual: tree/options fill the whole area.
     *  - SQL: a monospaced 5-row DDL editor sits above the tree/options with a draggable divider.
     *  - Data Source: fixed-height connection fields sit above the tree/options.
     */
    private fun applyModeLayout(mode: EditMode) {
        contentPanel.removeAll()
        val treeOptions = treeOptionsPanel ?: return
        when (mode) {
            EditMode.MANUAL -> {
                contentPanel.add(treeOptions, BorderLayout.CENTER)
            }
            EditMode.SQL -> {
                val split = JSplitPane(JSplitPane.VERTICAL_SPLIT, true, buildSqlPanel(), treeOptions).apply {
                    resizeWeight = 0.0
                    border = JBUI.Borders.empty()
                    installInvisibleDivider()
                    dividerSize = JBUIScale.scale(7)
                }
                sqlSplit = split
                contentPanel.add(split, BorderLayout.CENTER)
                SwingUtilities.invokeLater { split.dividerLocation = sqlPreferredHeight() }
            }
            EditMode.DATA_SOURCE -> {
                contentPanel.add(buildDataSourcePanel(), BorderLayout.NORTH)
                contentPanel.add(treeOptions, BorderLayout.CENTER)
            }
        }
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun sqlPreferredHeight(): Int = previewEditor.lineHeight * 5 + JBUIScale.scale(34)

    // -----------------------------------------------------------------------
    // Tree + options split
    // -----------------------------------------------------------------------
    private fun buildTreeOptionsSplit(): JComponent {
        horizontalSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, buildTreePanel(), buildOptionsPanel()).apply {
            resizeWeight = 0.33
            border = JBUI.Borders.empty()
            installInvisibleDivider()
            dividerSize = JBUIScale.scale(7)
        }
        return horizontalSplit!!
    }

    private fun buildTreePanel(): JComponent {
        tree.isRootVisible = true
        tree.showsRootHandles = false
        tree.toggleClickCount = 0
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = ModelTreeCellRenderer()
        // The structure is fixed and always fully expanded: disallow collapsing.
        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {}
            override fun treeWillCollapse(event: TreeExpansionEvent) = throw ExpandVetoException(event)
        })
        expandTree()
        (columnsFolder.firstChild as? DefaultMutableTreeNode)?.let {
            tree.selectionPath = TreePath(it.path)
        }

        val decorator = ToolbarDecorator.createDecorator(tree)
            .setAddAction { addColumn() }
            .setRemoveAction { removeSelectedColumn() }
            .setMoveUpAction { moveColumn(-1) }
            .setMoveDownAction { moveColumn(1) }
            .setAddActionUpdater { true }
            .setRemoveActionUpdater { selectedColumnNode() != null }
            .setMoveUpActionUpdater { canMoveColumn(-1) }
            .setMoveDownActionUpdater { canMoveColumn(1) }
            .addExtraAction(object : AnAction("Duplicate", "Duplicate selected column", AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) = duplicateSelectedColumn()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = selectedColumnNode() != null
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            .setToolbarPosition(ActionToolbarPosition.TOP)

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 0, 0, 4)
            add(decorator.createPanel(), BorderLayout.CENTER)
            minimumSize = Dimension(JBUIScale.scale(200), JBUIScale.scale(200))
        }
    }

    private fun buildOptionsPanel(): JComponent {
        cardIconLabel.iconTextGap = JBUIScale.scale(6)
        val headerCard = JPanel(FlowLayout(FlowLayout.LEFT, JBUIScale.scale(6), JBUIScale.scale(4))).apply {
            border = BorderFactory.createCompoundBorder(
                RoundedLineBorder(UIUtil.getBoundsColor(), JBUIScale.scale(8), 1),
                JBUI.Borders.empty(3, 8)
            )
            isOpaque = false
            add(cardIconLabel)
        }
        val headerWrap = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 0, 8, 0)
            add(headerCard, BorderLayout.WEST)
        }

        optionsCardPanel.add(buildTableCard(), "table")
        optionsCardPanel.add(buildColumnCard(), "column")
        optionsCardPanel.add(buildEmptyCard(), "empty")

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 4, 0, 0)
            add(headerWrap, BorderLayout.NORTH)
            add(optionsCardPanel, BorderLayout.CENTER)
            minimumSize = Dimension(JBUIScale.scale(280), JBUIScale.scale(200))
        }
    }

    private fun buildTableCard(): JComponent {
        tableDescriptionArea.lineWrap = true
        tableDescriptionArea.wrapStyleWord = true
        tableDescriptionArea.rows = 2
        val descScroll = JBScrollPane(tableDescriptionArea).apply {
            minimumSize = Dimension(0, JBUIScale.scale(44))
            preferredSize = Dimension(0, JBUIScale.scale(64))
        }
        val form = panel {
            row("Name:") {
                cell(modelNameField).resizableColumn().align(AlignX.FILL)
            }
            row { cell(tableNameDiffersCheckBox) }
            indent {
                row("Table:") {
                    cell(tableNameField).resizableColumn().align(AlignX.FILL)
                }
            }
            row("Description:") { }.topGap(TopGap.SMALL)
            row {
                cell(descScroll).resizableColumn().align(AlignX.FILL)
            }
        }.apply { border = JBUI.Borders.empty(4) }
        return wrapScrollable(form)
    }

    private fun buildColumnCard(): JComponent {
        defaultExpressionField.setOneLineMode(true)
        columnDescriptionArea.lineWrap = true
        columnDescriptionArea.wrapStyleWord = true
        columnDescriptionArea.rows = 2
        val descScroll = JBScrollPane(columnDescriptionArea).apply {
            minimumSize = Dimension(0, JBUIScale.scale(44))
            preferredSize = Dimension(0, JBUIScale.scale(64))
        }
        ComboboxSpeedSearch.installOn(typeCombo)

        val form = panel {
            row("Name:") {
                cell(attributeNameField).resizableColumn().align(AlignX.FILL)
            }
            row { cell(columnNameDiffersCheckBox) }
            indent {
                row("Column:") {
                    cell(columnNameField).resizableColumn().align(AlignX.FILL)
                }
            }
            row("Type:") {
                cell(typeCombo).resizableColumn().align(AlignX.FILL)
            }
            row {
                cell(primaryKeyCheckbox)
                cell(uniqueCheckbox)
                cell(nullableCheckbox)
            }
            row("Default:") {
                cell(defaultExpressionField).resizableColumn().align(AlignX.FILL)
            }
            row("Description:") { }.topGap(TopGap.SMALL)
            row {
                cell(descScroll).resizableColumn().align(AlignX.FILL)
            }
        }.apply { border = JBUI.Borders.empty(4) }
        return wrapScrollable(form)
    }

    /** Wraps an options form so it scrolls (instead of clipping) when the area is too short. */
    private fun wrapScrollable(content: JComponent): JComponent = JBScrollPane(content).apply {
        border = JBUI.Borders.empty()
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBar.unitIncrement = JBUIScale.scale(16)
    }

    private fun buildEmptyCard(): JComponent = JPanel(BorderLayout()).apply {
        add(JLabel("Select an item to edit its options", SwingConstants.CENTER).apply {
            foreground = UIUtil.getLabelDisabledForeground()
            font = font.deriveFont(Font.ITALIC)
        }, BorderLayout.CENTER)
    }

    // -----------------------------------------------------------------------
    // SQL panel
    // -----------------------------------------------------------------------
    private fun buildSqlPanel(): JComponent {
        sqlEditor.setPlaceholder("-- Paste CREATE TABLE DDL here")
        return JPanel(BorderLayout(0, JBUIScale.scale(4))).apply {
            border = JBUI.Borders.empty(4)
            add(JLabel("SQL (DDL):"), BorderLayout.NORTH)
            add(sqlEditor, BorderLayout.CENTER)
        }
    }

    // -----------------------------------------------------------------------
    // Data Source panel
    // -----------------------------------------------------------------------
    private fun buildDataSourcePanel(): JComponent {
        dataSourceCombo.isEnabled = false
        schemaCombo.isEnabled = false
        dsTableCombo.isEnabled = false
        return panel {
            row {
                comment("Database integration is coming soon. Connect a data source in the Database tool window.")
            }
            row("Data source:") { cell(dataSourceCombo).align(AlignX.FILL) }
            row("Schema:") { cell(schemaCombo).align(AlignX.FILL) }
            row("Table:") { cell(dsTableCombo).align(AlignX.FILL) }
        }.apply { border = JBUI.Borders.empty(8) }
    }

    // -----------------------------------------------------------------------
    // Preview panel
    // -----------------------------------------------------------------------
    private fun buildPreviewPanel(): JComponent {
        val placeholderPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(JLabel("Fill in required values to preview", SwingConstants.CENTER).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                font = font.deriveFont(Font.ITALIC)
            }, BorderLayout.CENTER)
            preferredSize = Dimension(0, JBUIScale.scale(160))
        }
        val editorComponent = previewEditor.component.also {
            it.preferredSize = Dimension(0, JBUIScale.scale(160))
        }
        previewCardPanel.add(placeholderPanel, previewPlaceholderCard)
        previewCardPanel.add(editorComponent, previewEditorCard)

        val content = JPanel(BorderLayout()).apply {
            add(previewCardPanel, BorderLayout.CENTER)
            isVisible = previewVisible
        }.also { previewContentPanel = it }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            add(buildPreviewHeader(), BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }
    }

    private fun buildPreviewHeader(): JComponent {
        val arrowLabel = JLabel(UIUtil.getTreeExpandedIcon()).also { previewArrowLabel = it }
        val titleLabel = JLabel("Preview").apply { foreground = UIUtil.getLabelForeground() }

        val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUIScale.scale(4), 0)).apply {
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            add(arrowLabel)
            add(titleLabel)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = togglePreview()
            })
        }

        val optionsButton = iconButton(AllIcons.General.GearPlain, "Generation options") { showOptionsPopup(it) }
        val copyButton = iconButton(AllIcons.Actions.Copy, "Copy generated code") { copyPreview() }
        val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUIScale.scale(4), 0)).apply {
            isOpaque = false
            add(optionsButton)
            add(copyButton)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 2, 4, 2)
            add(titlePanel, BorderLayout.WEST)
            add(actionsPanel, BorderLayout.EAST)
        }
    }

    private fun iconButton(icon: Icon, tooltip: String, onClick: (Component) -> Unit): JComponent {
        return JBLabel(icon).apply {
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(2)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = onClick(this@apply)
            })
        }
    }

    private fun showOptionsPopup(anchor: Component) {
        val typesMappingBox = JBCheckBox("Use Mapped[] type annotations", attributeTypesMapping)
        val legacyBox = JBCheckBox("Use legacy Column()", useLegacyColumns)
        val wrapBox = JBCheckBox("Wrap lines in preview", wrapLines)
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
            add(typesMappingBox)
            add(legacyBox)
            add(wrapBox)
        }
        typesMappingBox.addActionListener {
            attributeTypesMapping = typesMappingBox.isSelected
            updatePreview()
        }
        legacyBox.addActionListener {
            useLegacyColumns = legacyBox.isSelected
            updatePreview()
        }
        wrapBox.addActionListener {
            wrapLines = wrapBox.isSelected
            (previewEditor as? EditorEx)?.settings?.isUseSoftWraps = wrapLines
        }
        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, typesMappingBox)
            .setRequestFocus(true)
            .setTitle("Options")
            .createPopup()
            .showUnderneathOf(anchor)
    }

    private fun copyPreview() {
        CopyPasteManager.getInstance().setContents(StringSelection(previewDocument.text))
    }

    private fun togglePreview() {
        previewVisible = !previewVisible
        previewArrowLabel?.icon = if (previewVisible) UIUtil.getTreeExpandedIcon() else UIUtil.getTreeCollapsedIcon()
        val split = verticalSplit ?: return
        if (!previewVisible) {
            previewExpandedDividerLocation = split.dividerLocation
        }
        previewContentPanel?.isVisible = previewVisible
        split.dividerSize = if (previewVisible) JBUIScale.scale(7) else 0
        split.revalidate()
        split.repaint()
        SwingUtilities.invokeLater {
            val s = verticalSplit ?: return@invokeLater
            if (!previewVisible) {
                s.dividerLocation = s.maximumDividerLocation
                return@invokeLater
            }
            val fallback = (s.height * 0.62).toInt()
            val minPreviewHeight = JBUIScale.scale(140)
            val upperBound = minOf(s.maximumDividerLocation, s.height - minPreviewHeight)
            val clampedUpper = maxOf(s.minimumDividerLocation, upperBound)
            s.dividerLocation = (previewExpandedDividerLocation ?: fallback)
                .coerceIn(s.minimumDividerLocation, clampedUpper)
            s.revalidate()
            s.repaint()
        }
    }

    // -----------------------------------------------------------------------
    // Tree content / actions
    // -----------------------------------------------------------------------
    private fun buildTreeContent() {
        rootNode.add(columnsFolder)
        rootNode.add(indexesFolder)
        rootNode.add(relationshipsFolder)
        columnsFolder.add(
            DefaultMutableTreeNode(
                ColumnData(SqlAlchemyColumnSpec("id", SqlAlchemyColumnType.INTEGER, primaryKey = true, nullable = false))
            )
        )
        treeModel.reload()
    }

    private fun expandTree() {
        tree.expandPath(TreePath(rootNode.path))
        tree.expandPath(TreePath(columnsFolder.path))
    }

    private fun addColumn() {
        val spec = SqlAlchemyColumnSpec(name = nextColumnName("column"), type = SqlAlchemyColumnType.STRING, nullable = true)
        val node = DefaultMutableTreeNode(ColumnData(spec))
        val selected = selectedColumnNode()
        val insertIndex = if (selected != null) columnsFolder.getIndex(selected) + 1 else columnsFolder.childCount
        treeModel.insertNodeInto(node, columnsFolder, insertIndex)
        tree.selectionPath = TreePath(node.path)
        updatePreview()
        SwingUtilities.invokeLater { attributeNameField.requestFocusInWindow() }
    }

    private fun removeSelectedColumn() {
        val node = selectedColumnNode() ?: return
        val index = columnsFolder.getIndex(node)
        treeModel.removeNodeFromParent(node)
        val next: DefaultMutableTreeNode = when {
            columnsFolder.childCount == 0 -> columnsFolder
            index < columnsFolder.childCount -> columnsFolder.getChildAt(index) as DefaultMutableTreeNode
            else -> columnsFolder.getChildAt(columnsFolder.childCount - 1) as DefaultMutableTreeNode
        }
        tree.selectionPath = TreePath(next.path)
        updatePreview()
    }

    private fun duplicateSelectedColumn() {
        val node = selectedColumnNode() ?: return
        val src = (node.userObject as ColumnData).spec
        val copy = SqlAlchemyColumnSpec(
            name = nextColumnName(src.name.ifBlank { "column" }),
            type = src.type,
            primaryKey = src.primaryKey,
            nullable = src.nullable,
            unique = src.unique,
            defaultExpression = src.defaultExpression,
            comment = src.comment,
            columnNameDiffers = src.columnNameDiffers,
            columnName = src.columnName
        )
        val newNode = DefaultMutableTreeNode(ColumnData(copy))
        val insertIndex = columnsFolder.getIndex(node) + 1
        treeModel.insertNodeInto(newNode, columnsFolder, insertIndex)
        tree.selectionPath = TreePath(newNode.path)
        updatePreview()
    }

    private fun moveColumn(delta: Int) {
        val node = selectedColumnNode() ?: return
        val index = columnsFolder.getIndex(node)
        val target = index + delta
        if (target < 0 || target >= columnsFolder.childCount) return
        treeModel.removeNodeFromParent(node)
        treeModel.insertNodeInto(node, columnsFolder, target)
        tree.selectionPath = TreePath(node.path)
        updatePreview()
    }

    private fun canMoveColumn(delta: Int): Boolean {
        val node = selectedColumnNode() ?: return false
        val target = columnsFolder.getIndex(node) + delta
        return target in 0 until columnsFolder.childCount
    }

    private fun selectedColumnNode(): DefaultMutableTreeNode? {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
        return if (node.userObject is ColumnData) node else null
    }

    private fun nextColumnName(base: String): String {
        val existing = columnSpecs().map { it.name }.toSet()
        if (base !in existing) return base
        var i = 2
        while ("${base}_$i" in existing) i++
        return "${base}_$i"
    }

    private fun columnSpecs(): List<SqlAlchemyColumnSpec> =
        (0 until columnsFolder.childCount).map {
            ((columnsFolder.getChildAt(it) as DefaultMutableTreeNode).userObject as ColumnData).spec
        }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------
    private fun initListeners() {
        tree.addTreeSelectionListener { loadSelection() }

        // Table options
        modelNameField.document.addDocumentListener(simpleListener {
            if (syncing) return@simpleListener
            if (!tableNameDiffersCheckBox.isSelected) {
                syncTextLater(tableNameField, toSnakeCase(modelNameField.text))
            }
            if (!fileNameUserEdited) {
                fileName = suggestedFileName(modelNameField.text)
            }
            treeModel.nodeChanged(rootNode)
            updateCardHeader()
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
            if (syncing) return@simpleListener
            if (tableNameDiffersCheckBox.isSelected) updatePreview()
        })
        tableDescriptionArea.document.addDocumentListener(simpleListener { updatePreview() })

        // Column options
        attributeNameField.document.addDocumentListener(simpleListener {
            updateSelectedColumn { name = attributeNameField.text.trim() }
            (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.let { treeModel.nodeChanged(it) }
            updateCardHeader()
        })
        columnNameDiffersCheckBox.addActionListener {
            updateColumnNameFieldState()
            updateSelectedColumn {
                columnNameDiffers = columnNameDiffersCheckBox.isSelected
                columnName = if (columnNameDiffersCheckBox.isSelected) columnNameField.text.trim() else ""
            }
        }
        columnNameField.document.addDocumentListener(simpleListener {
            if (columnNameDiffersCheckBox.isSelected) updateSelectedColumn { columnName = columnNameField.text.trim() }
        })
        typeCombo.addActionListener {
            updateSelectedColumn { type = typeCombo.selectedItem as SqlAlchemyColumnType }
            (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.let { treeModel.nodeChanged(it) }
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
            (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.let { treeModel.nodeChanged(it) }
        }
        uniqueCheckbox.addActionListener { updateSelectedColumn { unique = uniqueCheckbox.isSelected } }
        nullableCheckbox.addActionListener { updateSelectedColumn { nullable = nullableCheckbox.isSelected } }
        defaultExpressionField.document.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                updateSelectedColumn { defaultExpression = defaultExpressionField.text }
            }
        }, disposable)
        columnDescriptionArea.document.addDocumentListener(simpleListener {
            updateSelectedColumn { comment = columnDescriptionArea.text }
        })

        sqlEditor.document.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) = updatePreview()
        }, disposable)

        updateTableNameFieldState()
        updateColumnNameFieldState()
    }

    // -----------------------------------------------------------------------
    // Selection loading
    // -----------------------------------------------------------------------
    private fun loadSelection() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
        when (val data = node?.userObject) {
            is TableData -> {
                optionsCardLayout.show(optionsCardPanel, "table")
            }
            is ColumnData -> {
                loadColumnCard(data.spec)
                optionsCardLayout.show(optionsCardPanel, "column")
            }
            else -> optionsCardLayout.show(optionsCardPanel, "empty")
        }
        updateCardHeader()
    }

    private fun loadColumnCard(spec: SqlAlchemyColumnSpec) {
        loading = true
        try {
            attributeNameField.text = spec.name
            columnNameDiffersCheckBox.isSelected = spec.columnNameDiffers
            columnNameField.text = spec.columnName
            typeCombo.selectedItem = spec.type
            primaryKeyCheckbox.isSelected = spec.primaryKey
            uniqueCheckbox.isSelected = spec.unique
            nullableCheckbox.isSelected = spec.nullable
            nullableCheckbox.isEnabled = !spec.primaryKey
            defaultExpressionField.text = spec.defaultExpression
            columnDescriptionArea.text = spec.comment
            updateColumnNameFieldState()
        } finally {
            loading = false
        }
    }

    private fun updateCardHeader() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
        when (val data = node?.userObject) {
            is TableData -> {
                cardIconLabel.icon = AllIcons.Nodes.DataTables
                cardIconLabel.text = tableDisplayName()
            }
            is ColumnData -> {
                cardIconLabel.icon = AllIcons.Nodes.DataColumn
                cardIconLabel.text = data.spec.name.ifBlank { "(unnamed)" }
            }
            is FolderData -> {
                cardIconLabel.icon = AllIcons.Nodes.Folder
                cardIconLabel.text = data.kind.label
            }
            else -> {
                cardIconLabel.icon = null
                cardIconLabel.text = ""
            }
        }
    }

    private fun updateSelectedColumn(update: SqlAlchemyColumnSpec.() -> Unit) {
        if (loading) return
        val node = selectedColumnNode() ?: return
        (node.userObject as ColumnData).spec.update()
        updatePreview()
    }

    private fun updateTableNameFieldState() {
        val enabled = tableNameDiffersCheckBox.isSelected
        tableNameField.isEnabled = enabled
        if (!enabled && !syncing) {
            tableNameField.text = toSnakeCase(modelNameField.text)
        }
    }

    private fun updateColumnNameFieldState() {
        columnNameField.isEnabled = columnNameDiffersCheckBox.isSelected
    }

    // -----------------------------------------------------------------------
    // Preview
    // -----------------------------------------------------------------------
    private fun updatePreview() {
        if (modeProperty.get() != EditMode.MANUAL || !isPreviewReady()) {
            previewCardLayout.show(previewCardPanel, previewPlaceholderCard)
            return
        }
        previewCardLayout.show(previewCardPanel, previewEditorCard)
        val code = SqlAlchemyCodeGenerator.generate(getModelSpec())
        ApplicationManager.getApplication().runWriteAction {
            previewDocument.setText(code)
        }
    }

    private fun isPreviewReady(): Boolean {
        if (modelNameField.text.trim().isBlank()) return false
        if (tableNameDiffersCheckBox.isSelected && tableNameField.text.trim().isBlank()) return false
        val cols = columnSpecs()
        if (cols.isEmpty() || cols.any { it.name.isBlank() }) return false
        return true
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------
    override fun doValidate(): ValidationInfo? {
        if (modeProperty.get() != EditMode.MANUAL) {
            return ValidationInfo("This generation mode is coming soon. Please use Manual for now.")
        }
        val modelName = modelNameField.text.trim()
        if (modelName.isEmpty()) return ValidationInfo("Model name is required", modelNameField)
        if (!modelName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
            return ValidationInfo("Model name must be a valid Python class name", modelNameField)
        }
        if (tableNameDiffersCheckBox.isSelected) {
            val tableName = tableNameField.text.trim()
            if (tableName.isEmpty()) return ValidationInfo("Table name is required", tableNameField)
            if (!tableName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                return ValidationInfo("Table name should contain only letters, digits and underscore", tableNameField)
            }
        }

        val cols = columnSpecs()
        if (cols.isEmpty()) return ValidationInfo("At least one column is required", tree)
        for (col in cols) {
            if (col.name.isBlank()) return ValidationInfo("All columns must have an attribute name", attributeNameField)
            if (!col.name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                return ValidationInfo("Attribute '${col.name}' must be a valid Python identifier", attributeNameField)
            }
            if (col.columnNameDiffers && col.columnName.isBlank()) {
                return ValidationInfo("Column name is required when 'Different column name' is checked", columnNameField)
            }
        }
        val dup = cols.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }
        if (dup != null) return ValidationInfo("Duplicate attribute name: ${dup.key}", tree)

        val file = effectiveFileName()
        if (targetDirectory?.findFile(file) != null) {
            return ValidationInfo("File '$file' already exists in the selected directory")
        }
        return null
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private fun tableDisplayName(): String = modelNameField.text.trim().ifBlank { "Unnamed" }

    private fun effectiveTableName(): String = if (tableNameDiffersCheckBox.isSelected) {
        tableNameField.text.trim()
    } else {
        toSnakeCase(modelNameField.text.trim())
    }

    private fun effectiveFileName(): String =
        if (fileNameUserEdited && fileName.isNotBlank()) fileName else suggestedFileName(modelNameField.text)

    private fun toSnakeCase(value: String): String = value
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("\\s+"), "_")
        .lowercase()
        .trim('_')

    private fun suggestedFileName(value: String): String {
        val base = toSnakeCase(value.trim())
        return if (base.isBlank()) "model.py" else "$base.py"
    }

    private fun simpleListener(callback: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = callback()
        override fun removeUpdate(e: DocumentEvent?) = callback()
        override fun changedUpdate(e: DocumentEvent?) = callback()
    }

    private fun syncTextLater(field: JTextField, value: String) {
        if (field.text == value) return
        SwingUtilities.invokeLater {
            if (!field.isDisplayable || field.text == value) return@invokeLater
            syncing = true
            field.text = value
            syncing = false
            updatePreview()
        }
    }

    private fun applyMonospaceFont() {
        listOf(
            modelNameField, tableNameField, attributeNameField, columnNameField
        ).forEach { it.font = monoFont }
        typeCombo.font = monoFont
        tableDescriptionArea.font = monoFont
        columnDescriptionArea.font = monoFont
        // EditorTextField components already use the editor (mono) font.
    }

    /** Keeps the split divider draggable but paints nothing, so no stray lines appear. */
    private fun JSplitPane.installInvisibleDivider() {
        setUI(object : BasicSplitPaneUI() {
            override fun createDefaultDivider(): BasicSplitPaneDivider =
                object : BasicSplitPaneDivider(this) {
                    override fun paint(g: Graphics?) {
                        // intentionally empty: invisible divider
                    }
                }
        })
        border = JBUI.Borders.empty()
    }

    // -----------------------------------------------------------------------
    // Tree cell renderer
    // -----------------------------------------------------------------------
    private inner class ModelTreeCellRenderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean,
            expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ) {
            val node = value as? DefaultMutableTreeNode ?: return
            when (val data = node.userObject) {
                is TableData -> {
                    icon = AllIcons.Nodes.DataTables
                    append(tableDisplayName())
                }
                is FolderData -> {
                    icon = AllIcons.Nodes.Folder
                    val attrs = if (data.kind.active) SimpleTextAttributes.REGULAR_ATTRIBUTES
                    else SimpleTextAttributes.GRAYED_ATTRIBUTES
                    append(data.kind.label, attrs)
                    if (data.kind == FolderKind.COLUMNS) {
                        append("  ${columnsFolder.childCount}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
                is ColumnData -> {
                    icon = AllIcons.Nodes.DataColumn
                    val col = data.spec
                    append(col.name.ifBlank { "(unnamed)" })
                    append("  ${col.type.pythonType}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    if (col.primaryKey) append("  [PK]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }
}


