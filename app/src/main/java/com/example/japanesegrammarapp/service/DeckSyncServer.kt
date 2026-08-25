package com.example.japanesegrammarapp.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.utils.AppLogger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class DeckSyncServer(
    private val appContext: Context? = null,
    private val settingsRepository: SettingsRepository,
    private val onScreenshotReceived: (Uri) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private val isRunningFlag = AtomicBoolean(false)
    private var serverThread: Thread? = null
    private val threadPool = Executors.newCachedThreadPool()

    val isRunning: Boolean
        get() = isRunningFlag.get()

    fun start(port: Int = 8765) {
        if (isRunningFlag.get()) return

        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress("0.0.0.0", port))
            serverSocket = socket
            isRunningFlag.set(true)

            serverThread = Thread({
                AppLogger.d("DECK_SYNC_SERVER", "DeckSync native ServerSocket listening on port $port")
                while (isRunningFlag.get() && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        threadPool.execute {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (isRunningFlag.get()) {
                            AppLogger.e("DECK_SYNC_SERVER", "Accept error", e)
                        }
                    }
                }
            }, "DeckSyncServer-AcceptLoop").apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SERVER", "Failed to bind ServerSocket on port $port", e)
            isRunningFlag.set(false)
            throw e
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 8000
            val input = BufferedInputStream(client.getInputStream())
            val output = client.getOutputStream()

            // 1. Read HTTP request line and headers
            val headerBytes = ByteArrayOutputStream()
            var prev1 = -1
            var prev2 = -1
            var prev3 = -1
            var b: Int
            while (input.read().also { b = it } != -1) {
                headerBytes.write(b)
                if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                    break
                }
                prev3 = prev2
                prev2 = prev1
                prev1 = b
            }

            val headerText = headerBytes.toString("UTF-8")
            val lines = headerText.split("\r\n")
            if (lines.isEmpty() || lines[0].isBlank()) {
                client.close()
                return
            }

            val requestLine = lines[0]
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                client.close()
                return
            }

            val method = parts[0].uppercase()
            val rawPath = parts[1]
            val path = rawPath.split("?")[0].trimEnd('/')

            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0

            when {
                method == "GET" && path == "/api/v1/ping" -> {
                    val json = """{"status":"ready","app":"YomiLLM","version":"1.9.2"}"""
                    sendHttpResponse(output, 200, "OK", "application/json", json.toByteArray(Charsets.UTF_8))
                }

                method == "POST" && path == "/api/v1/pair" -> {
                    val bodyBytes = readExactBytes(input, contentLength)
                    val bodyStr = String(bodyBytes, Charsets.UTF_8)
                    val jsonObj = try {
                        JsonParser.parseString(bodyStr).asJsonObject
                    } catch (e: Exception) {
                        JsonObject()
                    }
                    val pin = jsonObj.get("pin")?.asString?.trim() ?: ""
                    val currentSettings = settingsRepository.getDeckSyncSettings()

                    if (pin == currentSettings.pin && pin.isNotBlank()) {
                        var token = currentSettings.authToken
                        if (token.isBlank()) {
                            token = UUID.randomUUID().toString()
                            settingsRepository.setDeckSyncAuthToken(token)
                        }
                        val resJson = """{"status":"paired","token":"$token"}"""
                        sendHttpResponse(output, 200, "OK", "application/json", resJson.toByteArray(Charsets.UTF_8))
                    } else {
                        val errJson = """{"status":"error","message":"Invalid PIN code"}"""
                        sendHttpResponse(output, 401, "Unauthorized", "application/json", errJson.toByteArray(Charsets.UTF_8))
                    }
                }

                method == "POST" && path == "/api/v1/screenshot" -> {
                    val authHeader = headers["x-auth-token"]?.trim()
                    val currentToken = settingsRepository.getDeckSyncAuthToken()

                    if (currentToken.isBlank() || authHeader != currentToken) {
                        val errJson = """{"status":"error","message":"Unauthorized"}"""
                        sendHttpResponse(output, 401, "Unauthorized", "application/json", errJson.toByteArray(Charsets.UTF_8))
                        return
                    }

                    val bodyBytes = readExactBytes(input, contentLength)
                    val contentType = headers["content-type"] ?: ""

                    val imageBytes = if (contentType.contains("multipart/form-data")) {
                        extractImageFromMultipart(bodyBytes, contentType)
                    } else {
                        bodyBytes
                    }

                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        val filename = "deck_sync_${System.currentTimeMillis()}.jpg"
                        val cacheDir = appContext?.cacheDir ?: File(System.getProperty("java.io.tmpdir"), "deck_sync").apply { mkdirs() }
                        val targetFile = File(cacheDir, filename)
                        FileOutputStream(targetFile).use { it.write(imageBytes) }

                        val fileUri = if (appContext != null) {
                            FileProvider.getUriForFile(
                                appContext,
                                "${appContext.packageName}.fileprovider",
                                targetFile
                            )
                        } else {
                            Uri.fromFile(targetFile)
                        }
                        onScreenshotReceived(fileUri)

                        val resJson = """{"status":"success","received_at":${System.currentTimeMillis()}}"""
                        sendHttpResponse(output, 200, "OK", "application/json", resJson.toByteArray(Charsets.UTF_8))
                    } else {
                        val errJson = """{"status":"error","message":"No image data in request"}"""
                        sendHttpResponse(output, 400, "Bad Request", "application/json", errJson.toByteArray(Charsets.UTF_8))
                    }
                }

                else -> {
                    val notFound = """{"status":"error","message":"Not Found"}"""
                    sendHttpResponse(output, 404, "Not Found", "application/json", notFound.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SERVER", "Error handling client", e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun readExactBytes(input: InputStream, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(buffer, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return if (totalRead == length) buffer else buffer.copyOf(totalRead)
    }

    private fun sendHttpResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun extractImageFromMultipart(body: ByteArray, contentType: String): ByteArray? {
        val boundaryMarker = contentType.substringAfter("boundary=", "").trim().trim('"')
        if (boundaryMarker.isBlank()) return body

        val boundaryBytes = "--$boundaryMarker".toByteArray(Charsets.UTF_8)
        val doubleCrlf = "\r\n\r\n".toByteArray(Charsets.UTF_8)

        val headerEndIdx = indexOfSubarray(body, doubleCrlf, 0)
        if (headerEndIdx == -1) return body

        val imageStart = headerEndIdx + 4
        val nextBoundary = indexOfSubarray(body, boundaryBytes, imageStart)
        val imageEnd = if (nextBoundary != -1) (nextBoundary - 2).coerceAtLeast(imageStart) else body.size

        return body.copyOfRange(imageStart, imageEnd)
    }

    private fun indexOfSubarray(array: ByteArray, target: ByteArray, start: Int): Int {
        if (target.isEmpty() || array.size < target.size) return -1
        for (i in start..(array.size - target.size)) {
            var match = true
            for (j in target.indices) {
                if (array[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    fun stop() {
        try {
            isRunningFlag.set(false)
            serverSocket?.close()
            serverSocket = null
            serverThread?.interrupt()
            serverThread = null
            AppLogger.d("DECK_SYNC_SERVER", "DeckSync native ServerSocket stopped")
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SERVER", "Error stopping ServerSocket", e)
        }
    }
}
