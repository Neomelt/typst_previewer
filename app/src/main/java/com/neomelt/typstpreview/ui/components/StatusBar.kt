package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusBar(status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFEAF3FF),
        tonalElevation = 1.dp
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
