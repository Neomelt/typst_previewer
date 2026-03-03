package com.neomelt.typstpreview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

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
    var typContent by remember { mutableStateOf("还未导入 .typ 文件") }
    var typName by remember { mutableStateOf<String?>(null) }
    var expectedPdfName by remember { mutableStateOf<String?>(null) }
    var pdfName by remember { mutableStateOf<String?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pdfPageIndex by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("提示：先导入 .typ，再导入对应 PDF 预览") }

    val pickTyp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        grantReadPermissionSafely(context, uri)
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
        Text(status)

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
                            .clickable { status = "定位提示：标题 ${heading.title} 在第 ${heading.lineNumber} 行" }
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
                    .verticalScroll(rememberScrollState())
            )
        }

        Text("--- PDF 预览（当前页） ---")
        if (pdfUri != null && pdfPageCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { if (pdfPageIndex > 0) pdfPageIndex-- }) { Text("上一页") }
                Button(onClick = { if (pdfPageIndex < pdfPageCount - 1) pdfPageIndex++ }) { Text("下一页") }
                Text("第 ${pdfPageIndex + 1} / $pdfPageCount 页")
            }
            PdfPageImage(uri = pdfUri!!, pageIndex = pdfPageIndex)
        } else {
            Text("尚未加载 PDF")
        }
    }
}

@Composable
private fun PdfPageImage(uri: Uri, pageIndex: Int) {
    val context = LocalContext.current
    val result = remember(uri, pageIndex) {
        renderPdfPage(context, uri, pageIndex)
    }

    when (result) {
        is PdfRenderResult.Success -> {
            Image(
                bitmap = result.bitmap.asImageBitmap(),
                contentDescription = "PDF page",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        is PdfRenderResult.Error -> {
            Text("PDF 渲染失败：${result.reason.message}")
        }
    }
}

private fun readText(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: "读取失败"
    } catch (e: Exception) {
        "读取失败: ${e.message}"
    }
}

private fun hasSameBaseName(typName: String?, pdfName: String?): Boolean {
    if (typName.isNullOrBlank() || pdfName.isNullOrBlank()) return false
    return typName.substringBeforeLast(".") == pdfName.substringBeforeLast(".")
}

private fun grantReadPermissionSafely(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {
        // ignore non-persistable permission cases
    }
}

private fun isPdf(context: android.content.Context, uri: Uri): Boolean {
    val type = context.contentResolver.getType(uri)
    if (type == "application/pdf") return true
    val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    return ext.equals("pdf", true)
}

private fun getPdfPageCount(context: android.content.Context, uri: Uri): Int {
    return try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        PdfRenderer(pfd).use { it.pageCount }
    } catch (_: Exception) {
        0
    }
}

private sealed interface PdfRenderResult {
    data class Success(val bitmap: Bitmap) : PdfRenderResult
    data class Error(val reason: PdfRenderError) : PdfRenderResult
}

private enum class PdfRenderError(val message: String) {
    OPEN_FAILED("无法打开文件（可能无权限）"),
    PAGE_OUT_OF_RANGE("页码超出范围"),
    FILE_CORRUPTED("文件可能损坏"),
    UNKNOWN("未知错误")
}

private fun renderPdfPage(context: android.content.Context, uri: Uri, pageIndex: Int): PdfRenderResult {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return PdfRenderResult.Error(PdfRenderError.OPEN_FAILED)
        renderer = PdfRenderer(pfd)

        if (pageIndex !in 0 until renderer.pageCount) {
            return PdfRenderResult.Error(PdfRenderError.PAGE_OUT_OF_RANGE)
        }

        page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        PdfRenderResult.Success(bitmap)
    } catch (e: SecurityException) {
        PdfRenderResult.Error(PdfRenderError.OPEN_FAILED)
    } catch (e: IllegalStateException) {
        PdfRenderResult.Error(PdfRenderError.FILE_CORRUPTED)
    } catch (e: Exception) {
        PdfRenderResult.Error(PdfRenderError.UNKNOWN)
    } finally {
        page?.close()
        renderer?.close()
        pfd?.close()
    }
}
