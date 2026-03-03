package com.neomelt.typstpreview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var typContent by remember { mutableStateOf("还未导入 .typ 文件") }
    var typName by remember { mutableStateOf<String?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var pdfPageIndex by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("提示：先导入 .typ，再导入对应 PDF 预览") }

    val pickTyp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        typName = DocumentFile.fromSingleUri(context, uri)?.name
        typContent = readText(context, uri)
        status = "已导入 Typst: ${typName ?: "unknown"}"
    }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        if (!isPdf(context, uri)) {
            status = "选择的不是 PDF 文件"
            return@rememberLauncherForActivityResult
        }
        pdfUri = uri
        pdfPageCount = getPdfPageCount(context, uri)
        pdfPageIndex = 0
        status = "已导入 PDF，共 ${pdfPageCount} 页"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Typst Android 预览器（框架版）", style = MaterialTheme.typography.titleLarge)
        Text(status)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickTyp.launch(arrayOf("text/*")) }) {
                Text("导入 .typ")
            }
            Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                Text("导入 PDF")
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(uri, pageIndex) {
        renderPdfPage(context, uri, pageIndex)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF page",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    } else {
        Text("PDF 渲染失败")
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

private fun renderPdfPage(context: android.content.Context, uri: Uri, pageIndex: Int): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        renderer = PdfRenderer(pfd)
        if (pageIndex !in 0 until renderer.pageCount) return null
        page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    } catch (_: Exception) {
        null
    } finally {
        page?.close()
        renderer?.close()
        pfd?.close()
    }
}
