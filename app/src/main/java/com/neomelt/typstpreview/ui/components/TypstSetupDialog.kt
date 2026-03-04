package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun TypstSetupDialog(
    statusText: String,
    abiText: String,
    downloadUrl: String,
    onDownloadUrlChange: (String) -> Unit,
    onUseDefaultUrl: () -> Unit,
    onEnableTermuxMode: () -> Unit,
    onDismiss: () -> Unit,
    onDetect: () -> Unit,
    onAutoConfigure: () -> Unit,
    onInstallFromUrl: () -> Unit,
    onImportBinary: () -> Unit,
    onClearConfig: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Typst 环境引导") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "当前架构：$abiText\n" +
                        "步骤：\n" +
                        "1) 可点“自动配置”扫描本机可用 Typst\n" +
                        "2) 或填下载链接后点“云端安装”\n" +
                        "3) 或手动导入可执行文件\n\n" +
                        "当前状态：$statusText"
                )

                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = onDownloadUrlChange,
                    label = { Text("Typst 下载链接（bin 或 zip）") },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onUseDefaultUrl) { Text("填默认链接") }
                    TextButton(onClick = onAutoConfigure) { Text("自动配置") }
                    TextButton(onClick = onEnableTermuxMode) { Text("启用Termux") }
                    TextButton(onClick = onInstallFromUrl) { Text("云端安装") }
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
