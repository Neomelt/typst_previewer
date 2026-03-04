package com.neomelt.typstpreview

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream

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

internal fun preferredAbi(): String {
    return Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
}

internal suspend fun installTypstFromUrl(context: Context, url: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val cleanUrl = url.trim()
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            "仅支持 http/https 下载链接"
        }

        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        val target = File(binDir, "typst")
        val tmpFile = File(context.cacheDir, "typst-download.tmp")

        downloadToFile(cleanUrl, tmpFile)

        val installed = when {
            tryExtractTypstFromZip(tmpFile, target) -> true
            tryExtractTypstFromTarXz(tmpFile, target) -> true
            else -> {
                tmpFile.copyTo(target, overwrite = true)
                true
            }
        }

        tmpFile.delete()
        if (!installed) error("下载完成，但无法安装 typst")

        if (!target.setExecutable(true)) {
            error("下载成功，但无法设置可执行权限")
        }

        target.absolutePath
    }
}

private fun downloadToFile(url: String, target: File) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 15_000
    conn.readTimeout = 60_000
    conn.instanceFollowRedirects = true

    conn.inputStream.use { input ->
        FileOutputStream(target).use { output ->
            input.copyTo(output)
        }
    }
}

private fun tryExtractTypstFromZip(zipFile: File, target: File): Boolean {
    return runCatching {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && name == "typst") {
                    FileOutputStream(target).use { output ->
                        zip.copyTo(output)
                    }
                    return true
                }
                entry = zip.nextEntry
            }
        }
        false
    }.getOrDefault(false)
}

private fun tryExtractTypstFromTarXz(file: File, target: File): Boolean {
    return runCatching {
        FileInputStream(file).use { fis ->
            XZInputStream(fis).use { xzIn ->
                TarArchiveInputStream(xzIn).use { tarIn ->
                    var entry = tarIn.nextTarEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        if (!entry.isDirectory && name == "typst") {
                            FileOutputStream(target).use { out ->
                                tarIn.copyTo(out)
                            }
                            return true
                        }
                        entry = tarIn.nextTarEntry
                    }
                }
            }
        }
        false
    }.getOrDefault(false)
}
