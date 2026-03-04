package com.neomelt.typstpreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypstSearchTest {
    @Test
    fun returns_empty_when_query_blank() {
        val lines = TypstSearch.findLineMatches("a\nb", "   ")
        assertTrue(lines.isEmpty())
    }

    @Test
    fun finds_matches_case_insensitive_with_line_numbers() {
        val content = """
            Hello
            world
            HELLO typst
            x
        """.trimIndent()

        val lines = TypstSearch.findLineMatches(content, "hello")

        assertEquals(listOf(1, 3), lines)
    }
}
