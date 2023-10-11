package com.smarttoolfactory.composedrawingapp

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.composedrawingapp.gesture.MotionEvent
import com.smarttoolfactory.composedrawingapp.model.MyLine
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.ui.canvas.DrawingCanvas
import com.smarttoolfactory.composedrawingapp.ui.menu.DrawingPropertiesMenu
import com.smarttoolfactory.composedrawingapp.ui.theme.backgroundColor
import com.smarttoolfactory.composedrawingapp.viewmodel.CanvasViewModel

@Composable
fun DrawingApp(
    paddingValues: PaddingValues,
    viewModel: CanvasViewModel
) {
    val paths by viewModel.lineList.collectAsState()

    DrawingApp(
        paddingValues = paddingValues,
        paths = paths,
        updateLine = viewModel::updateLine,
        undo = viewModel::undo,
        redo = viewModel::redo,
        clearRedo = viewModel::clearRedo
    )
}

@Composable
fun DrawingApp(
    paddingValues: PaddingValues,
    paths: List<MyLine>,
    updateLine: (MyLine) -> Unit,
    undo: () -> Unit,
    redo: () -> Unit,
    clearRedo: () -> Unit = {}
) {

    val context = LocalContext.current

    /**
     * Paths that are added, this is required to have paths with different options and paths
     *  ith erase to keep over each other
     */
//    val paths = remember { mutableStateListOf<Pair<Path, PathProperties>>() }
//    val paths by viewModel.lineList.collectAsState()

    /**
     * Paths that are undone via button. These paths are restored if user pushes
     * redo button if there is no new path drawn.
     *
     * If new path is drawn after this list is cleared to not break paths after undoing previous
     * ones.
     */
    val pathsUndone = remember { mutableStateListOf<Pair<Path, PathProperties>>() }

    /**
     * Canvas touch state. [MotionEvent.Idle] by default, [MotionEvent.Down] at first contact,
     * [MotionEvent.Move] while dragging and [MotionEvent.Up] when first pointer is up
     */
    var motionEvent by remember { mutableStateOf(MotionEvent.Idle) }

    /**
     * Current position of the pointer that is pressed or being moved
     */
    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }

    /**
     * Previous motion event before next touch is saved into this current position.
     */
    var previousPosition by remember { mutableStateOf(Offset.Unspecified) }

    /**
     * Draw mode, erase mode or touch mode to
     */
    var drawMode by remember { mutableStateOf(DrawMode.Draw) }

    /**
     * Path that is being drawn between [MotionEvent.Down] and [MotionEvent.Up]. When
     * pointer is up this path is saved to **paths** and new instance is created
     */
    var currentPath by remember { mutableStateOf(Path()) }

    /**
     * Properties of path that is currently being drawn between
     * [MotionEvent.Down] and [MotionEvent.Up].
     */
    var currentPathProperty by remember { mutableStateOf(PathProperties()) }

    val canvasText = remember { StringBuilder() }
    val paint = remember {
        Paint().apply {
            textSize = 40f
            color = Color.Black.toArgb()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        DrawingCanvas(columnScope = this,
            paths = paths,
            updateLine = updateLine,
            clearRedo = clearRedo,
            ifDebug = true
        )

        DrawingPropertiesMenu(
            modifier = Modifier
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                .shadow(1.dp, RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .background(Color.White)
                .padding(4.dp),
            pathProperties = currentPathProperty,
            drawMode = drawMode,
            onUndo = {
                undo.invoke()
            },
            onRedo = {
                redo.invoke()
            },
            onPathPropertiesChange = {
                motionEvent = MotionEvent.Idle
            },
            onDrawModeChanged = {
                motionEvent = MotionEvent.Idle
                drawMode = it
                currentPathProperty.eraseMode = (drawMode == DrawMode.Erase)
                Toast.makeText(
                    context, "pathProperty: ${currentPathProperty.hashCode()}, " +
                            "Erase Mode: ${currentPathProperty.eraseMode}", Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
