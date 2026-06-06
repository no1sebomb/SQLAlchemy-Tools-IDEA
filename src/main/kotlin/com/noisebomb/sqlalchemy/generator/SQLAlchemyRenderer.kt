package com.noisebomb.sqlalchemy.generator

import com.noisebomb.sqlalchemy.model.ColumnDefinition
import com.noisebomb.sqlalchemy.model.ModelDefinition

object SqlAlchemyRenderer {

    fun render(model: ModelDefinition): String {
        val columns = model.elements.filterIsInstance<ColumnDefinition>()

        val body = columns.joinToString("\n\n") {
            "    ${it.name}: Mapped[...] = mapped_column()"
        }

        return """
from sqlalchemy.orm import Mapped, mapped_column
from db.base import Base

class ${model.modelName}(Base):
    __tablename__ = "${model.tableName}"

$body
        """.trimIndent()
    }
}