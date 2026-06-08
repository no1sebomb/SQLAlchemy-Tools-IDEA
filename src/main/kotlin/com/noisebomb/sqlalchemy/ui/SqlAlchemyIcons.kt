package com.noisebomb.sqlalchemy.ui

import com.intellij.openapi.util.IconLoader
import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnSpec
import javax.swing.Icon

/**
 * Plugin icons (IntelliJ Platform style) bundled under `resources/icons`.
 * Light/dark variants are resolved automatically by [IconLoader] via the `_dark` suffix.
 */
object SqlAlchemyIcons {
    /** SQLAlchemy model / table. */
    val Table: Icon = load("/icons/tableMapping.svg")

    /** Plain (nullable) column. */
    val Column: Icon = load("/icons/column.svg")

    /** Primary key column. */
    val ColumnPrimaryKey: Icon = load("/icons/columnPK.svg")

    /** Not-null (required) column. */
    val ColumnNotNull: Icon = load("/icons/columnNN.svg")

    /** Foreign key column. */
    val ColumnForeignKey: Icon = load("/icons/columnFK.svg")

    /** Relationship. */
    val Relationship: Icon = load("/icons/relationship.svg")
    val RelationshipLink: Icon = load("/icons/relationshipLink.svg")

    /** Picks the right column icon for the given spec. */
    fun forColumn(spec: SqlAlchemyColumnSpec): Icon = when {
        spec.primaryKey -> ColumnPrimaryKey
        !spec.nullable -> ColumnNotNull
        else -> Column
    }

    private fun load(path: String): Icon = IconLoader.getIcon(path, SqlAlchemyIcons::class.java)
}

