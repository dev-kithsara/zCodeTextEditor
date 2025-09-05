package com.example.codeeditor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.io.OutputStreamWriter

fun compileCode(
    context: Context,
    code: String,
    fileManager: FileManager,
    fileName: String,
    onResult: (String) -> Unit
) {
    // Send code and fileName as JSON to Flask server via HTTP POST and get output
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("http://127.0.0.1:8081/compile")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            // Build JSON payload
            val json = JSONObject()
            json.put("fileName", fileName)
            json.put("code", code)

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()) }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // Parse JSON and show only output or errors
            val displayResult = try {
                val json = org.json.JSONObject(response)
                if (json.optBoolean("ok", false)) {
                    json.optString("output", "")
                } else {
                    val errors = json.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until errors.length()) {
                            val err = errors.getJSONObject(i)
                            val line = err.optInt("line", 0)
                            val msg = err.optString("message", "")
                            sb.append("Line $line: $msg\n")
                        }
                        sb.toString()
                    } else {
                        "Unknown error."
                    }
                }
            } catch (e: Exception) {
                response // fallback: show raw response
            }
            withContext(Dispatchers.Main) {
                onResult(displayResult)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Error: ${e.message}")
            }
        }
    }
}
