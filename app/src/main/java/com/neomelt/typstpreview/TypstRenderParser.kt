package com.neomelt.typstpreview

internal sealed interface RenderBlock {
    data class Heading(val level: Int, val text: String) : RenderBlock
    data class Bullet(val text: String) : RenderBlock
    data class Paragraph(val text: String) : RenderBlock
    data object Spacer : RenderBlock
}

internal object TypstRenderParser {
    fun parse(content: String): List<RenderBlock> {
        return content.lineSequence().map { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> RenderBlock.Spacer
                trimmed.startsWith("=") -> {
                    val level = trimmed.takeWhile { it == '=' }.length.coerceIn(1, 3)
                    val text = normalizeInline(trimmed.dropWhile { it == '=' }.trim())
                    RenderBlock.Heading(level, text)
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    RenderBlock.Bullet(normalizeInline(trimmed.drop(2).trim()))
                }
                else -> RenderBlock.Paragraph(normalizeInline(trimmed))
            }
        }.toList()
    }

    private fun normalizeInline(input: String): String {
        return input
            .replace("*", "")
            .replace("#", "")
            .replace("[", "")
            .replace("]", "")
    }
}
