package com.pdfox.app.ui.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfox.app.util.FileManager
import com.pdfox.app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

abstract class BaseToolViewModel : ViewModel() {

    @Inject
    lateinit var fileManager: FileManager

    private val _uiState = MutableStateFlow<ToolUiState>(ToolUiState.Idle)
    val uiState: StateFlow<ToolUiState> = _uiState.asStateFlow()

    protected fun setState(state: ToolUiState) {
        _uiState.value = state
    }

    protected fun setLoading(progress: Int = -1) {
        _uiState.value = ToolUiState.Loading(progress)
    }

    protected fun setSuccess(outputFile: File) {
        _uiState.value = ToolUiState.Success(outputFile)
    }

    protected fun setError(message: String, exception: Throwable? = null) {
        Timber.e(exception, "Tool error: $message")
        _uiState.value = ToolUiState.Error(message, exception)
    }

    fun resetState() {
        _uiState.value = ToolUiState.Idle
    }

    fun processFiles(toolType: String, fileUris: List<Uri>, options: Map<String, Any>) {
        viewModelScope.launch {
            try {
                _uiState.value = ToolUiState.Loading()
                val inputFiles = fileUris.map { uri ->
                    val fileName = fileManager.getFileNameFromUri(uri) ?: "input"
                    fileManager.uriToFile(uri, fileName)
                }

                val result = executeToolAction(toolType, inputFiles, options)
                result.fold(
                    onSuccess = { outputFile ->
                        setSuccess(outputFile)
                    },
                    onFailure = { exception ->
                        setError(
                            message = exception.message ?: "Processing failed",
                            exception = exception
                        )
                    }
                )
            } catch (e: Exception) {
                setError(
                    message = e.message ?: "An unexpected error occurred",
                    exception = e
                )
            }
        }
    }

    protected abstract suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File>

    protected fun <T> safeExecute(action: () -> T): Result<T> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            Timber.e(e, "Error during tool execution")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    protected suspend fun <T> safeExecuteSuspend(action: suspend () -> T): Result<T> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            Timber.e(e, "Error during tool execution")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
}
