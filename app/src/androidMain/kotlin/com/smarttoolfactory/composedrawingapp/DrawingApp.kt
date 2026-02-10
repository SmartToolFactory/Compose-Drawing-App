package com.smarttoolfactory.composedrawingapp

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.composedrawingapp.gesture.MotionEvent
import com.smarttoolfactory.composedrawingapp.gesture.dragMotionEvent
import com.smarttoolfactory.composedrawingapp.model.DrawingStroke
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.ui.menu.DrawingPropertiesMenu
import com.smarttoolfactory.composedrawingapp.ui.theme.backgroundColor
import androidx.compose.runtime.snapshots.SnapshotStateList

@Composable
fun DrawingApp(
    paddingValues: PaddingValues,
    paths: SnapshotStateList<Pair<Path, PathProperties>>,
    pathsUndone: SnapshotStateList<Pair<Path, PathProperties>>,
    strokeList: SnapshotStateList<DrawingStroke>,
    strokeListUndone: SnapshotStateList<DrawingStroke>,
    currentPathPropertyState: MutableState<PathProperties>
) {

    val context = LocalContext.current

    var currentPathProperty by currentPathPropertyState
    val currentPoints = remember { mutableStateListOf<Offset>() }
    
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

        val drawModifier = Modifier
            .padding(8.dp)
            .shadow(1.dp)
            .fillMaxWidth()
            .weight(1f)
            .background(Color.White)
//            .background(getRandomColor())
            .dragMotionEvent(
                onDragStart = { pointerInputChange ->
                    motionEvent = MotionEvent.Down
                    currentPosition = pointerInputChange.position
                    if (drawMode != DrawMode.Touch) {
                         currentPoints.clear()
                         currentPoints.add(currentPosition)
                    }
                    pointerInputChange.consume()

                },
                onDrag = { pointerInputChange ->
                    motionEvent = MotionEvent.Move
                    currentPosition = pointerInputChange.position

                    if (drawMode == DrawMode.Touch) {
                        val change = pointerInputChange.positionChange()
                        println("DRAG: $change")
                        
                        // Translate all paths
                        paths.forEach { entry ->
                            val path: Path = entry.first
                            path.translate(change)
                        }
                        currentPath.translate(change)
                        
                        // Also translate all stroke points to keep them in sync
                        strokeList.forEachIndexed { index, stroke ->
                            val translatedPoints = stroke.points.map { point ->
                                Offset(point.x + change.x, point.y + change.y)
                            }
                            strokeList[index] = DrawingStroke(stroke.pathProperties, translatedPoints)
                        }
                    } else {
                        currentPoints.add(currentPosition)
                    }
                    pointerInputChange.consume()

                },
                onDragEnd = { pointerInputChange ->
                    motionEvent = MotionEvent.Up
                    pointerInputChange.consume()
                }
            )

        Canvas(modifier = drawModifier) {

            when (motionEvent) {

                MotionEvent.Down -> {
                    if (drawMode != DrawMode.Touch) {
                        currentPath.moveTo(currentPosition.x, currentPosition.y)
                    }

                    previousPosition = currentPosition

                }
                MotionEvent.Move -> {

                    if (drawMode != DrawMode.Touch) {
                        currentPath.quadraticBezierTo(
                            previousPosition.x,
                            previousPosition.y,
                            (previousPosition.x + currentPosition.x) / 2,
                            (previousPosition.y + currentPosition.y) / 2

                        )
                    }

                    previousPosition = currentPosition
                }

                MotionEvent.Up -> {
                    if (drawMode != DrawMode.Touch) {
                        currentPath.lineTo(currentPosition.x, currentPosition.y)

                        // Pointer is up save current path
//                        paths[currentPath] = currentPathProperty
                        paths.add(Pair(currentPath, currentPathProperty))

                        strokeList.add(DrawingStroke(currentPathProperty.copy(), currentPoints.toList()))

                        // Since paths are keys for map, use new one for each key
                        // and have separate path for each down-move-up gesture cycle
                        currentPath = Path()

                        // Create new instance of path properties to have new path and properties
                        // only for the one currently being drawn
                        currentPathProperty = PathProperties(
                            strokeWidth = currentPathProperty.strokeWidth,
                            color = currentPathProperty.color,
                            strokeCap = currentPathProperty.strokeCap,
                            strokeJoin = currentPathProperty.strokeJoin,
                            eraseMode = currentPathProperty.eraseMode
                        )
                    }

                    // Since new path is drawn no need to store paths to undone
                    pathsUndone.clear()
                    strokeListUndone.clear()

                    // If we leave this state at MotionEvent.Up it causes current path to draw
                    // line from (0,0) if this composable recomposes when draw mode is changed
                    currentPosition = Offset.Unspecified
                    previousPosition = currentPosition
                    motionEvent = MotionEvent.Idle
                }
                else -> Unit
            }

            with(drawContext.canvas.nativeCanvas) {

                val checkPoint = saveLayer(null, null)

                paths.forEach {

                    val path = it.first
                    val property = it.second

                    if (!property.eraseMode) {
                        drawPath(
                            color = property.color,
                            path = path,
                            style = Stroke(
                                width = property.strokeWidth,
                                cap = property.strokeCap,
                                join = property.strokeJoin
                            )
                        )
                    } else {

                        // Source
                        drawPath(
                            color = Color.Transparent,
                            path = path,
                            style = Stroke(
                                width = currentPathProperty.strokeWidth,
                                cap = currentPathProperty.strokeCap,
                                join = currentPathProperty.strokeJoin
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                }

                if (motionEvent != MotionEvent.Idle) {

                    if (!currentPathProperty.eraseMode) {
                        drawPath(
                            color = currentPathProperty.color,
                            path = currentPath,
                            style = Stroke(
                                width = currentPathProperty.strokeWidth,
                                cap = currentPathProperty.strokeCap,
                                join = currentPathProperty.strokeJoin
                            )
                        )
                    } else {
                        drawPath(
                            color = Color.Transparent,
                            path = currentPath,
                            style = Stroke(
                                width = currentPathProperty.strokeWidth,
                                cap = currentPathProperty.strokeCap,
                                join = currentPathProperty.strokeJoin
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
                restoreToCount(checkPoint)
            }

        }

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
                if (paths.isNotEmpty()) {

                    val lastItem = paths.last()
                    val lastPath = lastItem.first
                    val lastPathProperty = lastItem.second
                    paths.remove(lastItem)

                    pathsUndone.add(Pair(lastPath, lastPathProperty))

                    if (strokeList.isNotEmpty()) {
                        val lastStroke = strokeList.last()
                        strokeList.remove(lastStroke)
                        strokeListUndone.add(lastStroke)
                    }
                }
            },
            onRedo = {
                if (pathsUndone.isNotEmpty()) {

                    val lastPath = pathsUndone.last().first
                    val lastPathProperty = pathsUndone.last().second
                    if (pathsUndone.isNotEmpty()) {
                        pathsUndone.removeAt(pathsUndone.lastIndex)
                    }
                    paths.add(Pair(lastPath, lastPathProperty))

                    if (strokeListUndone.isNotEmpty()) {
                        val lastStroke = strokeListUndone.last()
                        strokeListUndone.remove(lastStroke)
                        strokeList.add(lastStroke)
                    }
                }
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


private fun DrawScope.drawText(text: String, x: Float, y: Float, paint: Paint) {

    val lines = text.split("\n")
    // 🔥🔥 There is not a built-in function as of 1.0.0
    // for drawing text so we get the native canvas to draw text and use a Paint object
    val nativeCanvas = drawContext.canvas.nativeCanvas

    lines.indices.withIndex().forEach { (posY, i) ->
        nativeCanvas.drawText(lines[i], x, posY * 40 + y, paint)
    }
}
