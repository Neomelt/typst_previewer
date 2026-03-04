package com.neomelt.typstpreview

internal const val PREF_NAME = "typst_previewer_prefs"
internal const val PREF_TYP_URI = "typ_uri"
internal const val PREF_PDF_URI = "pdf_uri"
internal const val PREF_PDF_PAGE = "pdf_page"
internal const val PREF_TYPST_CMD = "typst_cmd"
internal const val PREF_TYPST_URL = "typst_url"
internal const val TERMUX_MODE = "termux:typst"

internal fun buildRestoreStatusMessage(typRestoreFailed: Boolean, pdfRestoreFailed: Boolean): String? {
    return when {
        typRestoreFailed && pdfRestoreFailed -> "检测到历史 typ/pdf 权限或路径失效，请重新导入"
        typRestoreFailed -> "检测到历史 typ 路径失效，请重新导入 .typ"
        pdfRestoreFailed -> "检测到历史 PDF 路径失效，请重新导入 PDF"
        else -> null
    }
}
