package com.example.codeeditor

import android.content.Context
import android.util.Log
import java.io.File

class FileManager(private val context: Context) {

    // Create a new file (if not exists) and return its name
    fun createNewFile(fileName: String): String {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file.name
    }

    // Save text content to a file (creates it if missing)
    fun saveFile(fileName: String, content: String) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(content)
        Log.d("FileManager", "Saved to ${file.absolutePath}")
    }

    // Open a file and return its content (empty if not exists)
    fun openFile(fileName: String): String {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readText() else ""
    }
}
