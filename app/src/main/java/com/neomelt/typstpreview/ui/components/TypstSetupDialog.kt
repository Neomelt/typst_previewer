package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun TypstSetupDialog(
    statusText: String,
    onDismiss: () -> Unit,
    onDetect: () -> Unit,
    onAutoConfigure: () -> Unit,
    onImportBinary: () -> Unit,
    onClearConfig: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Typst 环境引导") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "步骤：\n" +
                        "1) 下载对应架构的 typst 可执行文件\n" +
                        "2) 点“导入可执行文件”并选择它\n" +
                        "3) 点“自动检测”确认可用\n\n" +
                        "当前状态：$statusText"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAutoConfigure) { Text("自动配置") }
                    TextButton(onClick = onImportBinary) { Text("导入可执行文件") }
                    TextButton(onClick = onClearConfig) { Text("清除配置") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDetect) {
                Text("自动检测")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
