package com.smarttoolfactory.composedrawingapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.smarttoolfactory.composedrawingapp.data.DrawingRepository
import com.smarttoolfactory.composedrawingapp.model.DrawingStroke
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.model.SavedDrawing
import com.smarttoolfactory.composedrawingapp.ui.theme.ComposeDrawingAppTheme
import com.smarttoolfactory.composedrawingapp.ui.dialogs.*
import com.smarttoolfactory.composedrawingapp.ui.screens.SavedDrawingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeDrawingAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val systemUiController = rememberSystemUiController()

    DisposableEffect(systemUiController) {
        systemUiController.isStatusBarVisible = true
        systemUiController.isNavigationBarVisible = true
        onDispose {}
    }

    val context = LocalContext.current
    val repository = remember { DrawingRepository(context) }
    val scope = rememberCoroutineScope()
    
    val paths = remember { mutableStateListOf<Pair<Path, PathProperties>>() }
    val pathsUndone = remember { mutableStateListOf<Pair<Path, PathProperties>>() }
    val strokeList = remember { mutableStateListOf<DrawingStroke>() }
    val strokeListUndone = remember { mutableStateListOf<DrawingStroke>() }
    val currentPathPropertyState = remember { mutableStateOf(PathProperties()) }
    
    // Navigation and drawing state
    var showSavedDrawingsScreen by remember { mutableStateOf(false) }
    var currentDrawingName by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var savedDrawings by remember { mutableStateOf<List<SavedDrawing>>(emptyList()) }
    var initialStrokeCount by remember { mutableStateOf(0) }
    
    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDrawingExistsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Restore
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val savedStrokes = repository.getStrokes()
            val (savedPref, savedDrawingName) = repository.getPreference()
            withContext(Dispatchers.Main) {
                 if (savedPref != null) {
                     currentPathPropertyState.value = savedPref
                 }
                 currentDrawingName = savedDrawingName
                 if (savedStrokes.isNotEmpty()) {
                     strokeList.clear()
                     strokeList.addAll(savedStrokes)
                     paths.clear()
                     savedStrokes.forEach { stroke ->
                         val p = Path()
                         if (stroke.points.isNotEmpty()) {
                             p.moveTo(stroke.points[0].x, stroke.points[0].y)
                             if (stroke.points.size > 1) {
                                 for (i in 1 until stroke.points.size) {
                                     p.lineTo(stroke.points[i].x, stroke.points[i].y)
                                 }
                             }
                         }
                         paths.add(Pair(p, stroke.pathProperties))
                     }
                 }
                 initialStrokeCount = strokeList.size
            }
        }
    }
    
    // Track changes - only when stroke count differs from initial/saved state
    LaunchedEffect(strokeList.size) {
        if (strokeList.size != initialStrokeCount) {
            hasUnsavedChanges = true
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val strokes = strokeList.toList()
                val pref = currentPathPropertyState.value
                val drawingName = currentDrawingName
                scope.launch(Dispatchers.IO) {
                    repository.saveStrokes(strokes)
                    repository.savePreference(pref, drawingName)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Helper functions
    fun loadDrawing(drawing: SavedDrawing) {
        scope.launch(Dispatchers.IO) {
            val strokes = repository.loadDrawing(drawing.id)
            withContext(Dispatchers.Main) {
                currentDrawingName = drawing.name
                hasUnsavedChanges = false
                strokeList.clear()
                strokeList.addAll(strokes)
                paths.clear()
                strokes.forEach { stroke ->
                    val p = Path()
                    if (stroke.points.isNotEmpty()) {
                        p.moveTo(stroke.points[0].x, stroke.points[0].y)
                        if (stroke.points.size > 1) {
                            for (i in 1 until stroke.points.size) {
                                p.lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
                    }
                    paths.add(Pair(p, stroke.pathProperties))
                }
                initialStrokeCount = strokeList.size
                showSavedDrawingsScreen = false
            }
        }
    }
    
    fun saveCurrentDrawing(name: String) {
        scope.launch(Dispatchers.IO) {
            val success = repository.saveDrawing(name, strokeList.toList())
            withContext(Dispatchers.Main) {
                if (success) {
                    currentDrawingName = name
                    hasUnsavedChanges = false
                    initialStrokeCount = strokeList.size
                    Toast.makeText(context, "Drawing saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error saving drawing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun clearCanvas() {
        paths.clear()
        pathsUndone.clear()
        strokeList.clear()
        strokeListUndone.clear()
        currentDrawingName = null
        hasUnsavedChanges = false
        initialStrokeCount = 0
    }

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        if (showSavedDrawingsScreen) {
            SavedDrawingsScreen(
                savedDrawings = savedDrawings,
                onBack = { showSavedDrawingsScreen = false },
                onDrawingClick = { drawing -> loadDrawing(drawing) },
                onDeleteDrawing = { drawing ->
                    scope.launch(Dispatchers.IO) {
                        repository.deleteDrawing(drawing.id)
                        savedDrawings = repository.getAllSavedDrawings()
                    }
                }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                topBar = {
                    DrawingAppBar(
                        drawingName = currentDrawingName,
                        hasUnsavedChanges = hasUnsavedChanges,
                        onSave = {
                            if (currentDrawingName != null) {
                                // Update existing drawing
                                saveCurrentDrawing(currentDrawingName!!)
                            } else {
                                // Show save dialog for new drawing
                                showSaveDialog = true
                            }
                        },
                        onOpen = {
                            scope.launch(Dispatchers.IO) {
                                savedDrawings = repository.getAllSavedDrawings()
                                withContext(Dispatchers.Main) {
                                    if (hasUnsavedChanges) {
                                        showDiscardDialog = true
                                    } else {
                                        showSavedDrawingsScreen = true
                                    }
                                }
                            }
                        },
                        onExport = {
                            saveBitmapWithBounds(context, paths.toList())
                        },
                        onClear = {
                            clearCanvas()
                        }
                    )
                }
            ) { paddingValues: PaddingValues ->
                DrawingApp(
                    paddingValues,
                    paths,
                    pathsUndone,
                    strokeList,
                    strokeListUndone,
                    currentPathPropertyState
                )
            }
        }
    }
    
    // Dialogs
    if (showSaveDialog) {
        SaveDrawingDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                saveCurrentDrawing(name)
                showSaveDialog = false
            },
            onNameExists = {
                showSaveDialog = false
                showDrawingExistsDialog = true
            },
            checkNameExists = { name ->
                repository.drawingNameExists(name)
            }
        )
    }
    
    if (showDrawingExistsDialog) {
        DrawingExistsDialog(
            onDismiss = {
                showDrawingExistsDialog = false
                showSaveDialog = true
            }
        )
    }
    
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = {
                showDiscardDialog = false
                showSavedDrawingsScreen = true
            },
            onCancel = {
                showDiscardDialog = false
            }
        )
    }
}

@Composable
fun DrawingAppBar(
    drawingName: String?,
    hasUnsavedChanges: Boolean,
    onSave: () -> Unit = {},
    onOpen: () -> Unit = {},
    onExport: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    var showClearDialog by remember { mutableStateOf(false) }
    
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Clear canvas and start a new drawing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClear()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    TopAppBar(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        title = {
            Column {
                Text(
                    text = "DrawIt",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onPrimary
                )
                Text(
                    text = when {
                        drawingName != null && hasUnsavedChanges -> "$drawingName *"
                        drawingName != null -> drawingName
                        else -> "*Untitled*"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        },
        actions = {
           IconButton(onClick = onSave) {
               Icon(Icons.Filled.Save, contentDescription = "Save")
           }
           IconButton(onClick = onOpen) {
               Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
           }
           IconButton(onClick = { showClearDialog = true }) {
               Icon(Icons.Filled.Add, contentDescription = "New Drawing")
           }
           IconButton(onClick = onExport) {
               Icon(Icons.Filled.Share, contentDescription = "Export PNG")
           }
        }
    )
}

/**
 * Calculate the bounds of all paths to capture the entire drawing
 */
fun calculateDrawingBounds(paths: List<Pair<Path, PathProperties>>): Rect {
    if (paths.isEmpty()) {
        return Rect(0f, 0f, 100f, 100f) // Default small size if no paths
    }
    
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    paths.forEach { (path, props) ->
        val androidPath = path.asAndroidPath()
        val bounds = android.graphics.RectF()
        androidPath.computeBounds(bounds, true)
        
        // Account for stroke width to ensure edges aren't cut off
        val strokePadding = props.strokeWidth / 2f
        
        minX = min(minX, bounds.left - strokePadding)
        minY = min(minY, bounds.top - strokePadding)
        maxX = max(maxX, bounds.right + strokePadding)
        maxY = max(maxY, bounds.bottom + strokePadding)
    }
    
    // Add some padding around the drawing
    val padding = 20f
    return Rect(
        minX - padding,
        minY - padding,
        maxX + padding,
        maxY + padding
    )
}

/**
 * Save the entire drawing by calculating its bounds and exporting only the drawn area
 */
fun saveBitmapWithBounds(context: Context, paths: List<Pair<Path, PathProperties>>) {
    try {
        if (paths.isEmpty()) {
            Toast.makeText(context, "Nothing to export - canvas is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Calculate the actual bounds of the drawing
        val bounds = calculateDrawingBounds(paths)
        val drawingWidth = (bounds.width).toInt().coerceAtLeast(100)
        val drawingHeight = (bounds.height).toInt().coerceAtLeast(100)
        
        val bitmap = Bitmap.createBitmap(drawingWidth, drawingHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Translate the canvas so the drawing starts at (0,0) in the bitmap
        canvas.translate(-bounds.left, -bounds.top)
        
        paths.forEach { (path, props) ->
            val paint = android.graphics.Paint().apply {
                color = props.color.toArgb()
                strokeWidth = props.strokeWidth
                alpha = (props.alpha * 255).toInt()
                style = android.graphics.Paint.Style.STROKE
                strokeCap = when(props.strokeCap) { 
                    StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
                    StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
                    else -> android.graphics.Paint.Cap.SQUARE
                }
                strokeJoin = when(props.strokeJoin) {
                    StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
                    StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
                    else -> android.graphics.Paint.Join.BEVEL
                }
                isAntiAlias = true
            }
            if (props.eraseMode) {
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            }
            canvas.drawPath(path.asAndroidPath(), paint)
        }

        val filename = "Draw_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved ${filename} (${drawingWidth}x${drawingHeight}px) to Pictures", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveBitmap(context: Context, paths: List<Pair<Path, PathProperties>>, width: Int, height: Int) {
    try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        paths.forEach { (path, props) ->
             val paint = android.graphics.Paint().apply {
                 color = props.color.toArgb()
                 strokeWidth = props.strokeWidth
                 alpha = (props.alpha * 255).toInt()
                 style = android.graphics.Paint.Style.STROKE
                 strokeCap = when(props.strokeCap) { 
                     StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
                     StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
                     else -> android.graphics.Paint.Cap.SQUARE
                 }
                 strokeJoin = when(props.strokeJoin) {
                     StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
                     StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
                     else -> android.graphics.Paint.Join.BEVEL
                 }
                 isAntiAlias = true
             }
             if (props.eraseMode) {
                 paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
             }
             canvas.drawPath(path.asAndroidPath(), paint)
        }

        val filename = "Draw_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved ${filename} to Pictures", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeDrawingAppTheme {
        MainScreen()
    }
}
