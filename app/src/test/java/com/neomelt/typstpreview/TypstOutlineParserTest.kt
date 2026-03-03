package com.neomelt.typstpreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypstOutlineParserTest {
    @Test
    fun parse_extracts_headings_with_level_and_line() {
        val input = """
            = Title
            body
            == Section A
            === Subsection
        """.trimIndent()

        val result = TypstOutlineParser.parse(input)

        assertEquals(3, result.size)
        assertEquals(TypstHeading(1, "Title", 1), result[0])
        assertEquals(TypstHeading(2, "Section A", 3), result[1])
        assertEquals(TypstHeading(3, "Subsection", 4), result[2])
    }

    @Test
    fun parse_skips_non_heading_lines() {
        val input = """
            hello
            #set page(width: auto)
            =
        """.trimIndent()

        val result = TypstOutlineParser.parse(input)

        assertTrue(result.isEmpty())
    }
}
