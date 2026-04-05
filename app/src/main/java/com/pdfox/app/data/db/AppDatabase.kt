package com.pdfox.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFile::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        const val DATABASE_NAME = "pdfox_database"
    }
}
