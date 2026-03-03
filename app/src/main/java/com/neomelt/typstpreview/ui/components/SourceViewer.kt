package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SourceViewer(content: String, scrollState: ScrollState, modifier: Modifier = Modifier) {
    Text("--- Typst 源码 ---")
    SelectionContainer {
        Text(
            text = content,
            modifier = modifier.fillMaxWidth().verticalScroll(scrollState)
        )
    }
}
