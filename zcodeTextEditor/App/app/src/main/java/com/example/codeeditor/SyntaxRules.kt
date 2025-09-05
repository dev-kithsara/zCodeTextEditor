package com.example.codeeditor

import android.content.Context
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class SyntaxRules(
    val keywords: List<String>,
    val comments: List<String>,
    val strings: List<String>
)

fun loadSyntaxRules(context: Context, filename: String): SyntaxRules {
    val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
    return Json.decodeFromString<SyntaxRules>(jsonString)
}
