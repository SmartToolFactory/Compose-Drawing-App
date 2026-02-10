package com.smarttoolfactory.composedrawingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolfactory.composedrawingapp.model.SavedDrawing
import com.smarttoolfactory.composedrawingapp.ui.dialogs.DeleteDrawingDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedDrawingsScreen(
    savedDrawings: List<SavedDrawing>,
    onBack: () -> Unit,
    onDrawingClick: (SavedDrawing) -> Unit,
    onDeleteDrawing: (SavedDrawing) -> Unit
) {
    var drawingToDelete by remember { mutableStateOf<SavedDrawing?>(null) }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Your Saved Drawings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        if (savedDrawings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "There are no saved drawings",
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedDrawings) { drawing ->
                    SavedDrawingItem(
                        drawing = drawing,
                        onClick = { onDrawingClick(drawing) },
                        onDelete = { drawingToDelete = drawing }
                    )
                }
            }
        }
    }
    
    drawingToDelete?.let { drawing ->
        DeleteDrawingDialog(
            onDelete = {
                onDeleteDrawing(drawing)
                drawingToDelete = null
            },
            onCancel = {
                drawingToDelete = null
            }
        )
    }
}

@Composable
fun SavedDrawingItem(
    drawing: SavedDrawing,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drawing.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(drawing.dateCreated),
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete drawing",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
