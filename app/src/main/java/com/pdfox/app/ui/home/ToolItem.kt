package com.pdfox.app.ui.home

import androidx.annotation.DrawableRes
import com.pdfox.app.util.ToolCategory

data class ToolItem(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val category: ToolCategory,
    val destinationId: String
)
