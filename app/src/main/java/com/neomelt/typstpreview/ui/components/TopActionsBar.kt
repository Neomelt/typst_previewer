package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun TopActionsBar(
    hasExpectedPdfName: Boolean,
    onPickTyp: () -> Unit,
    onPickPdf: () -> Unit,
    onCompilePlaceholder: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPickTyp) {
            Text("导入 .typ")
        }
        Button(onClick = onPickPdf) {
            Text(if (hasExpectedPdfName) "选择同名 PDF" else "导入 PDF")
        }
        Button(onClick = onCompilePlaceholder) {
            Text("编译（占位）")
        }
    }
}
