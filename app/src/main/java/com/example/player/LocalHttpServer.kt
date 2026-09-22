package com.example.player

import android.util.Log
import com.example.data.AudioTrack
import com.example.viewmodel.QuranPlayerUiState
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalHttpServer(
    private val port: Int = 8080,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrev: () -> Unit,
    private val onSearch: (String) -> Unit,
    private val onPlayTrack: (String) -> Unit,
    private val onToggleFavorite: (String) -> Unit,
    private val onToggleNightMode: () -> Unit,
    private val onSetSleepTimer: (Int?) -> Unit,
    private val onToggleShuffle: () -> Unit,
    private val onToggleRepeat: () -> Unit,
    private val getCurrentState: () -> QuranPlayerUiState
) {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverExecutor: ExecutorService? = null
    private var clientWorkers: ExecutorService? = null

    companion object {
        private const val TAG = "LocalHttpServer"

        /**
         * Returns the local IPv4 address of the device on the Wi-Fi/Ethernet network.
         */
        fun getLocalIpAddress(): String? {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in interfaces) {
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress) {
                            val sAddr = address.hostAddress
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) {
                                return sAddr
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to get local IP address", ex)
            }
            return null
        }
    }

    fun start() {
        if (isRunning.getAndSet(true)) {
            return
        }

        serverExecutor = Executors.newSingleThreadExecutor()
        clientWorkers = Executors.newCachedThreadPool()

        serverExecutor?.execute {
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
                Log.i(TAG, "Local HTTP Server started successfully on port $port")

                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        clientWorkers?.execute {
                            handleClientSocket(clientSocket)
                        }
                    } catch (se: SocketException) {
                        if (!isRunning.get()) {
                            break
                        }
                        Log.w(TAG, "SocketException in server accept loop: ${se.message}")
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Exception in server accept loop", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind local HTTP server on port $port", e)
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close server socket", e)
        }
        serverSocket = null

        try {
            clientWorkers?.shutdownNow()
            serverExecutor?.shutdownNow()
            Log.i(TAG, "Local HTTP Server stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop executors", e)
        }
    }

    private fun handleClientSocket(socket: Socket) {
        try {
            socket.use { s ->
                s.soTimeout = 10000
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine() ?: return

                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val fullUri = parts[1]

                // Consume remaining request headers
                while (true) {
                    val headerLine = reader.readLine() ?: break
                    if (headerLine.isEmpty()) break
                }

                val uriParts = fullUri.split("?", limit = 2)
                val path = uriParts[0]
                val query = if (uriParts.size > 1) uriParts[1] else ""

                val os = s.getOutputStream()

                if (method == "OPTIONS") {
                    sendResponse(os, 200, "OK", "text/plain", ByteArray(0))
                    return
                }

                when (path) {
                    "/", "/index.html" -> {
                        val bytes = HTML_REMOTE_PAGE.toByteArray(Charsets.UTF_8)
                        sendResponse(os, 200, "OK", "text/html; charset=utf-8", bytes)
                    }
                    "/api/status" -> {
                        val responseJson = generateStatusJson()
                        val bytes = responseJson.toByteArray(Charsets.UTF_8)
                        sendResponse(os, 200, "OK", "application/json; charset=utf-8", bytes)
                    }
                    "/api/tracks" -> {
                        val responseJson = generateTracksJson()
                        val bytes = responseJson.toByteArray(Charsets.UTF_8)
                        sendResponse(os, 200, "OK", "application/json; charset=utf-8", bytes)
                    }
                    "/api/command" -> {
                        val responseJson = handleCommand(query)
                        val bytes = responseJson.toByteArray(Charsets.UTF_8)
                        sendResponse(os, 200, "OK", "application/json; charset=utf-8", bytes)
                    }
                    else -> {
                        val notFoundBytes = "404 Not Found".toByteArray(Charsets.UTF_8)
                        sendResponse(os, 404, "Not Found", "text/plain", notFoundBytes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client connection handled or ended: ${e.message}")
        }
    }

    private fun sendResponse(
        os: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val header = StringBuilder()
        header.append("HTTP/1.1 $statusCode $statusText\r\n")
        header.append("Content-Type: $contentType\r\n")
        header.append("Content-Length: ${body.size}\r\n")
        header.append("Access-Control-Allow-Origin: *\r\n")
        header.append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        header.append("Access-Control-Allow-Headers: *\r\n")
        header.append("Connection: close\r\n")
        header.append("\r\n")

        os.write(header.toString().toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            os.write(body)
        }
        os.flush()
    }

    private fun generateStatusJson(): String {
        val state = getCurrentState()
        val track = state.currentTrack
        val escapedTrack = if (track != null) {
            """
            {
                "id": "${escapeJson(track.id)}",
                "title": "${escapeJson(track.title)}",
                "surahNameArabic": "${escapeJson(track.surahNameArabic)}",
                "reciterOrSubtitle": "${escapeJson(track.reciterOrSubtitle)}",
                "fileName": "${escapeJson(track.fileName)}",
                "filePath": "${escapeJson(track.filePath)}",
                "durationMs": ${track.durationMs},
                "sizeBytes": ${track.sizeBytes},
                "reciterName": "${escapeJson(track.reciterName)}"
            }
            """.trimIndent()
        } else {
            "null"
        }

        return """
        {
            "isPlaying": ${state.isPlaying},
            "isBuffering": ${state.isBuffering},
            "currentPositionMs": ${state.currentPositionMs},
            "durationMs": ${state.durationMs},
            "progress": ${state.progress},
            "repeatMode": "${state.repeatMode.name}",
            "isShuffle": ${state.isShuffle},
            "isNightMode": ${state.isNightMode},
            "sleepTimerMinutes": ${state.sleepTimerMinutes ?: "null"},
            "sleepTimerRemainingSeconds": ${state.sleepTimerRemainingSeconds},
            "isScreensaverActive": ${state.isScreensaverActive},
            "currentTrack": $escapedTrack
        }
        """.trimIndent()
    }

    private fun generateTracksJson(): String {
        val state = getCurrentState()
        val tracksList = state.tracks
        val jsonArray = tracksList.joinToString(separator = ",") { track ->
            val isFav = state.favorites.contains(track.filePath)
            """
            {
                "id": "${escapeJson(track.id)}",
                "title": "${escapeJson(track.title)}",
                "surahNameArabic": "${escapeJson(track.surahNameArabic)}",
                "reciterOrSubtitle": "${escapeJson(track.reciterOrSubtitle)}",
                "fileName": "${escapeJson(track.fileName)}",
                "filePath": "${escapeJson(track.filePath)}",
                "durationMs": ${track.durationMs},
                "sizeBytes": ${track.sizeBytes},
                "reciterName": "${escapeJson(track.reciterName)}",
                "isFavorite": $isFav
            }
            """.trimIndent()
        }
        return "[$jsonArray]"
    }

    private fun handleCommand(query: String): String {
        val params = parseQueryParams(query)
        val action = params["action"] ?: ""

        Log.i(TAG, "Command received from phone: action=$action")

        when (action) {
            "play_pause" -> onPlayPause()
            "play" -> {
                val state = getCurrentState()
                if (!state.isPlaying) onPlayPause()
            }
            "pause" -> {
                val state = getCurrentState()
                if (state.isPlaying) onPlayPause()
            }
            "next" -> onNext()
            "prev" -> onPrev()
            "search" -> {
                val q = params["q"] ?: ""
                onSearch(q)
            }
            "play_track" -> {
                val id = params["id"] ?: ""
                onPlayTrack(id)
            }
            "toggle_favorite" -> {
                val path = params["path"] ?: ""
                onToggleFavorite(path)
            }
            "toggle_night" -> onToggleNightMode()
            "toggle_shuffle" -> onToggleShuffle()
            "toggle_repeat" -> onToggleRepeat()
            "set_timer" -> {
                val minsStr = params["mins"] ?: "null"
                val mins = if (minsStr == "null" || minsStr.isBlank()) null else minsStr.toIntOrNull()
                onSetSleepTimer(mins)
            }
        }

        return "{\"status\":\"success\",\"action\":\"$action\"}"
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isBlank()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // Elegant, highly customized Apple TV-Style remote page in Arabic
    private val HTML_REMOTE_PAGE = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>جهاز التحكم عن بعد | مشغل القرآن</title>
            <style>
                :root {
                    --apple-bg: #000000;
                    --apple-card: rgba(28, 28, 30, 0.7);
                    --apple-accent: #ffffff;
                    --apple-accent-active: #a3a3a3;
                    --apple-text-primary: #f5f5f7;
                    --apple-text-secondary: #8e8e93;
                    --apple-border: rgba(255, 255, 255, 0.1);
                    --glass-bg: rgba(255, 255, 255, 0.08);
                    --glass-border: rgba(255, 255, 255, 0.06);
                }
                
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    -webkit-tap-highlight-color: transparent;
                }

                body {
                    background-color: var(--apple-bg);
                    color: var(--apple-text-primary);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 16px;
                    overflow-x: hidden;
                }

                header {
                    width: 100%;
                    max-width: 500px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 12px 6px;
                    margin-bottom: 12px;
                    border-bottom: 1px solid var(--apple-border);
                }

                header h1 {
                    font-size: 18px;
                    font-weight: 700;
                    color: var(--apple-text-primary);
                    letter-spacing: -0.5px;
                }

                header .status-indicator {
                    display: flex;
                    align-items: center;
                    font-size: 11px;
                    color: #34c759;
                    background: rgba(52, 199, 89, 0.1);
                    padding: 4px 10px;
                    border-radius: 12px;
                    font-weight: 600;
                }

                .status-dot {
                    width: 6px;
                    height: 6px;
                    background-color: #34c759;
                    border-radius: 50%;
                    margin-left: 6px;
                    animation: pulse 2s infinite;
                }

                @keyframes pulse {
                    0% { opacity: 0.4; }
                    50% { opacity: 1; }
                    100% { opacity: 0.4; }
                }

                /* Active Track Hero Widget */
                .track-card {
                    width: 100%;
                    max-width: 500px;
                    background: var(--apple-card);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid var(--apple-border);
                    border-radius: 24px;
                    padding: 20px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    text-align: center;
                    margin-bottom: 16px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                }

                .artwork-fallback {
                    width: 80px;
                    height: 80px;
                    background: var(--glass-bg);
                    border: 1px solid var(--glass-border);
                    border-radius: 18px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-bottom: 14px;
                    color: var(--apple-text-primary);
                }

                .track-title {
                    font-size: 18px;
                    font-weight: 700;
                    margin-bottom: 6px;
                    max-width: 100%;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .track-reciter {
                    font-size: 13px;
                    color: var(--apple-text-secondary);
                    margin-bottom: 18px;
                }

                /* Media Progress */
                .progress-container {
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    margin-bottom: 6px;
                }

                .progress-bar-bg {
                    width: 100%;
                    height: 4px;
                    background: rgba(255, 255, 255, 0.15);
                    border-radius: 2px;
                    position: relative;
                    margin-bottom: 8px;
                    overflow: hidden;
                }

                .progress-bar-fill {
                    height: 100%;
                    width: 0%;
                    background: var(--apple-accent);
                    border-radius: 2px;
                    transition: width 0.3s ease;
                }

                .progress-time {
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                    color: var(--apple-text-secondary);
                }

                /* Remote Controls Panel */
                .control-grid {
                    width: 100%;
                    max-width: 500px;
                    background: var(--apple-card);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid var(--apple-border);
                    border-radius: 24px;
                    padding: 16px;
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                    margin-bottom: 16px;
                }

                .remote-row {
                    display: flex;
                    justify-content: space-around;
                    align-items: center;
                }

                .btn-circle {
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    border: 1px solid var(--glass-border);
                    background: var(--glass-bg);
                    color: var(--apple-text-primary);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
                }

                .btn-circle:active {
                    background: rgba(255,255,255,0.25);
                    transform: scale(0.92);
                }

                .btn-circle.primary {
                    width: 66px;
                    height: 66px;
                    background: #ffffff;
                    color: #000000;
                    border: none;
                }

                .btn-circle.primary:active {
                    background: #d1d1d6;
                }

                .btn-circle.active {
                    background: rgba(255,255,255,0.9);
                    color: #000000;
                }

                /* Options / Modes Controls */
                .options-container {
                    display: flex;
                    justify-content: space-between;
                    width: 100%;
                    gap: 10px;
                }

                .btn-option {
                    flex: 1;
                    padding: 10px;
                    border-radius: 12px;
                    border: 1px solid var(--glass-border);
                    background: var(--glass-bg);
                    color: var(--apple-text-primary);
                    font-size: 11px;
                    font-weight: 600;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 6px;
                    cursor: pointer;
                    transition: all 0.2s;
                }

                .btn-option:active {
                    background: rgba(255, 255, 255, 0.2);
                }

                .btn-option.active {
                    background: #ffffff;
                    color: #000000;
                    border: none;
                }

                /* Sleep Timer Section */
                .timer-bar {
                    display: flex;
                    gap: 6px;
                    width: 100%;
                    overflow-x: auto;
                    padding-bottom: 4px;
                    scrollbar-width: none;
                }
                .timer-bar::-webkit-scrollbar {
                    display: none;
                }

                .timer-chip {
                    padding: 8px 12px;
                    background: rgba(255,255,255,0.06);
                    border: 1px solid var(--glass-border);
                    border-radius: 14px;
                    font-size: 11px;
                    white-space: nowrap;
                    color: var(--apple-text-primary);
                    cursor: pointer;
                }

                .timer-chip.active {
                    background: #34c759;
                    color: white;
                    border: none;
                }

                /* Search & Playlist */
                .list-container {
                    width: 100%;
                    max-width: 500px;
                    background: var(--apple-card);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid var(--apple-border);
                    border-radius: 24px;
                    padding: 16px;
                    display: flex;
                    flex-direction: column;
                    flex-grow: 1;
                    max-height: 400px;
                }

                .search-box {
                    width: 100%;
                    display: flex;
                    background: rgba(255, 255, 255, 0.08);
                    border-radius: 14px;
                    padding: 10px 14px;
                    align-items: center;
                    border: 1px solid var(--glass-border);
                    margin-bottom: 12px;
                }

                .search-box input {
                    background: none;
                    border: none;
                    color: var(--apple-text-primary);
                    outline: none;
                    flex-grow: 1;
                    font-size: 14px;
                    text-align: right;
                }

                .search-box input::placeholder {
                    color: var(--apple-text-secondary);
                }

                .track-list {
                    width: 100%;
                    overflow-y: auto;
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    flex-grow: 1;
                }

                .list-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 10px 12px;
                    border-radius: 14px;
                    background: rgba(255,255,255,0.02);
                    border: 1px solid transparent;
                    cursor: pointer;
                    transition: all 0.2s;
                }

                .list-item:active {
                    background: rgba(255,255,255,0.1);
                }

                .list-item.active {
                    background: rgba(255, 255, 255, 0.08);
                    border-color: rgba(255, 255, 255, 0.15);
                }

                .list-item-info {
                    display: flex;
                    flex-direction: column;
                    gap: 3px;
                    max-width: 80%;
                }

                .list-item-title {
                    font-size: 13.5px;
                    font-weight: 600;
                    color: var(--apple-text-primary);
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .list-item-sub {
                    font-size: 11px;
                    color: var(--apple-text-secondary);
                }

                .icon-svg {
                    fill: currentColor;
                    width: 22px;
                    height: 22px;
                }
                
                .pulse-playing {
                    display: flex;
                    align-items: flex-end;
                    gap: 2px;
                    height: 12px;
                }
                
                .pulse-bar {
                    width: 2.5px;
                    background: #ffffff;
                    border-radius: 1px;
                    animation: pulseBar 1.2s ease-in-out infinite alternate;
                }
                .pulse-bar:nth-child(1) { height: 100%; animation-delay: 0.1s; }
                .pulse-bar:nth-child(2) { height: 60%; animation-delay: 0.4s; }
                .pulse-bar:nth-child(3) { height: 80%; animation-delay: 0.2s; }
                
                @keyframes pulseBar {
                    0% { height: 20%; }
                    100% { height: 100%; }
                }
            </style>
        </head>
        <body>
            <header>
                <h1>جهاز التحكم عن بعد</h1>
                <div class="status-indicator">
                    <div class="status-dot"></div>
                    <span id="txt-status">متصل بالتلفاز</span>
                </div>
            </header>

            <!-- 1. Track Display Widget -->
            <div class="track-card">
                <div class="artwork-fallback">
                    <svg class="icon-svg" viewBox="0 0 24 24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                </div>
                <div class="track-title" id="track-title">اختر تلاوة للبدء</div>
                <div class="track-reciter" id="track-reciter">تلفاز القرآن الكريم</div>

                <div class="progress-container">
                    <div class="progress-bar-bg" id="progress-bg">
                        <div class="progress-bar-fill" id="progress-fill"></div>
                    </div>
                    <div class="progress-time">
                        <span id="time-duration">00:00</span>
                        <span id="time-elapsed">00:00</span>
                    </div>
                </div>
            </div>

            <!-- 2. Remote Controls Grid -->
            <div class="control-grid">
                <div class="remote-row">
                    <!-- Shuffle -->
                    <button class="btn-circle" id="btn-shuffle" title="عشوائي">
                        <svg class="icon-svg" viewBox="0 0 24 24"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.45 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
                    </button>
                    
                    <!-- Previous -->
                    <button class="btn-circle" id="btn-prev" title="السابق">
                        <svg class="icon-svg" viewBox="0 0 24 24"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
                    </button>
                    
                    <!-- Play / Pause -->
                    <button class="btn-circle primary" id="btn-play-pause" title="تشغيل / إيقاف">
                        <svg class="icon-svg" id="play-icon" viewBox="0 0 24 24" style="display:none;"><path d="M8 5v14l11-7z"/></svg>
                        <svg class="icon-svg" id="pause-icon" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                    </button>
                    
                    <!-- Next -->
                    <button class="btn-circle" id="btn-next" title="التالي">
                        <svg class="icon-svg" viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6zm9-12v12h2V6z"/></svg>
                    </button>
                    
                    <!-- Repeat -->
                    <button class="btn-circle" id="btn-repeat" title="تكرار">
                        <svg class="icon-svg" id="repeat-icon" viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
                    </button>
                </div>

                <!-- Night Mode & Timer Row -->
                <div class="options-container">
                    <button class="btn-option" id="btn-night-mode">
                        <svg class="icon-svg" style="width:14px; height:14px;" viewBox="0 0 24 24"><path d="M12.3 22h-.1c-5.5 0-10-4.5-10-10C2.2 6.8 6.5 2.5 11.7 2.2c.5 0 .9.3 1.1.7.2.4.1.9-.2 1.2-1.9 2-2.1 5.2-.4 7.4 1.7 2.3 4.9 2.9 7.3 1.4.4-.2.8-.2 1.1 0 .3.2.5.6.5 1 0 4.5-3.6 8.1-8.3 8.1zm-1-17.8c-3.9.5-6.9 3.8-6.9 7.8 0 4.4 3.6 8 8 8 3.5 0 6.5-2.3 7.6-5.5-2.2.4-4.5-.3-6-2.2-2.1-2.4-2.2-5.9-.7-8.1z"/></svg>
                        <span>وضع الاستماع الليلي</span>
                    </button>
                </div>

                <!-- Sleep Timer Chips -->
                <div style="display:flex; flex-direction:column; gap:6px;">
                    <span style="font-size:10px; color:var(--apple-text-secondary); font-weight:600;">مؤقت النوم</span>
                    <div class="timer-bar">
                        <div class="timer-chip" id="timer-off" onclick="setTimer(null)">إيقاف</div>
                        <div class="timer-chip" id="timer-15" onclick="setTimer(15)">15 دقيقة</div>
                        <div class="timer-chip" id="timer-30" onclick="setTimer(30)">30 دقيقة</div>
                        <div class="timer-chip" id="timer-45" onclick="setTimer(45)">45 دقيقة</div>
                        <div class="timer-chip" id="timer-60" onclick="setTimer(60)">ساعة كاملة</div>
                    </div>
                </div>
            </div>

            <!-- 3. Dynamic Searchable Playlist -->
            <div class="list-container">
                <div class="search-box">
                    <svg class="icon-svg" style="color:var(--apple-text-secondary); width:18px; height:18px; margin-left:10px;" viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                    <input type="text" id="inp-search" placeholder="ابحث باسم السورة، القارئ..." oninput="onSearchInput()">
                </div>
                <div class="track-list" id="playlist-box">
                    <!-- Loaded dynamically -->
                </div>
            </div>

            <script>
                let tracks = [];
                let currentTrackId = null;

                // Simple fetch wrapper
                function sendCommand(action, params = {}) {
                    let url = '/api/command?action=' + action;
                    for (let key in params) {
                        url += '&' + key + '=' + encodeURIComponent(params[key]);
                    }
                    fetch(url)
                        .then(res => res.json())
                        .catch(err => console.error('Command failed:', err));
                }

                // Event Listeners
                document.getElementById('btn-play-pause').onclick = () => sendCommand('play_pause');
                document.getElementById('btn-next').onclick = () => sendCommand('next');
                document.getElementById('btn-prev').onclick = () => sendCommand('prev');
                document.getElementById('btn-shuffle').onclick = () => sendCommand('toggle_shuffle');
                document.getElementById('btn-repeat').onclick = () => sendCommand('toggle_repeat');
                document.getElementById('btn-night-mode').onclick = () => sendCommand('toggle_night');

                function setTimer(mins) {
                    sendCommand('set_timer', { mins: mins === null ? 'null' : mins });
                }

                function playTrack(id) {
                    sendCommand('play_track', { id: id });
                }

                function toggleFavorite(path, e) {
                    e.stopPropagation();
                    sendCommand('toggle_favorite', { path: path });
                }

                function onSearchInput() {
                    let q = document.getElementById('inp-search').value;
                    sendCommand('search', { q: q });
                    filterLocalPlaylist(q);
                }

                // Filtering playlist locally on phone for smooth responsive search
                function filterLocalPlaylist(q) {
                    q = q.trim().toLowerCase();
                    const box = document.getElementById('playlist-box');
                    box.innerHTML = '';

                    const filtered = tracks.filter(t => 
                        t.title.toLowerCase().includes(q) || 
                        t.surahNameArabic.toLowerCase().includes(q) || 
                        t.reciterOrSubtitle.toLowerCase().includes(q) || 
                        t.reciterName.toLowerCase().includes(q)
                    );

                    if(filtered.length === 0) {
                        box.innerHTML = '<div style="text-align:center; padding:20px; color:var(--apple-text-secondary); font-size:12px;">لا توجد تلاوات مطابقة</div>';
                        return;
                    }

                    filtered.forEach(t => {
                        const isActive = t.id === currentTrackId;
                        const isFav = t.isFavorite;
                        const item = document.createElement('div');
                        item.className = 'list-item' + (isActive ? ' active' : '');
                        item.onclick = () => playTrack(t.id);

                        let pulseAnimation = '';
                        if (isActive) {
                            pulseAnimation = `
                                <div class="pulse-playing">
                                    <div class="pulse-bar"></div>
                                    <div class="pulse-bar"></div>
                                    <div class="pulse-bar"></div>
                                </div>
                            `;
                        }

                        item.innerHTML = `
                            <div class="list-item-info">
                                <div class="list-item-title">${'$'}{t.title}</div>
                                <div class="list-item-sub">${'$'}{t.reciterOrSubtitle}</div>
                            </div>
                            <div style="display:flex; align-items:center; gap:12px;">
                                ${'$'}{pulseAnimation}
                                <span onclick="toggleFavorite('${'$'}{t.filePath.replace(/'/g, "\\'")}', event)" style="color:${'$'}{isFav ? '#ff453a' : 'var(--apple-text-secondary)'}; font-size:18px;">
                                    ${'$'}{isFav ? '★' : '☆'}
                                </span>
                            </div>
                        `;
                        box.appendChild(item);
                    });
                }

                // Poll Status from TV (every 1 second)
                function pollStatus() {
                    fetch('/api/status')
                        .then(res => res.json())
                        .then(status => {
                            document.getElementById('txt-status').innerText = 'متصل بالتلفاز';
                            document.getElementById('txt-status').parentElement.style.background = 'rgba(52, 199, 89, 0.1)';
                            document.getElementById('txt-status').parentElement.style.color = '#34c759';

                            // Play / Pause Icon toggle
                            if (status.isPlaying) {
                                document.getElementById('play-icon').style.display = 'none';
                                document.getElementById('pause-icon').style.display = 'block';
                            } else {
                                document.getElementById('play-icon').style.display = 'block';
                                document.getElementById('pause-icon').style.display = 'none';
                            }

                            // Track Title / Reciter
                            if (status.currentTrack) {
                                document.getElementById('track-title').innerText = status.currentTrack.title;
                                document.getElementById('track-reciter').innerText = status.currentTrack.reciterOrSubtitle;
                                currentTrackId = status.currentTrack.id;
                            } else {
                                document.getElementById('track-title').innerText = 'اختر تلاوة للبدء';
                                document.getElementById('track-reciter').innerText = 'تلفاز القرآن الكريم';
                                currentTrackId = null;
                            }

                            // Progress Bar
                            const elapsed = status.currentPositionMs;
                            const duration = status.durationMs;
                            const pct = status.progress * 100;
                            document.getElementById('progress-fill').style.width = pct + '%';

                            document.getElementById('time-elapsed').innerText = formatDuration(elapsed);
                            document.getElementById('time-duration').innerText = formatDuration(duration);

                            // Active states classes
                            toggleActiveState('btn-shuffle', status.isShuffle);
                            toggleActiveState('btn-repeat', status.repeatMode !== 'OFF');
                            toggleActiveState('btn-night-mode', status.isNightMode);

                            // Active Timer Chip
                            clearTimerChips();
                            if (status.sleepTimerMinutes) {
                                let chip = document.getElementById('timer-' + status.sleepTimerMinutes);
                                if (chip) chip.className = 'timer-chip active';
                            } else {
                                document.getElementById('timer-off').className = 'timer-chip active';
                            }

                            // Synchronize playlist selections
                            const items = document.querySelectorAll('.list-item');
                            items.forEach(el => {
                                el.classList.remove('active');
                            });
                            
                            // Re-filter/draw playlist if tracks are modified
                            updatePlaylistHighlight();
                        })
                        .catch(err => {
                            document.getElementById('txt-status').innerText = 'انقطع الاتصال';
                            document.getElementById('txt-status').parentElement.style.background = 'rgba(255, 69, 58, 0.1)';
                            document.getElementById('txt-status').parentElement.style.color = '#ff453a';
                        });
                }

                function updatePlaylistHighlight() {
                    const items = document.querySelectorAll('.list-item');
                    // Find active and highlight it
                }

                function toggleActiveState(id, isActive) {
                    const btn = document.getElementById(id);
                    if (isActive) {
                        btn.className = 'btn-circle active';
                    } else {
                        btn.className = 'btn-circle';
                    }
                }

                function clearTimerChips() {
                    const ids = ['timer-off', 'timer-15', 'timer-30', 'timer-45', 'timer-60'];
                    ids.forEach(id => {
                        const chip = document.getElementById(id);
                        if (chip) chip.className = 'timer-chip';
                    });
                }

                function formatDuration(ms) {
                    if (!ms || isNaN(ms)) return '00:00';
                    let totalSec = Math.floor(ms / 1000);
                    let min = Math.floor(totalSec / 60);
                    let sec = totalSec % 60;
                    return (min < 10 ? '0' + min : min) + ':' + (sec < 10 ? '0' + sec : sec);
                }

                // Fetch Tracks List
                function loadTracks() {
                    fetch('/api/tracks')
                        .then(res => res.json())
                        .then(data => {
                            tracks = data;
                            filterLocalPlaylist(document.getElementById('inp-search').value);
                        })
                        .catch(err => console.error('Failed to load tracks list:', err));
                }

                // Start polling and loading
                setInterval(pollStatus, 1000);
                setInterval(loadTracks, 3000); // Poll tracks every 3s to reflect favorites toggling immediately
                pollStatus();
                loadTracks();
            </script>
        </body>
        </html>
    """.trimIndent()
}
