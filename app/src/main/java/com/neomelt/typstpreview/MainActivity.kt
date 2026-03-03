package com.neomelt.typstpreview

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.neomelt.typstpreview.ui.components.PdfPageImage
import com.neomelt.typstpreview.ui.components.StatusBar
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

    LaunchedEffect(Unit) {
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

        typUri = uri
        typName = DocumentFile.fromSingleUri(context, uri)?.name
        expectedPdfName = typName?.substringBeforeLast(".")?.plus(".pdf")
        typContent = readText(context, uri)
        status = if (expectedPdfName != null) {
            "已导入 Typst: ${typName ?: "unknown"}，建议选择同名 PDF：$expectedPdfName"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Typst Android 预览器（本地预览 MVP）", style = MaterialTheme.typography.titleLarge)

        StatusBar(status)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickTyp.launch(arrayOf("text/*")) }) {
                Text("导入 .typ")
            }
            Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                Text(if (expectedPdfName == null) "导入 PDF" else "选择同名 PDF")
            }
            Button(onClick = { status = "编译功能占位：后续可接本地/远端 typst 编译" }) {
                Text("编译（占位）")
            }
        }

        Text("当前 typ：${typName ?: "未选择"}")
        Text("当前 pdf：${pdfName ?: "未选择"}")

        expectedPdfName?.let {
            Text("推荐文件名：$it")
        }

        Text("--- 文档目录（Typst 标题） ---")
        if (headings.isEmpty()) {
            Text("未解析到标题（提示：Typst 标题以 '=' 开头）")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(headings) { heading ->
                    val indent = (heading.level - 1).coerceAtLeast(0) * 12
                    Text(
                        text = "${" ".repeat(indent / 2)}• ${heading.title} (L${heading.lineNumber})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val target = ((heading.lineNumber - 1) * 28).coerceAtMost(typScrollState.maxValue)
                                scope.launch { typScrollState.animateScrollTo(target) }
                                status = "已跳转到第 ${heading.lineNumber} 行附近"
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        Text("--- Typst 源码 ---")
        SelectionContainer {
            Text(
                text = typContent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(typScrollState)
            )
        }

        Text("--- PDF 预览（当前页） ---")
        if (pdfUri != null && pdfPageCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { if (pdfPageIndex > 0) pdfPageIndex-- }, enabled = pdfPageIndex > 0) {
                    Text("上一页")
                }
                Button(
                    onClick = { if (pdfPageIndex < pdfPageCount - 1) pdfPageIndex++ },
                    enabled = pdfPageIndex < pdfPageCount - 1
                ) {
                    Text("下一页")
                }
                Button(onClick = {
                    val exported = exportCurrentPageAsPng(context, pdfUri!!, pdfPageIndex)
                    status = exported ?: "导出失败：请确认 PDF 可读"
                }) {
                    Text("导出当前页 PNG")
                }
                Text("第 ${pdfPageIndex + 1} / $pdfPageCount 页")
            }
            PdfPageImage(uri = pdfUri!!, pageIndex = pdfPageIndex, pageCount = pdfPageCount)
        } else {
            Text("尚未加载 PDF。先导入 .typ，再选择同名 PDF 进行预览。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                    Text(if (expectedPdfName == null) "现在导入 PDF" else "导入 $expectedPdfName")
                }
            }
        }
    }
}

