package com.pdfox.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputFileName: String,
    val inputFilePath: String?,
    val outputFileName: String,
    val outputFilePath: String,
    val toolUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long,
    val pageCount: Int,
    val thumbnailPath: String? = null,
    val outputFormat: String = "PDF"
)
