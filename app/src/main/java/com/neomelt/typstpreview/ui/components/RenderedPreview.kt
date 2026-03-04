package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomelt.typstpreview.RenderBlock

@Composable
internal fun RenderedPreview(blocks: List<RenderBlock>, scrollState: ScrollState, modifier: Modifier = Modifier) {
    Text("--- 渲染预览（基础） ---")
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth().verticalScroll(scrollState)
    ) {
        blocks.forEach { block ->
            when (block) {
                is RenderBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    Text(
                        text = block.text,
                        fontSize = size,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is RenderBlock.Bullet -> {
                    Text(
                        text = "• ${block.text}",
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is RenderBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                RenderBlock.Spacer -> {
                    Text(text = "", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
