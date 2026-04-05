package com.pdfox.app.ui.tools.removepages

data class PageThumbnail(
    val pageNumber: Int,
    var isSelected: Boolean,
    var thumbnailPath: String?
)
