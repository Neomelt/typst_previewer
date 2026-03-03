package com.neomelt.typstpreview.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neomelt.typstpreview.PdfRenderResult
import com.neomelt.typstpreview.renderPdfPage

@Composable
internal fun PdfPageImage(uri: Uri, pageIndex: Int, pageCount: Int) {
    val context = LocalContext.current
    val pageCache = remember(uri) { mutableStateMapOf<Int, PdfRenderResult>() }

    val result = remember(uri, pageIndex) {
        pageCache[pageIndex] ?: renderPdfPage(context, uri, pageIndex).also {
            pageCache[pageIndex] = it
        }
    }

    LaunchedEffect(uri, pageIndex, pageCount) {
        val prev = pageIndex - 1
        val next = pageIndex + 1

        if (prev >= 0 && pageCache[prev] == null) {
            pageCache[prev] = renderPdfPage(context, uri, prev)
        }
        if (next < pageCount && pageCache[next] == null) {
            pageCache[next] = renderPdfPage(context, uri, next)
        }
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
            androidx.compose.material3.Text("PDF 渲染失败：${result.reason.message}")
        }
    }
}
