package com.example.japanesegrammarapp.domain.usecase

import com.example.japanesegrammarapp.domain.model.AnalysisEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AnalysisEvent>()
    val events: SharedFlow<AnalysisEvent> = _events.asSharedFlow()

    suspend fun post(event: AnalysisEvent) {
        _events.emit(event)
    }
}