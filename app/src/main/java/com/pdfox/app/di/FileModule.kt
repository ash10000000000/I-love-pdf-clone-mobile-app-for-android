package com.pdfox.app.di

import android.content.Context
import com.pdfox.app.data.db.RecentFileDao
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.util.FileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FileModule {

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }

    @Provides
    @Singleton
    fun provideFileRepository(recentFileDao: RecentFileDao): FileRepository {
        return FileRepository(recentFileDao)
    }
}
