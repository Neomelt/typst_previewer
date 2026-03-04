package com.neomelt.typstpreview

internal object TypstSearch {
    fun findLineMatches(content: String, keyword: String): List<Int> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        return content
            .lineSequence()
            .mapIndexedNotNull { index, line ->
                if (line.contains(query, ignoreCase = true)) index + 1 else null
            }
            .toList()
    }
}
