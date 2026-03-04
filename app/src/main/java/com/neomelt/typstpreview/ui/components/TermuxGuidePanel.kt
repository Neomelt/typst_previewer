package com.neomelt.typstpreview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun TermuxGuidePanel() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Termux 教程（安卓本地真预览）", style = MaterialTheme.typography.titleMedium)
        Text("1) 小米商店没有 Termux：请从 F-Droid 或 GitHub 官方仓下载 APK")
        Text("2) 打开 Termux，执行：")
        Text("pkg update && pkg upgrade -y")
        Text("pkg install -y typst")
        Text("3) 回到本 App：环境配置 -> 自动配置 -> 自动检测")
        Text("4) 导入 .typ 后自动真渲染 PDF")
    }
}
