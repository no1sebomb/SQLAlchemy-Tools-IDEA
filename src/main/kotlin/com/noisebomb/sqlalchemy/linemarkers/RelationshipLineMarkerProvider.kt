package com.noisebomb.sqlalchemy.linemarkers

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.stubs.PyClassNameIndex
import com.noisebomb.sqlalchemy.ui.SqlAlchemyIcons

/**
 * Adds a gutter icon to SQLAlchemy `relationship(...)` attributes that declare a non-empty
 * `back_populates`, linking to the back-populated attribute on the related model.
 *
 * Performance notes:
 *  - The provider does the cheapest possible checks first and bails out early. It only ever does
 *    real work for the leaf identifier of a class attribute assigned to a `relationship(...)` call,
 *    so the vast majority of visited PSI elements are rejected with a couple of `==` comparisons.
 *  - Markers are attached to leaf elements only (required by the platform and avoids duplicates).
 *  - The related class is resolved through the [PyClassNameIndex] stub index (no AST parsing of
 *    other files) and the back-populated attribute via a stub-based [PyClass.findClassAttribute]
 *    lookup with a `null` type-eval context, so no expensive type inference is triggered.
 */
@Suppress("UnstableApiUsage")
class RelationshipLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // 1. Cheapest possible gate: only react to identifier leaves.
        if (element.firstChild != null) return
        if (element.node?.elementType !== PyTokenTypes.IDENTIFIER) return

        // 2. The identifier must be the name of a class attribute (`name = ...`).
        val target = element.parent as? PyTargetExpression ?: return
        if (target.nameIdentifier !== element) return

        // 3. The assigned value must be a `relationship(...)` call.
        val call = target.findAssignedValue() as? PyCallExpression ?: return
        if (!call.isRelationshipCall()) return

        // 4. `back_populates` must be present and non-empty.
        val backPopulates = call.stringKeywordArgument("back_populates")
        if (backPopulates.isNullOrEmpty()) return

        // 5. The attribute must live inside a model class that inherits a (declarative) base.
        val ownerClass = PsiTreeUtil.getParentOfType(target, PyClass::class.java) ?: return
        if (ownerClass.superClassExpressions.isEmpty()) return

        // 6. Resolve the related class referenced by the first positional argument.
        val relatedClassName = call.relationshipTargetClassName() ?: return
        val relatedAttribute = findBackPopulatedAttribute(element, relatedClassName, backPopulates) ?: return

        // 7. Build the gutter marker pointing to the back-populated attribute.
        val builder = NavigationGutterIconBuilder.create(SqlAlchemyIcons.RelationshipLink)
            .setTarget(relatedAttribute)
            .setTooltipText("Navigate to $relatedClassName.$backPopulates")
            .setPopupTitle("SQLAlchemy Relationship")

        result.add(builder.createLineMarkerInfo(element))
    }

    /** Matches `relationship(...)`, `orm.relationship(...)`, etc. (only the last name component is checked). */
    private fun PyCallExpression.isRelationshipCall(): Boolean =
        (callee as? PyReferenceExpression)?.referencedName == RELATIONSHIP

    /** Returns the string value of a keyword argument, or `null` when it is missing / not a string literal. */
    private fun PyCallExpression.stringKeywordArgument(name: String): String? =
        (getKeywordArgument(name) as? PyStringLiteralExpression)?.stringValue

    /**
     * Extracts the target class name from the first positional argument of `relationship(...)`.
     * Supports both string forms (`relationship("OrderItem")`, `relationship("pkg.OrderItem")`) and
     * the class-object form (`relationship(OrderItem)`). Only the simple (last) name component is kept.
     */
    private fun PyCallExpression.relationshipTargetClassName(): String? {
        val firstPositional = arguments.firstOrNull { it !is PyKeywordArgument } ?: return null
        val raw = when (firstPositional) {
            is PyStringLiteralExpression -> firstPositional.stringValue
            is PyReferenceExpression -> firstPositional.referencedName
            else -> null
        } ?: return null
        return raw.substringAfterLast('.').ifEmpty { null }
    }

    /**
     * Finds the `attributeName` attribute on the model named [className] within the project, returning the
     * first match that actually declares the attribute. Uses stub indexes only (no type inference).
     */
    private fun findBackPopulatedAttribute(
        context: PsiElement,
        className: String,
        attributeName: String
    ): PyTargetExpression? {
        val scope = GlobalSearchScope.projectScope(context.project)
        for (pyClass in PyClassNameIndex.find(className, context.project, scope)) {
            pyClass.findClassAttribute(attributeName, true, null)?.let { return it }
        }
        return null
    }

    private companion object {
        const val RELATIONSHIP = "relationship"
    }
}






