package com.example.japanesegrammarapp.service

import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DeckSyncEventBusTest {

    @Test
    fun emitStatusChangedEvent() = runTest {
        val event = DeckSyncEvent.StatusChanged(
            isRunning = true,
            ipAddress = "192.168.1.100",
            port = 8765,
            isMdnsBroadcasting = true
        )

        val receivedEvents = mutableListOf<DeckSyncEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeckSyncEventBus.events.toList(receivedEvents)
        }

        DeckSyncEventBus.emit(event)

        assertEquals(1, receivedEvents.size)
        val status = receivedEvents[0] as DeckSyncEvent.StatusChanged
        assertTrue(status.isRunning)
        assertEquals("192.168.1.100", status.ipAddress)
        assertEquals(8765, status.port)
        assertTrue(status.isMdnsBroadcasting)

        job.cancel()
    }

    @Test
    fun emitScreenshotReceivedEvent() = runTest {
        val mockUri = mock(Uri::class.java)
        val event = DeckSyncEvent.ScreenshotReceived(mockUri)

        val receivedEvents = mutableListOf<DeckSyncEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeckSyncEventBus.events.toList(receivedEvents)
        }

        DeckSyncEventBus.emit(event)

        assertEquals(1, receivedEvents.size)
        assertTrue(receivedEvents[0] is DeckSyncEvent.ScreenshotReceived)
        val screenshotEvent = receivedEvents[0] as DeckSyncEvent.ScreenshotReceived
        assertEquals(mockUri, screenshotEvent.imageUri)

        job.cancel()
    }

    @Test
    fun newSubscriberDoesNotReceiveOldEvents_noReplay() = runTest {
        val mockUri = mock(Uri::class.java)
        // Emit before any subscriber exists
        DeckSyncEventBus.emit(DeckSyncEvent.ScreenshotReceived(mockUri))

        // New subscriber subscribes after emission
        val receivedEvents = mutableListOf<DeckSyncEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeckSyncEventBus.events.toList(receivedEvents)
        }

        // Replay cache is 0, so no old event should be replayed
        assertEquals(0, receivedEvents.size)

        job.cancel()
    }
}
