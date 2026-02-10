package com.smarttoolfactory.composedrawingapp.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SaveDrawingDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onNameExists: () -> Unit,
    checkNameExists: (String) -> Boolean
) {
    var drawingName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Name your drawing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = drawingName,
                    onValueChange = { drawingName = it },
                    label = { Text("Drawing name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (checkNameExists(drawingName)) {
                                onNameExists()
                            } else {
                                onSave(drawingName)
                            }
                        },
                        enabled = drawingName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DrawingExistsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drawing with this name already exists") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Discard current drawing?") },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteDrawingDialog(
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete drawing?") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
