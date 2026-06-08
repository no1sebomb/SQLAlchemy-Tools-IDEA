package com.noisebomb.sqlalchemy.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.noisebomb.sqlalchemy.ui.SqlAlchemyIcons

class RelationshipLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return null
    }

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        for (element in elements) {

            val callExpression = element as? PyCallExpression
                ?: continue

            val calleeName = callExpression.callee?.name
                ?: continue

            if (calleeName != "relationship") {
                continue
            }

            val builder = NavigationGutterIconBuilder
                .create(SqlAlchemyIcons.RelationshipLink)
                .setTargets(element)
                .setTooltipText("SQLAlchemy relationship")

            result.add(
                builder.createLineMarkerInfo(callExpression)
            )
        }
    }
}