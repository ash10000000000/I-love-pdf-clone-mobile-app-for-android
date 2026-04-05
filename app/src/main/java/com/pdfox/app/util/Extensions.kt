package com.pdfox.app.util

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.pdfox.app.R
import java.text.DecimalFormat
import java.util.Locale

// Format file size to human-readable string
fun Long.formatFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${"%.1f".format(Locale.getDefault(), this / 1024.0)} KB"
        this < 1024 * 1024 * 1024 -> "${"%.1f".format(Locale.getDefault(), this / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(Locale.getDefault(), this / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

// Format page count string
fun Int.formatPageCount(): String = "$this page${if (this != 1) "s" else ""}"

// Get category color by tool type
@ColorInt
fun getCategoryColor(context: Context, category: ToolCategory): Int {
    val colorRes = when (category) {
        ToolCategory.ORGANIZE -> R.color.category_organize
        ToolCategory.OPTIMIZE -> R.color.category_optimize
        ToolCategory.CONVERT -> R.color.category_convert
        ToolCategory.SECURITY -> R.color.category_security
        ToolCategory.EDIT -> R.color.category_edit
    }
    return ContextCompat.getColor(context, colorRes)
}

// Get category color with alpha for icon backgrounds
@ColorInt
fun getCategoryColorWithAlpha(context: Context, category: ToolCategory): Int {
    val baseColor = getCategoryColor(context, category)
    return Color.argb(30, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
}

enum class ToolCategory {
    ORGANIZE, OPTIMIZE, CONVERT, SECURITY, EDIT
}

// Extension to set padding
fun View.setPaddingAll(padding: Int) {
    setPadding(padding, padding, padding, padding)
}

// Extension to show/hide views
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}
