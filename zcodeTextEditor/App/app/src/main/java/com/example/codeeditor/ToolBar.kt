package com.example.codeeditor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
/**
 * MiniToolbar Composable
 *
 * A simple toolbar UI that provides buttons for text editing actions:
 * - Cut
 * - Copy
 * - Paste
 *
 * The actual behavior is passed in through the callbacks [onCut], [onCopy], and [onPaste].
 */
@Composable
fun MiniToolbar(
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit
) {
    val editorState = remember { TextEditorState() }
    Log.d("FileManager", "Mini tool bar")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(8.dp),
    ) {
        TextButton(onClick = onCut) { Text("Cut") }
        TextButton(onClick = onCopy) { Text("Copy") }
        TextButton(onClick = onPaste) { Text("Paste") }
    }
}