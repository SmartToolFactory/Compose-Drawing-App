package com.smarttoolfactory.composedrawingapp.viewmodel

import androidx.lifecycle.ViewModel
import com.smarttoolfactory.composedrawingapp.model.MyLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CanvasViewModel: ViewModel() {
    private val mutableLineList = mutableListOf<MyLine>()
    private val redoLineList = mutableListOf<MyLine>()
    private val _lineList = MutableStateFlow(mutableLineList.toList())
    val lineList: StateFlow<List<MyLine>> = _lineList

    fun updateLine(newLine: MyLine) {
        mutableLineList.add(newLine)
        updateToFlow()
    }

    fun undo() {
        // Ignore for my use case.
        if (mutableLineList.isNotEmpty()) {

            val lastItem = mutableLineList.last()
            mutableLineList.remove(lastItem)
            redoLineList.add(lastItem)
            updateToFlow()
        }
    }

    fun redo() {
        // Ignore for my use case.
        if (redoLineList.isNotEmpty()) {

            val last = redoLineList.removeLast()
            mutableLineList.add(last)
            updateToFlow()
        }
    }

    fun clearRedo() {
        redoLineList.clear()
    }

    private fun updateToFlow() {
        _lineList.value = mutableLineList.toList()
    }
}