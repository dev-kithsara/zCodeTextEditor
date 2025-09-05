package com.example.codeeditor

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString


object DevCppThemeColors {
    val keyword = Color(0xFF0000FF)   // Blue
    val comment = Color(0xFF008000)   // Green
    val string = Color(0xFF800000)    // Maroon
    val number = Color(0xFF008080)    // Teal
    val text = Color(0xFF000000)      // Black
    val background = Color(0xFFFFFFFF) // White
}

@Composable
fun CompilerInterface(
    clipboardManager: ClipboardManager,
    compileOutput: String,
    onClose: () -> Unit
) {
    // Apply Dev-C++ style to output text
    val highlightedOutput = buildAnnotatedString {
        // 👉 Simple demo logic: highlight "error" in red, numbers in teal, strings in maroon
        val words = compileOutput.split(" ")
        for (word in words) {
            when {
                word.contains("error", ignoreCase = true) -> {
                    pushStyle(SpanStyle(color = Color.Red))
                    append("$word ")
                    pop()
                }
                word.all { it.isDigit() } -> {
                    pushStyle(SpanStyle(color = DevCppThemeColors.number))
                    append("$word ")
                    pop()
                }
                word.startsWith("\"") && word.endsWith("\"") -> {
                    pushStyle(SpanStyle(color = DevCppThemeColors.string))
                    append("$word ")
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = DevCppThemeColors.text))
                    append("$word ")
                    pop()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onClose() },
        title = { Text("Compiler Result", color = DevCppThemeColors.keyword) },
        text = { Text(highlightedOutput) },
        confirmButton = {
            Row {
                Button(onClick = {
                    val pathOnly = compileOutput.substringAfter("Kotlin file saved at ").trim()
                    if (pathOnly.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(pathOnly))
                    }
                }) {
                    Text("Copy")
                }

                Button(onClick = { onClose() }) {
                    Text("OK")
                }
            }
        },
        containerColor = DevCppThemeColors.background
    )
}