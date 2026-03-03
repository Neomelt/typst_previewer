package com.neomelt.typstpreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestoreStatusTest {
    @Test
    fun returns_null_when_no_restore_failed() {
        assertNull(buildRestoreStatusMessage(false, false))
    }

    @Test
    fun returns_typ_message_when_typ_failed_only() {
        assertEquals(
            "检测到历史 typ 路径失效，请重新导入 .typ",
            buildRestoreStatusMessage(true, false)
        )
    }

    @Test
    fun returns_pdf_message_when_pdf_failed_only() {
        assertEquals(
            "检测到历史 PDF 路径失效，请重新导入 PDF",
            buildRestoreStatusMessage(false, true)
        )
    }

    @Test
    fun returns_combined_message_when_both_failed() {
        assertEquals(
            "检测到历史 typ/pdf 权限或路径失效，请重新导入",
            buildRestoreStatusMessage(true, true)
        )
    }
}
