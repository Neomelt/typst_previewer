package com.neomelt.typstpreview

data class TypstHeading(
    val level: Int,
    val title: String,
    val lineNumber: Int
)

object TypstOutlineParser {
    fun parse(input: String): List<TypstHeading> {
        return input
            .lineSequence()
            .mapIndexedNotNull { index, rawLine ->
                val line = rawLine.trim()
                if (!line.startsWith("=")) {
                    return@mapIndexedNotNull null
                }

                val level = line.takeWhile { it == '=' }.length
                val title = line.drop(level).trim()
                if (title.isBlank()) {
                    return@mapIndexedNotNull null
                }

                TypstHeading(
                    level = level,
                    title = title,
                    lineNumber = index + 1
                )
            }
            .toList()
    }
}
