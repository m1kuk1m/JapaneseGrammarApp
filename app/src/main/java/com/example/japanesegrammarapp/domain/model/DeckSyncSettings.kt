package com.example.japanesegrammarapp.domain.model

data class DeckSyncSettings(
    val isEnabled: Boolean = false,
    val port: Int = DEFAULT_PORT,
    val pin: String = "8848",
    val authToken: String = ""
) {
    companion object {
        const val DEFAULT_PORT = 8765
        const val MDNS_SERVICE_TYPE = "_yomillm._tcp"
        const val MDNS_SERVICE_NAME = "YomiLLM-DeckSync"
    }
}

data class DeckSyncServerStatus(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = DeckSyncSettings.DEFAULT_PORT,
    val isMdnsBroadcasting: Boolean = false,
    val pin: String = "8848"
)
