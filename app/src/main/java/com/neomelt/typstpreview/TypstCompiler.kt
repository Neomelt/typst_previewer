package com.neomelt.typstpreview

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface TypstCompiler {
    suspend fun compile(context: Context, inputUri: Uri): CompileResult
}

internal sealed interface CompileResult {
    data class Success(val outputPdfFile: File) : CompileResult
    data class Failure(val reason: String) : CompileResult
}

internal class LocalTypstCommandCompiler(
    private val command: String = "typst"
) : TypstCompiler {
    override suspend fun compile(context: Context, inputUri: Uri): CompileResult = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "typst-compile").apply { mkdirs() }
        val inputFile = File(workDir, "input.typ")
        val outputFile = File(workDir, "output.pdf")

        try {
            copyUriToFile(context, inputUri, inputFile)
            if (outputFile.exists()) outputFile.delete()

            val process = ProcessBuilder(command, "compile", inputFile.absolutePath, outputFile.absolutePath)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext CompileResult.Failure("编译超时：30 秒内未完成")
            }

            if (process.exitValue() != 0) {
                return@withContext CompileResult.Failure("编译失败：请确认设备已安装 typst 命令")
            }

            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext CompileResult.Failure("编译失败：未生成有效 PDF")
            }

            CompileResult.Success(outputFile)
        } catch (e: Exception) {
            CompileResult.Failure("编译异常：${e.message}")
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("无法读取 Typst 源文件")
    }
}

internal fun loadCompiledPdf(file: File): Uri {
    return Uri.fromFile(file)
}

internal fun canReadPdfUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.read() }
        true
    } catch (_: Exception) {
        try {
            if (uri.scheme == "file") {
                FileInputStream(uri.path ?: "").use { it.read() }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
