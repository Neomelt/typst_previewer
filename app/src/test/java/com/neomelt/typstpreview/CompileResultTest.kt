package com.neomelt.typstpreview

import org.junit.Assert.assertTrue
import org.junit.Test

class CompileResultTest {
    @Test
    fun failure_contains_reason() {
        val result: CompileResult = CompileResult.Failure("missing typst")
        assertTrue(result is CompileResult.Failure)
        assertTrue((result as CompileResult.Failure).reason.contains("typst"))
    }
}
