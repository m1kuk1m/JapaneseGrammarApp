package com.example.japanesegrammarapp.di

import com.example.japanesegrammarapp.domain.repository.*
import com.example.japanesegrammarapp.domain.usecase.AnalysisTaskManager
import com.example.japanesegrammarapp.domain.usecase.DefaultAnalysisTaskManager
import com.example.japanesegrammarapp.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindPagedHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): PagedHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindOcrRepository(
        ocrRepositoryImpl: OcrRepositoryImpl
    ): OcrRepository

    @Binds
    @Singleton
    abstract fun bindTtsRepository(
        ttsRepositoryImpl: TtsRepositoryImpl
    ): TtsRepository

    @Binds
    @Singleton
    abstract fun bindLlmRepository(
        llmRepositoryImpl: LlmRepositoryImpl
    ): LlmRepository

    @Binds
    @Singleton
    abstract fun bindImageAttachmentLoader(
        imageAttachmentLoaderImpl: ImageAttachmentLoaderImpl
    ): ImageAttachmentLoader

    @Binds
    @Singleton
    abstract fun bindLlmAnalysisService(
        llmAnalysisServiceImpl: LlmAnalysisServiceImpl
    ): LlmAnalysisService

    @Binds
    @Singleton
    abstract fun bindDetailedResultSerializer(
        detailedResultSerializerImpl: DetailedResultSerializerImpl
    ): DetailedResultSerializer

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(
        bookmarkRepositoryImpl: BookmarkRepositoryImpl
    ): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindUiPreferencesRepository(
        uiPreferencesRepositoryImpl: UiPreferencesRepositoryImpl
    ): UiPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisTaskManager(
        defaultAnalysisTaskManager: DefaultAnalysisTaskManager
    ): AnalysisTaskManager
}
