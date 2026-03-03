package com.neomelt.typstpreview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

internal sealed interface PdfRenderResult {
    data class Success(val bitmap: Bitmap) : PdfRenderResult
    data class Error(val reason: PdfRenderError) : PdfRenderResult
}

internal enum class PdfRenderError(val message: String) {
    OPEN_FAILED("无法打开文件（可能无权限）"),
    PAGE_OUT_OF_RANGE("页码超出范围"),
    FILE_CORRUPTED("文件可能损坏"),
    UNKNOWN("未知错误")
}

internal fun getPdfPageCount(context: Context, uri: Uri): Int {
    return try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        PdfRenderer(pfd).use { it.pageCount }
    } catch (_: Exception) {
        0
    }
}

internal fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): PdfRenderResult {
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
    } catch (_: SecurityException) {
        PdfRenderResult.Error(PdfRenderError.OPEN_FAILED)
    } catch (_: IllegalStateException) {
        PdfRenderResult.Error(PdfRenderError.FILE_CORRUPTED)
    } catch (_: Exception) {
        PdfRenderResult.Error(PdfRenderError.UNKNOWN)
    } finally {
        page?.close()
        renderer?.close()
        pfd?.close()
    }
}

internal fun exportCurrentPageAsPng(context: Context, uri: Uri, pageIndex: Int): String? {
    val result = renderPdfPage(context, uri, pageIndex)
    if (result !is PdfRenderResult.Success) return null

    return try {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "typst_page_${pageIndex + 1}.png")
        FileOutputStream(file).use { out ->
            result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        "已导出：${file.absolutePath}"
    } catch (_: Exception) {
        null
    }
}
