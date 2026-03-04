package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchPanel(
    query: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onQueryChange: (String) -> Unit,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("搜索 Typst 文本") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPrevMatch, enabled = matchCount > 0) {
            Text("上一个")
        }
        Button(onClick = onNextMatch, enabled = matchCount > 0) {
            Text("下一个")
        }
        if (matchCount > 0) {
            Text("匹配 ${currentMatchIndex + 1}/$matchCount")
        } else {
            Text("无匹配")
        }
    }
}
