package com.example.japanesegrammarapp.di

import android.content.Context
import com.example.japanesegrammarapp.data.AnalysisDao
import com.example.japanesegrammarapp.data.AppDatabase
import com.example.japanesegrammarapp.data.BookmarkDao
import com.example.japanesegrammarapp.data.BookmarkedSentenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideAnalysisDao(database: AppDatabase): AnalysisDao {
        return database.analysisDao()
    }

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideBookmarkedSentenceDao(database: AppDatabase): BookmarkedSentenceDao {
        return database.bookmarkedSentenceDao()
    }
}
