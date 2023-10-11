package com.smarttoolfactory.composedrawingapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.composedrawingapp.gesture.MotionEvent
import com.smarttoolfactory.composedrawingapp.model.MyLine
import com.smarttoolfactory.composedrawingapp.ui.canvas.DrawingCanvas
import com.smarttoolfactory.composedrawingapp.ui.menu.LayoutInfoMenus
import com.smarttoolfactory.composedrawingapp.ui.theme.backgroundColor
import com.smarttoolfactory.composedrawingapp.viewmodel.CanvasViewModel

@Composable
fun DrawingApp(
    paddingValues: PaddingValues,
    viewModel: CanvasViewModel
) {
    val paths by viewModel.lineList.collectAsState()
    val drawMode by viewModel.drawMode.collectAsState()
    val motionEvent by viewModel.motionEvent.collectAsState()

    DrawingApp(
        paddingValues = paddingValues,
        paths = paths,
        drawMode = drawMode,
        motionEvent = motionEvent,
        updateLine = viewModel::updateLine,
        clearRedo = viewModel::clearRedo,
        updateMotionEvent = viewModel::updateMotionEvent
    )
}

@Composable
fun DrawingApp(
    paddingValues: PaddingValues,
    paths: List<MyLine>,
    drawMode: DrawMode,
    motionEvent: MotionEvent,
    updateLine: (MyLine) -> Unit,
    clearRedo: () -> Unit = {},
    updateMotionEvent: (MotionEvent) -> Unit = {}
) {
    // Debug.
//    Log.d("qwer", "DrawMode : $drawMode, MotionEvent: $motionEvent")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        DrawingCanvas(
            columnScope = this,
            paths = paths,
            drawMode = drawMode,
            motionEvent = motionEvent,
            updateLine = updateLine,
            updateMotionEvent = updateMotionEvent,
            clearRedo = clearRedo,
            ifDebug = true
        )

        LayoutInfoMenus(
            modifier = Modifier
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                .shadow(1.dp, RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .background(Color.White)
                .padding(4.dp)
        )
    }
}
