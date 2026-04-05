package com.pdfox.app.ui.tools.extractpages

data class PageThumbnail(
    val pageNumber: Int,
    var isSelected: Boolean,
    var thumbnailPath: String?
)
