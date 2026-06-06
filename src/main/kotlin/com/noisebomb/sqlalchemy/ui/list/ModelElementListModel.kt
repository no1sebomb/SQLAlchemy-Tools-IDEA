package com.noisebomb.sqlalchemy.ui.list

import com.noisebomb.sqlalchemy.model.ModelElement
import javax.swing.AbstractListModel

class ModelElementListModel(
    private val elements: MutableList<ModelElement>
) : AbstractListModel<ModelElement>() {

    override fun getSize(): Int = elements.size

    override fun getElementAt(index: Int): ModelElement = elements[index]

    fun add(element: ModelElement) {
        elements.add(element)
        fireIntervalAdded(this, elements.size - 1, elements.size - 1)
    }

    fun remove(index: Int) {
        elements.removeAt(index)
        fireIntervalRemoved(this, index, index)
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        elements.swap(index, index - 1)
        fireContentsChanged(this, index - 1, index)
    }

    fun moveDown(index: Int) {
        if (index >= elements.size - 1) return
        elements.swap(index, index + 1)
        fireContentsChanged(this, index, index + 1)
    }

    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}