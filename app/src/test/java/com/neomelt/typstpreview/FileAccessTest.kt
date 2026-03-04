package com.neomelt.typstpreview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileAccessTest {
    @Test
    fun typ_like_file_name_accepts_supported_extensions() {
        assertTrue(isTypLikeFileName("demo.typ"))
        assertTrue(isTypLikeFileName("demo.TXT"))
        assertTrue(isTypLikeFileName("notes.md"))
    }

    @Test
    fun typ_like_file_name_rejects_unsupported_or_blank() {
        assertFalse(isTypLikeFileName("report.pdf"))
        assertFalse(isTypLikeFileName(""))
        assertFalse(isTypLikeFileName(null))
    }
}
