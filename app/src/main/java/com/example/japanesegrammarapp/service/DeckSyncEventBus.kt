package com.example.japanesegrammarapp.service

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class DeckSyncEvent {
    data class ScreenshotReceived(val imageUri: Uri) : DeckSyncEvent()
    data class StatusChanged(
        val isRunning: Boolean,
        val ipAddress: String,
        val port: Int,
        val isMdnsBroadcasting: Boolean
    ) : DeckSyncEvent()
}

object DeckSyncEventBus {
    private val _events = MutableSharedFlow<DeckSyncEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<DeckSyncEvent> = _events.asSharedFlow()

    fun emit(event: DeckSyncEvent) {
        _events.tryEmit(event)
    }
}
