package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neomelt.typstpreview.TypstHeading

@Composable
internal fun OutlinePanel(
    headings: List<TypstHeading>,
    onHeadingClick: (TypstHeading) -> Unit
) {
    Text("--- 文档目录（Typst 标题） ---")
    if (headings.isEmpty()) {
        Text("未解析到标题（提示：Typst 标题以 '=' 开头）")
        return
    }

    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
        items(headings) { heading ->
            val indent = (heading.level - 1).coerceAtLeast(0) * 12
            Text(
                text = "${" ".repeat(indent / 2)}• ${heading.title} (L${heading.lineNumber})",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeadingClick(heading) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
