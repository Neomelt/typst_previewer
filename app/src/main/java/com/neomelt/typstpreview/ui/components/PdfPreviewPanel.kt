package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PdfPreviewPanel(
    hasPdf: Boolean,
    expectedPdfName: String?,
    pdfPageIndex: Int,
    pdfPageCount: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onExportCurrentPage: () -> Unit,
    onPickPdf: () -> Unit,
    content: @Composable () -> Unit
) {
    Text("--- PDF 预览（当前页） ---")

    if (hasPdf) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onPrevPage, enabled = pdfPageIndex > 0) {
                Text("上一页")
            }
            Button(onClick = onNextPage, enabled = pdfPageIndex < pdfPageCount - 1) {
                Text("下一页")
            }
            Button(onClick = onExportCurrentPage) {
                Text("导出当前页 PNG")
            }
            Text("第 ${pdfPageIndex + 1} / $pdfPageCount 页")
        }
        content()
        return
    }

    Text("尚未加载 PDF。先导入 .typ，再选择同名 PDF 进行预览。")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPickPdf) {
            Text(if (expectedPdfName == null) "现在导入 PDF" else "导入 $expectedPdfName")
        }
    }
}
