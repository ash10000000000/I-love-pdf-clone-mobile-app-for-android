package com.pdfox.app.ui.tools.organizepages

data class OrganizePageItem(
    val originalPageNumber: Int,
    var rotation: Int,
    var thumbnailPath: String?
)
