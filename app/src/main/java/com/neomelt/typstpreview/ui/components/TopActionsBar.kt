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
    compiling: Boolean,
    hasTypLoaded: Boolean,
    compilerReady: Boolean,
    onPickTyp: () -> Unit,
    onPickPdf: () -> Unit,
    onCompile: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPickTyp, enabled = !compiling) {
            Text("导入 .typ")
        }
        Button(onClick = onPickPdf, enabled = !compiling) {
            Text(if (hasExpectedPdfName) "选择同名 PDF" else "导入 PDF")
        }
        Button(onClick = onCompile, enabled = hasTypLoaded && compilerReady && !compiling) {
            Text(if (compiling) "编译中..." else "编译")
        }
    }
}
