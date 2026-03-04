package com.neomelt.typstpreview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val pfd = if (uri.scheme == "file") {
            val path = uri.path ?: return 0
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
        } ?: return 0

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
        pfd = if (uri.scheme == "file") {
            val path = uri.path ?: return PdfRenderResult.Error(PdfRenderError.OPEN_FAILED)
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
        } ?: return PdfRenderResult.Error(PdfRenderError.OPEN_FAILED)
        renderer = PdfRenderer(pfd)

        if (pageIndex !in 0 until renderer.pageCount) {
            return PdfRenderResult.Error(PdfRenderError.PAGE_OUT_OF_RANGE)
        }

        page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
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

internal data class ExportedImage(
    val uri: Uri,
    val displayName: String
)

internal fun exportCurrentPageAsPng(context: Context, uri: Uri, pageIndex: Int): ExportedImage? {
    val result = renderPdfPage(context, uri, pageIndex)
    if (result !is PdfRenderResult.Success) return null

    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val displayName = "typst_page_${pageIndex + 1}_$ts.png"

    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TypstPreviewer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        resolver.openOutputStream(imageUri)?.use { out ->
            result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, values, null, null)
        }

        ExportedImage(uri = imageUri, displayName = displayName)
    } catch (_: Exception) {
        null
    }
}
