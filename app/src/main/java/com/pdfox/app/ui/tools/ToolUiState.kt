package com.pdfox.app.ui.tools

import java.io.File

sealed interface ToolUiState {
    object Idle : ToolUiState
    data class Loading(val progress: Int = -1) : ToolUiState
    data class Success(val outputFile: File) : ToolUiState
    data class Error(val message: String, val exception: Throwable? = null) : ToolUiState
}
