package com.neomelt.typstpreview

import org.junit.Assert.assertEquals
import org.junit.Test

class TypstRenderParserTest {
    @Test
    fun parse_generates_heading_bullet_paragraph() {
        val content = """
            = Title
            - Item A
            normal text
        """.trimIndent()

        val blocks = TypstRenderParser.parse(content)

        assertEquals(RenderBlock.Heading(1, "Title"), blocks[0])
        assertEquals(RenderBlock.Bullet("Item A"), blocks[1])
        assertEquals(RenderBlock.Paragraph("normal text"), blocks[2])
    }
}
