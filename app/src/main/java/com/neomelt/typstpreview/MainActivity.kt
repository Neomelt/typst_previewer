package com.neomelt.typstpreview

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.neomelt.typstpreview.ui.components.SearchPanel
import com.neomelt.typstpreview.ui.components.SourceViewer
import com.neomelt.typstpreview.ui.components.StatusBar
import com.neomelt.typstpreview.ui.components.TopActionsBar
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
    val compiler = remember { LocalTypstCommandCompiler() }

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
    var compiling by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        compilerReady = compiler.isAvailable()
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

        status = if (expectedPdfName != null) {
            if (hadPdfLoaded) {
                "已导入 Typst: ${typName ?: "unknown"}，已清空旧 PDF，请选择同名文件：$expectedPdfName"
            } else {
                "已导入 Typst: ${typName ?: "unknown"}，建议选择同名 PDF：$expectedPdfName"
            }
        } else {
            "已导入 Typst: ${typName ?: "unknown"}"
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
                        when (val result = compiler.compile(context, sourceUri)) {
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

        SourceViewer(content = typContent, scrollState = typScrollState)

        PdfPreviewPanel(
            hasPdf = pdfUri != null && pdfPageCount > 0,
            expectedPdfName = expectedPdfName,
            pdfPageIndex = pdfPageIndex,
            pdfPageCount = pdfPageCount,
            onPrevPage = { if (pdfPageIndex > 0) pdfPageIndex-- },
            onNextPage = { if (pdfPageIndex < pdfPageCount - 1) pdfPageIndex++ },
            onExportCurrentPage = {
                val exported = exportCurrentPageAsPng(context, pdfUri!!, pdfPageIndex)
                status = exported ?: "导出失败：请确认 PDF 可读"
            },
            onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) }
        ) {
            PdfPageImage(uri = pdfUri!!, pageIndex = pdfPageIndex, pageCount = pdfPageCount)
        }
    }
}
