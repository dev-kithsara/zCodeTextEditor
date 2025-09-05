package com.example.codeeditor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    initialFileName: String,
    context: Context,
    onNewFile: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onSaveFile: (String) -> Unit,
    onLoadSyntax: () -> Unit
    ) {
    var fileName by remember { mutableStateOf(initialFileName) }
    var showDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    val extensions = listOf(".kt", ".txt", ".java")
    var selectedExtension by remember { mutableStateOf(extensions.first()) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.6f)
            .background(MaterialTheme.colorScheme.surface) // dark theme bg
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        TextButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("New File", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        TextButton(onClick = { showOpenDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Open", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        TextButton(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Save", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        TextButton(onClick = { onLoadSyntax() }, modifier = Modifier.fillMaxWidth()) {
            Text("Load syntax JSON", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    // New File Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = if (fileName.endsWith(selectedExtension)) {
                        fileName
                    } else {
                        fileName + selectedExtension
                    }
                    onNewFile(finalName)
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            title = { Text("Create New File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        trailingIcon = { Text(selectedExtension) }
                    )
                    Button(onClick = { expanded = !expanded }) {
                        Text("Select extension")
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Arrow Down"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        extensions.forEach { ext ->
                            DropdownMenuItem(
                                text = { Text(ext) },
                                onClick = {
                                    selectedExtension = ext
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    // Save File Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = if (fileName.endsWith(selectedExtension)) {
                        fileName
                    } else {
                        fileName + selectedExtension
                    }
                    onSaveFile(finalName)
                    showSaveDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
            title = { Text("Save the file") }
        )
    }

    // Open File Dialog
    if (showOpenDialog) {
        val files = context.filesDir.listFiles()?.toList() ?: emptyList()
        AlertDialog(
            onDismissRequest = { showOpenDialog = false },
            title = { Text("Select a file") },
            text = {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(files.size) { index ->
                        val file = files[index]
                        Text(
                            text = file.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    onOpenFile(file.name)
                                    showOpenDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
