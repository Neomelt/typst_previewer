package com.neomelt.typstpreview

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.BufferedReader
import java.io.InputStreamReader

internal fun readText(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: "读取失败"
    } catch (e: Exception) {
        "读取失败: ${e.message}"
    }
}

internal fun hasSameBaseName(typName: String?, pdfName: String?): Boolean {
    if (typName.isNullOrBlank() || pdfName.isNullOrBlank()) return false
    return typName.substringBeforeLast(".") == pdfName.substringBeforeLast(".")
}

internal fun canReadUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.read()
        }
        true
    } catch (_: Exception) {
        false
    }
}

internal fun grantReadPermissionSafely(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {
        // ignore non-persistable permission cases
    }
}

internal fun isPdf(context: Context, uri: Uri): Boolean {
    val type = context.contentResolver.getType(uri)
    if (type == "application/pdf") return true
    val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    return ext.equals("pdf", true)
}
