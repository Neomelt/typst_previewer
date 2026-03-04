package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun RenderModeToggle(
    renderMode: Boolean,
    onSourceMode: () -> Unit,
    onRenderMode: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onSourceMode, enabled = renderMode) {
            Text("源码")
        }
        Button(onClick = onRenderMode, enabled = !renderMode) {
            Text("渲染预览")
        }
    }
}
