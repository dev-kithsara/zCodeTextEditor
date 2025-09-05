package com.example.codeeditor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeeditor.ui.theme.CodeEditorTheme
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.MoreVert



@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var fileManager: FileManager
    private var currentFileName by mutableStateOf("Untitled")
    private val editorState = TextEditorState()

    override fun onPause() {
        super.onPause()
        saveFile(currentFileName)
    }

    // File picker launcher for syntax JSON
    private var syntaxPickerCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register file picker launcher
        val syntaxPickerLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val rules = kotlinx.serialization.json.Json.decodeFromString<SyntaxRules>(json)
                    syntaxPickerCallback?.invoke(json)
                    editorState.syntaxRules = rules
                    editorState.language = "custom"
                } catch (e: Exception) {
                    // Show a user-friendly error (Toast)
                    android.widget.Toast.makeText(
                        this,
                        "Failed to load syntax JSON: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        setContent {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current
            var showMiniToolbar by remember { mutableStateOf(false) }
            var showFindReplace by remember { mutableStateOf(false) }
            var showCompilerInterface by remember { mutableStateOf(false) }
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            var compileOutput by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            // Initialize syntax rules and language
            if (editorState.syntaxRules == null) {
                editorState.syntaxRules = loadSyntaxRules(context, "kotlin.json")
                editorState.language = "kotlin"
            }

            // Auto-save + commit
            LaunchedEffect(editorState.textField.value) {
                snapshotFlow { editorState.textField.value }
                    .debounce(500)
                    .collect {
                        editorState.commitChange()
                        saveFile(currentFileName)
                    }
            }

            fileManager = FileManager(context)
            CodeEditorTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            initialFileName = currentFileName,
                            context = this,
                            onNewFile = { filename ->
                                createNewFile(filename)
                                currentFileName = filename
                                scope.launch { drawerState.close() }
                            },
                            onOpenFile = { filename ->
                                openFile(filename)
                                currentFileName = filename
                                scope.launch { drawerState.close() }
                            },
                            onSaveFile = { filename ->
                                saveFile(filename)
                                currentFileName = filename
                                scope.launch { drawerState.close() }
                            },
                            onLoadSyntax = {
                                syntaxPickerCallback = {
                                    // Already handled in launcher
                                }
                                syntaxPickerLauncher.launch("application/json")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                            .background(Color(0xFF121212)),
                        containerColor = Color(0xFF121212),
                        topBar = {
                            TopAppBar(
                                title = { Text("Project: $currentFileName", color = Color.White) },
                                actions = {
                                    // Edit button
                                    IconButton(onClick = { showMiniToolbar = !showMiniToolbar }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editing Option",
                                            tint = Color.White
                                        )
                                    }


                                    // 3-dot menu now only opens the sidebar
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "Menu"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    titleContentColor = Color.White,
                                    actionIconContentColor = Color.White
                                )
                            )
                        }
                        ,
                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    IconButton(onClick = { editorState.undo() }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.undo),
                                            contentDescription = "Undo"
                                        )
                                    }
                                    IconButton(onClick = { editorState.redo() }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.redo),
                                            contentDescription = "Redo"
                                        )
                                    }
                                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Find"
                                        )
                                    }
                                    IconButton(onClick = {
                                        compileCode(
                                            context,
                                            editorState.textField.value.text,
                                            fileManager,
                                            currentFileName
                                        ) { output ->
                                            compileOutput = output
                                            showCompilerInterface = true
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBox,
                                            contentDescription = "Compile"
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            if (showCompilerInterface) {
                                CompilerInterface(
                                    clipboardManager,
                                    compileOutput,
                                    onClose = { showCompilerInterface = false }
                                )
                            }

                            if (showFindReplace) {
                                FindReplaceBar(editorState = editorState, onClose = {
                                    showFindReplace = false
                                })
                            }

                            if (showMiniToolbar) {
                                MiniToolbar(
                                    onCut = {
                                        cutText(
                                            editorState.textField.value,
                                            { editorState.onTextChange(it) },
                                            clipboardManager
                                        )
                                    },
                                    onCopy = { copyText(editorState.textField.value, clipboardManager) },
                                    onPaste = {
                                        pasteText(
                                            editorState.textField.value,
                                            { editorState.onTextChange(it) },
                                            clipboardManager
                                        )
                                    }
                                )
                            }

                            CodeEditor(
                                modifier = Modifier.weight(1f),
                                editorState = editorState,
                                syntaxRules = editorState.syntaxRules ?: loadSyntaxRules(context, "kotlin.json"),
                                language = editorState.language
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNewFile(filename: String) {
        val file = fileManager.createNewFile(filename)
        currentFileName = file
        editorState.textField.value = TextFieldValue("")
    }

    private fun saveFile(filename: String) {
        fileManager.saveFile(filename, editorState.textField.value.text)
    }

    private fun openFile(filename: String) {
        val content = fileManager.openFile(filename)
        editorState.textField.value = TextFieldValue(content)
        currentFileName = filename
    }
}

@Composable
fun CodeEditor(
    modifier: Modifier,
    editorState: TextEditorState,
    syntaxRules: SyntaxRules,
    language: String = "kotlin"
) {
    val editorText = editorState.textField.value
    val scrollState = rememberScrollState()
    val lines = editorText.text.split("\n").ifEmpty { listOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // ✅ theme background
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        Row {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .padding(end = 4.dp)
            ) {
                lines.forEachIndexed { i, _ ->
                    Text(
                        text = "${i + 1}.",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .height(24.dp)
                            .padding(vertical = 2.dp)
                    )
                }
            }
            BasicTextField(
                value = editorText,
                onValueChange = { editorState.onTextChange(it) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Transparent,
                    lineHeight = 24.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                decorationBox = { innerTextField ->
                    Box {
                        Text(
                            text = highlightSyntax(editorText.text, syntaxRules, language),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        if (editorText.text.isEmpty()) {
                            Text(
                                "Type here...",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

fun highlightSyntax(text: String, rules: SyntaxRules, language: String = "kotlin"): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (language == "kotlin") {
            // Native Kotlin rules
            val keywords = listOf(
                "fun", "class", "object", "interface", "enum", "val", "var", "const", "typealias",
                "if", "else", "when", "for", "while", "do", "break", "continue", "return",
                "try", "catch", "finally", "throw", "is", "in", "as", "this", "super", "null", "true", "false",
                "package", "import", "override", "open", "abstract", "final", "companion", "constructor", "init",
                "lateinit", "by", "where", "println"
            )
            // Keywords (blue)
            keywords.forEach { keyword ->
                "\\b$keyword\\b".toRegex().findAll(text).forEach { match ->
                    addStyle(
                        SpanStyle(color = Color(0xFF569CD6)),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
            // Comments (green)
            Regex("//.*|/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = Color(0xFF6A9955)),
                    match.range.first,
                    match.range.last + 1
                )
            }
            // Strings (orange)
            val stringRegex = Regex("\".*?\"|'.*?'")
            stringRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = Color(0xFFD69D85)),
                    match.range.first,
                    match.range.last + 1
                )
            }
        } else {
            // Use rules from JSON for other languages
            // Keywords (blue)
            rules.keywords.forEach { keyword ->
                "\\b$keyword\\b".toRegex().findAll(text).forEach { match ->
                    addStyle(
                        SpanStyle(color = Color(0xFF569CD6)),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
            // Comments (green)
            rules.comments.forEach { comment ->
                Regex("${Regex.escape(comment)}.*").findAll(text).forEach { match ->
                    addStyle(
                        SpanStyle(color = Color(0xFF6A9955)),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
            // Strings (orange)
            val stringRegex = Regex("\".*?\"|'.*?'")
            stringRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = Color(0xFFD69D85)),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}
