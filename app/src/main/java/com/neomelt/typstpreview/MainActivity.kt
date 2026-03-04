package com.neomelt.typstpreview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.neomelt.typstpreview.ui.components.OutlinePanel
import com.neomelt.typstpreview.ui.components.PdfPageImage
import com.neomelt.typstpreview.ui.components.PdfPreviewPanel
import com.neomelt.typstpreview.ui.components.RenderModeToggle
import com.neomelt.typstpreview.ui.components.RenderedPreview
import com.neomelt.typstpreview.ui.components.SearchPanel
import com.neomelt.typstpreview.ui.components.SourceViewer
import com.neomelt.typstpreview.ui.components.StatusBar
import com.neomelt.typstpreview.ui.components.TopActionsBar
import com.neomelt.typstpreview.ui.components.TypstSetupDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TypstPreviewScreen()
                }
            }
        }
    }
}

@Composable
private fun TypstPreviewScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val typScrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE) }

    var typUri by remember { mutableStateOf<Uri?>(null) }
    var typContent by remember { mutableStateOf("还未导入 .typ 文件") }
    var typName by remember { mutableStateOf<String?>(null) }
    var expectedPdfName by remember { mutableStateOf<String?>(null) }

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf<String?>(null) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pdfPageIndex by remember { mutableIntStateOf(0) }

    var status by remember { mutableStateOf("提示：先导入 .typ，再导入对应 PDF 预览") }
    var compilerReady by remember { mutableStateOf(false) }
    var typstCommandPath by remember { mutableStateOf<String?>(null) }
    var typstDownloadUrl by remember { mutableStateOf("") }
    var setupDialogVisible by remember { mutableStateOf(false) }
    var setupStatus by remember { mutableStateOf("未检测") }
    var compiling by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var renderMode by remember { mutableStateOf(false) }
    var exportedImage by remember { mutableStateOf<ExportedImage?>(null) }

    LaunchedEffect(Unit) {
        typstCommandPath = prefs.getString(PREF_TYPST_CMD, null)
        typstDownloadUrl = prefs.getString(PREF_TYPST_URL, "") ?: ""

        val env = detectTypstEnvironment(typstCommandPath)
        compilerReady = env.available
        setupStatus = env.detail
        if (!compilerReady) {
            status = "未检测到 typst 命令：可继续手动导入 PDF 预览"
        }

        val savedTyp = prefs.getString(PREF_TYP_URI, null)
        val savedPdf = prefs.getString(PREF_PDF_URI, null)
        val savedPage = prefs.getInt(PREF_PDF_PAGE, 0)

        var typRestoreFailed = false
        var pdfRestoreFailed = false

        if (!savedTyp.isNullOrBlank()) {
            val uri = Uri.parse(savedTyp)
            if (canReadUri(context, uri)) {
                typUri = uri
                typName = DocumentFile.fromSingleUri(context, uri)?.name
                expectedPdfName = typName?.substringBeforeLast(".")?.plus(".pdf")
                typContent = readText(context, uri)
                status = "已恢复上次 Typst: ${typName ?: "unknown"}"
            } else {
                typRestoreFailed = true
                prefs.edit().remove(PREF_TYP_URI).apply()
            }
        }

        if (!savedPdf.isNullOrBlank()) {
            val uri = Uri.parse(savedPdf)
            val count = getPdfPageCount(context, uri)
            if (count > 0) {
                pdfUri = uri
                pdfName = DocumentFile.fromSingleUri(context, uri)?.name
                pdfPageCount = count
                pdfPageIndex = savedPage.coerceIn(0, count - 1)
                status = "已恢复上次 PDF，共 $pdfPageCount 页"
            } else {
                pdfRestoreFailed = true
                prefs.edit().remove(PREF_PDF_URI).remove(PREF_PDF_PAGE).apply()
            }
        }

        buildRestoreStatusMessage(typRestoreFailed, pdfRestoreFailed)?.let {
            status = it
        }
    }

    LaunchedEffect(typUri, pdfUri, pdfPageIndex) {
        prefs.edit()
            .putString(PREF_TYP_URI, typUri?.toString())
            .putString(PREF_PDF_URI, pdfUri?.toString())
            .putInt(PREF_PDF_PAGE, pdfPageIndex)
            .apply()
    }

    val pickTypstBinary = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        grantReadPermissionSafely(context, uri)

        val installResult = installTypstBinaryFromUri(context, uri)
        if (installResult.isSuccess) {
            typstCommandPath = installResult.getOrNull()
            prefs.edit().putString(PREF_TYPST_CMD, typstCommandPath).apply()
            scope.launch {
                val env = detectTypstEnvironment(typstCommandPath)
                compilerReady = env.available
                setupStatus = env.detail
                status = if (env.available) "Typst 环境配置成功" else "导入成功但环境检测失败"
            }
        } else {
            status = "导入 Typst 可执行文件失败：${installResult.exceptionOrNull()?.message}"
        }
    }

    val pickTyp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        grantReadPermissionSafely(context, uri)

        val selectedName = resolveDisplayName(context, uri)
        if (!isTypLikeFileName(selectedName)) {
            status = "导入失败：请选择 .typ/.txt/.md 文件（QQ 文件请先下载完成并复制到 Download）"
            return@rememberLauncherForActivityResult
        }

        val hadPdfLoaded = pdfUri != null

        typUri = uri
        typName = selectedName
        expectedPdfName = typName?.substringBeforeLast(".")?.plus(".pdf")
        typContent = readText(context, uri)
        currentMatchIndex = 0

        if (hadPdfLoaded) {
            pdfUri = null
            pdfName = null
            pdfPageCount = 0
            pdfPageIndex = 0
        }

        if (!compilerReady) {
            status = if (expectedPdfName != null) {
                "已导入 Typst: ${typName ?: "unknown"}。未检测到 typst 命令，请手动选择 PDF：$expectedPdfName"
            } else {
                "已导入 Typst: ${typName ?: "unknown"}"
            }
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            compiling = true
            status = "已导入 Typst，正在自动渲染 PDF..."
            val activeCompiler = LocalTypstCommandCompiler(typstCommandPath ?: "typst")
            when (val result = activeCompiler.compile(context, uri)) {
                is CompileResult.Success -> {
                    val compiledUri = loadCompiledPdf(result.outputPdfFile)
                    val pageCount = getPdfPageCount(context, compiledUri)
                    if (pageCount > 0 && canReadPdfUri(context, compiledUri)) {
                        pdfUri = compiledUri
                        pdfName = result.outputPdfFile.name
                        pdfPageCount = pageCount
                        pdfPageIndex = 0
                        status = "自动渲染成功，已加载 PDF（$pageCount 页）"
                    } else {
                        status = "自动渲染成功但加载 PDF 失败，请手动导入"
                    }
                }

                is CompileResult.Failure -> {
                    status = "自动渲染失败：${result.reason}"
                }
            }
            compiling = false
        }
    }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        grantReadPermissionSafely(context, uri)

        if (!isPdf(context, uri)) {
            status = "选择失败：不是 PDF 文件"
            return@rememberLauncherForActivityResult
        }

        pdfName = DocumentFile.fromSingleUri(context, uri)?.name
        val sameBase = hasSameBaseName(typName, pdfName)
        val pageCount = getPdfPageCount(context, uri)

        if (pageCount <= 0) {
            status = "PDF 加载失败：文件损坏或权限不足"
            pdfUri = null
            pdfPageCount = 0
            pdfPageIndex = 0
            return@rememberLauncherForActivityResult
        }

        pdfUri = uri
        pdfPageCount = pageCount
        pdfPageIndex = 0
        status = if (sameBase) {
            "已导入同名 PDF，共 $pdfPageCount 页"
        } else {
            val suggestion = expectedPdfName?.let { "，建议选择：$it" } ?: ""
            "已导入 PDF（与 typ 文件名不同），共 $pdfPageCount 页$suggestion"
        }
    }

    val headings = remember(typContent) { TypstOutlineParser.parse(typContent) }
    val renderBlocks = remember(typContent) { TypstRenderParser.parse(typContent) }
    val searchMatches = remember(typContent, searchQuery) {
        TypstSearch.findLineMatches(typContent, searchQuery)
    }

    LaunchedEffect(searchMatches) {
        if (searchMatches.isEmpty()) {
            currentMatchIndex = 0
        } else {
            currentMatchIndex = currentMatchIndex.coerceIn(0, searchMatches.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Typst Android 预览器（本地预览 MVP）", style = MaterialTheme.typography.titleLarge)
        StatusBar(status)

        TopActionsBar(
            hasExpectedPdfName = expectedPdfName != null,
            compiling = compiling,
            hasTypLoaded = typUri != null,
            compilerReady = compilerReady,
            onPickTyp = { pickTyp.launch(arrayOf("*/*")) },
            onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) },
            onSetup = { setupDialogVisible = true },
            onCompile = {
                val sourceUri = typUri
                if (!compilerReady) {
                    status = "当前设备未安装 typst 命令，无法本地编译"
                } else if (sourceUri == null) {
                    status = "请先导入 .typ 文件"
                } else {
                    scope.launch {
                        compiling = true
                        status = "正在编译 Typst..."
                        val activeCompiler = LocalTypstCommandCompiler(typstCommandPath ?: "typst")
                        when (val result = activeCompiler.compile(context, sourceUri)) {
                            is CompileResult.Success -> {
                                val compiledUri = loadCompiledPdf(result.outputPdfFile)
                                val pageCount = getPdfPageCount(context, compiledUri)
                                if (pageCount > 0 && canReadPdfUri(context, compiledUri)) {
                                    pdfUri = compiledUri
                                    pdfName = result.outputPdfFile.name
                                    pdfPageCount = pageCount
                                    pdfPageIndex = 0
                                    status = "编译成功，已加载生成的 PDF（$pageCount 页）"
                                } else {
                                    status = "编译成功但加载 PDF 失败，请手动导入"
                                }
                            }

                            is CompileResult.Failure -> {
                                status = result.reason
                            }
                        }
                        compiling = false
                    }
                }
            }
        )

        Text("当前 typ：${typName ?: "未选择"}")
        Text("当前 pdf：${pdfName ?: "未选择"}")
        expectedPdfName?.let { Text("推荐文件名：$it") }

        OutlinePanel(
            headings = headings,
            onHeadingClick = { heading ->
                val target = ((heading.lineNumber - 1) * 28).coerceAtMost(typScrollState.maxValue)
                scope.launch { typScrollState.animateScrollTo(target) }
                status = "已跳转到第 ${heading.lineNumber} 行附近"
            }
        )

        SearchPanel(
            query = searchQuery,
            matchCount = searchMatches.size,
            currentMatchIndex = currentMatchIndex,
            onQueryChange = {
                searchQuery = it
                currentMatchIndex = 0
            },
            onPrevMatch = {
                if (searchMatches.isNotEmpty()) {
                    currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else searchMatches.size - 1
                    val lineNumber = searchMatches[currentMatchIndex]
                    val target = ((lineNumber - 1) * 28).coerceAtMost(typScrollState.maxValue)
                    scope.launch { typScrollState.animateScrollTo(target) }
                    status = "搜索命中：第 $lineNumber 行"
                }
            },
            onNextMatch = {
                if (searchMatches.isNotEmpty()) {
                    currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                    val lineNumber = searchMatches[currentMatchIndex]
                    val target = ((lineNumber - 1) * 28).coerceAtMost(typScrollState.maxValue)
                    scope.launch { typScrollState.animateScrollTo(target) }
                    status = "搜索命中：第 $lineNumber 行"
                }
            }
        )

        RenderModeToggle(
            renderMode = renderMode,
            onSourceMode = { renderMode = false },
            onRenderMode = { renderMode = true }
        )

        if (renderMode) {
            RenderedPreview(blocks = renderBlocks, scrollState = typScrollState)
        } else {
            SourceViewer(content = typContent, scrollState = typScrollState)
        }

        PdfPreviewPanel(
            hasPdf = pdfUri != null && pdfPageCount > 0,
            expectedPdfName = expectedPdfName,
            pdfPageIndex = pdfPageIndex,
            pdfPageCount = pdfPageCount,
            onPrevPage = { if (pdfPageIndex > 0) pdfPageIndex-- },
            onNextPage = { if (pdfPageIndex < pdfPageCount - 1) pdfPageIndex++ },
            onExportCurrentPage = {
                val exported = exportCurrentPageAsPng(context, pdfUri!!, pdfPageIndex)
                if (exported != null) {
                    exportedImage = exported
                    status = "已导出到相册：${exported.displayName}"
                } else {
                    status = "导出失败：请确认 PDF 可读"
                }
            },
            onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) }
        ) {
            PdfPageImage(uri = pdfUri!!, pageIndex = pdfPageIndex, pageCount = pdfPageCount)
        }
    }

    if (setupDialogVisible) {
        TypstSetupDialog(
            statusText = setupStatus,
            abiText = preferredAbi(),
            downloadUrl = typstDownloadUrl,
            onDownloadUrlChange = {
                typstDownloadUrl = it
                prefs.edit().putString(PREF_TYPST_URL, it).apply()
            },
            onDismiss = { setupDialogVisible = false },
            onDetect = {
                scope.launch {
                    val env = detectTypstEnvironment(typstCommandPath)
                    compilerReady = env.available
                    setupStatus = env.detail
                    status = if (env.available) "Typst 环境检测通过" else "Typst 环境不可用"
                }
            },
            onAutoConfigure = {
                scope.launch {
                    val result = autoConfigureTypst(context, typstCommandPath)
                    compilerReady = result.available
                    setupStatus = result.detail
                    if (result.available && !result.command.isNullOrBlank()) {
                        typstCommandPath = result.command
                        prefs.edit().putString(PREF_TYPST_CMD, result.command).apply()
                        status = "Typst 自动配置完成"
                    } else {
                        status = result.detail
                    }
                }
            },
            onInstallFromUrl = {
                scope.launch {
                    val url = typstDownloadUrl.trim()
                    if (url.isBlank()) {
                        status = "请先填写下载链接"
                        return@launch
                    }
                    setupStatus = "正在下载并安装..."
                    val install = installTypstFromUrl(context, url)
                    if (install.isSuccess) {
                        typstCommandPath = install.getOrNull()
                        prefs.edit().putString(PREF_TYPST_CMD, typstCommandPath).apply()
                        val env = detectTypstEnvironment(typstCommandPath)
                        compilerReady = env.available
                        setupStatus = env.detail
                        status = if (env.available) "云端安装并配置成功" else "安装完成，但检测失败"
                    } else {
                        setupStatus = "云端安装失败"
                        status = "云端安装失败：${install.exceptionOrNull()?.message}"
                    }
                }
            },
            onImportBinary = { pickTypstBinary.launch(arrayOf("*/*")) },
            onClearConfig = {
                typstCommandPath = null
                prefs.edit().remove(PREF_TYPST_CMD).apply()
                compilerReady = false
                setupStatus = "已清除本地 Typst 配置"
                status = "已清除 Typst 配置，请重新导入可执行文件"
            }
        )
    }

    exportedImage?.let { image ->
        AlertDialog(
            onDismissRequest = { exportedImage = null },
            title = { Text("已导出到相册") },
            text = { Text("${image.displayName}\n现在打开相册查看吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(image.uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val launched = runCatching { context.startActivity(intent) }.isSuccess
                    if (!launched) {
                        status = "无法直接打开相册，请手动在系统相册查看"
                    }
                    exportedImage = null
                }) {
                    Text("打开")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedImage = null }) {
                    Text("暂不")
                }
            }
        )
    }
}
