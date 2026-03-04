package com.neomelt.typstpreview

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

internal data class TypstEnvStatus(
    val available: Boolean,
    val command: String,
    val detail: String
)

internal data class TypstAutoConfigResult(
    val available: Boolean,
    val command: String?,
    val detail: String
)

internal suspend fun detectTypstEnvironment(commandPath: String?): TypstEnvStatus {
    val command = commandPath?.takeIf { it.isNotBlank() } ?: "typst"
    val available = LocalTypstCommandCompiler(command).isAvailable()
    val detail = if (available) {
        "已就绪：$command"
    } else {
        "未检测到可用 Typst：$command"
    }
    return TypstEnvStatus(available = available, command = command, detail = detail)
}

internal fun installTypstBinaryFromUri(context: Context, sourceUri: Uri): Result<String> {
    return runCatching {
        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        val target = File(binDir, "typst")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } ?: error("无法读取选择的文件")

        if (!target.setExecutable(true)) {
            error("复制成功，但无法设置可执行权限")
        }

        target.absolutePath
    }
}

internal suspend fun autoConfigureTypst(context: Context, currentPath: String?): TypstAutoConfigResult {
    val candidates = buildList {
        currentPath?.takeIf { it.isNotBlank() }?.let { add(it) }
        add(File(context.filesDir, "bin/typst").absolutePath)
        add("/data/data/com.termux/files/usr/bin/typst")
        add("typst")
    }.distinct()

    for (candidate in candidates) {
        val env = detectTypstEnvironment(candidate)
        if (env.available) {
            return TypstAutoConfigResult(
                available = true,
                command = candidate,
                detail = "自动配置成功：${env.command}"
            )
        }
    }

    return TypstAutoConfigResult(
        available = false,
        command = null,
        detail = "自动配置失败：未找到可用 Typst，请手动导入可执行文件"
    )
}
