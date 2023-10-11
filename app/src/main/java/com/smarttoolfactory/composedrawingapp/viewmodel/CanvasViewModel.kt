package com.smarttoolfactory.composedrawingapp.viewmodel

import androidx.lifecycle.ViewModel
import com.smarttoolfactory.composedrawingapp.DrawMode
import com.smarttoolfactory.composedrawingapp.gesture.MotionEvent
import com.smarttoolfactory.composedrawingapp.model.MyLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CanvasViewModel: ViewModel() {
    private val mutableLineList = mutableListOf<MyLine>()
    private val redoLineList = mutableListOf<MyLine>()
    private val _lineList = MutableStateFlow(mutableLineList.toList())
    val lineList: StateFlow<List<MyLine>> = _lineList

    private val _drawMode = MutableStateFlow(DrawMode.Draw)
    val drawMode: StateFlow<DrawMode> = _drawMode

    private val _motionEvent = MutableStateFlow(MotionEvent.Idle)
    val motionEvent: StateFlow<MotionEvent> = _motionEvent

    fun updateLine(newLine: MyLine) {
        // For my use case there should only be 1 line from user.
        mutableLineList.clear()
        mutableLineList.add(newLine)
        updateToFlow()
    }

    fun updateDrawMode(newDrawMode: DrawMode) {
        _drawMode.value = newDrawMode
    }

    fun updateMotionEvent(newMotionEvent: MotionEvent) {
        _motionEvent.value = newMotionEvent
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