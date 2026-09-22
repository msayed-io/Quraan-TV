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
    private val onSetVolume: (Int) -> Unit = {},
    private val onAdjustVolume: (Int) -> Unit = {},
    private val onToggleMute: () -> Unit = {},
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
            "volumePercent": ${state.volumePercent},
            "isMuted": ${state.isMuted},
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
            "set_volume" -> {
                val vol = params["vol"]?.toIntOrNull() ?: 100
                onSetVolume(vol)
            }
            "adjust_volume" -> {
                val delta = params["delta"]?.toIntOrNull() ?: 0
                onAdjustVolume(delta)
            }
            "toggle_mute" -> onToggleMute()
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

    // Ultra-Premium Apple iOS 18-Style Remote Web Companion in Arabic
    private val HTML_REMOTE_PAGE = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
            <title>جهاز التحكم عن بعد | تلفاز القرآن</title>
            <style>
                :root {
                    --bg-canvas: #000000;
                    --card-surface: rgba(28, 28, 30, 0.75);
                    --card-surface-elevated: rgba(44, 44, 46, 0.8);
                    --card-border: rgba(255, 255, 255, 0.08);
                    --card-border-subtle: rgba(255, 255, 255, 0.04);
                    --text-primary: #FFFFFF;
                    --text-secondary: #8E8E93;
                    --text-tertiary: #636366;
                    --apple-blue: #0A84FF;
                    --apple-green: #30D158;
                    --apple-red: #FF453A;
                    --glass-bg: rgba(255, 255, 255, 0.06);
                    --glass-pressed: rgba(255, 255, 255, 0.15);
                }

                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    -webkit-tap-highlight-color: transparent;
                    font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Segoe UI", Roboto, sans-serif;
                }

                body {
                    background-color: var(--bg-canvas);
                    color: var(--text-primary);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: max(16px, env(safe-area-inset-top)) 16px max(24px, env(safe-area-inset-bottom));
                    overflow-x: hidden;
                }

                .app-wrapper {
                    width: 100%;
                    max-width: 440px;
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                }

                /* 1. Dynamic Island / Top Status Capsule */
                .top-capsule {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    background: var(--card-surface);
                    backdrop-filter: blur(24px);
                    -webkit-backdrop-filter: blur(24px);
                    border: 0.5px solid var(--card-border);
                    border-radius: 20px;
                    padding: 8px 14px;
                    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
                }

                .brand-info {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .brand-icon {
                    width: 24px;
                    height: 24px;
                    border-radius: 6px;
                    background: var(--glass-bg);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: var(--text-primary);
                }

                .brand-title {
                    font-size: 13px;
                    font-weight: 700;
                    letter-spacing: -0.2px;
                }

                .connection-badge {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    font-size: 11px;
                    font-weight: 600;
                    color: var(--apple-green);
                    background: rgba(48, 209, 88, 0.12);
                    padding: 4px 10px;
                    border-radius: 12px;
                    transition: all 0.3s ease;
                }

                .connection-dot {
                    width: 6px;
                    height: 6px;
                    border-radius: 50%;
                    background: var(--apple-green);
                    box-shadow: 0 0 6px var(--apple-green);
                    animation: pulseDot 2s infinite ease-in-out;
                }

                @keyframes pulseDot {
                    0%, 100% { opacity: 0.4; transform: scale(0.85); }
                    50% { opacity: 1; transform: scale(1.15); }
                }

                /* 2. Hero Now Playing Card */
                .hero-card {
                    background: var(--card-surface);
                    backdrop-filter: blur(30px);
                    -webkit-backdrop-filter: blur(30px);
                    border: 0.5px solid var(--card-border);
                    border-radius: 28px;
                    padding: 22px 18px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    text-align: center;
                    box-shadow: 0 12px 36px rgba(0, 0, 0, 0.6);
                }

                .artwork-box {
                    width: 130px;
                    height: 130px;
                    border-radius: 22px;
                    background: linear-gradient(135deg, #2C2C2E 0%, #1C1C1E 100%);
                    border: 0.5px solid rgba(255, 255, 255, 0.12);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-bottom: 16px;
                    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.5);
                    position: relative;
                    overflow: hidden;
                }

                .artwork-icon {
                    width: 48px;
                    height: 48px;
                    color: rgba(255, 255, 255, 0.4);
                }

                .playing-overlay {
                    position: absolute;
                    bottom: 8px;
                    display: flex;
                    gap: 3px;
                    align-items: flex-end;
                    height: 16px;
                }

                .wave-bar {
                    width: 3px;
                    background: var(--text-primary);
                    border-radius: 1.5px;
                    animation: bounceBar 1.2s ease-in-out infinite alternate;
                }

                .wave-bar:nth-child(1) { height: 100%; animation-delay: 0.1s; }
                .wave-bar:nth-child(2) { height: 60%; animation-delay: 0.35s; }
                .wave-bar:nth-child(3) { height: 80%; animation-delay: 0.2s; }

                @keyframes bounceBar {
                    0% { height: 20%; }
                    100% { height: 100%; }
                }

                .track-title {
                    font-size: 17px;
                    font-weight: 700;
                    color: var(--text-primary);
                    margin-bottom: 4px;
                    width: 100%;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .track-reciter {
                    font-size: 13px;
                    color: var(--text-secondary);
                    margin-bottom: 18px;
                    width: 100%;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                /* Progress bar */
                .progress-wrapper {
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                }

                .progress-track {
                    width: 100%;
                    height: 5px;
                    background: rgba(255, 255, 255, 0.12);
                    border-radius: 3px;
                    overflow: hidden;
                    position: relative;
                }

                .progress-fill {
                    height: 100%;
                    width: 0%;
                    background: var(--text-primary);
                    border-radius: 3px;
                    transition: width 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
                }

                .progress-labels {
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                    font-weight: 500;
                    color: var(--text-secondary);
                }

                /* 3. Media Controls Hub */
                .controls-card {
                    background: var(--card-surface);
                    backdrop-filter: blur(30px);
                    -webkit-backdrop-filter: blur(30px);
                    border: 0.5px solid var(--card-border);
                    border-radius: 28px;
                    padding: 16px 14px;
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
                }

                .playback-row {
                    display: flex;
                    justify-content: space-around;
                    align-items: center;
                }

                .btn-tap {
                    border: none;
                    outline: none;
                    background: none;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: transform 0.15s cubic-bezier(0.175, 0.885, 0.32, 1.275), background-color 0.2s;
                }

                .btn-tap:active {
                    transform: scale(0.90);
                }

                .btn-disc-primary {
                    width: 66px;
                    height: 66px;
                    border-radius: 50%;
                    background: #FFFFFF;
                    color: #000000;
                    box-shadow: 0 6px 18px rgba(255, 255, 255, 0.2);
                }

                .btn-disc-primary:active {
                    background: #E5E5EA;
                }

                .btn-disc-secondary {
                    width: 48px;
                    height: 48px;
                    border-radius: 50%;
                    background: var(--card-surface-elevated);
                    border: 0.5px solid var(--card-border);
                    color: var(--text-primary);
                }

                .btn-disc-secondary:active {
                    background: var(--glass-pressed);
                }

                .btn-toggle {
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    background: transparent;
                    color: var(--text-secondary);
                }

                .btn-toggle.active {
                    color: var(--apple-blue);
                    background: rgba(10, 132, 255, 0.14);
                }

                /* Secondary Options (Night Mode + Sleep Timer) */
                .actions-grid {
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                    padding-top: 4px;
                    border-top: 0.5px solid var(--card-border-subtle);
                }

                .night-mode-pill {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    background: var(--glass-bg);
                    border: 0.5px solid var(--card-border);
                    border-radius: 14px;
                    padding: 10px 14px;
                    cursor: pointer;
                    transition: all 0.2s;
                }

                .night-mode-pill:active {
                    background: var(--glass-pressed);
                }

                .night-mode-pill.active {
                    background: rgba(10, 132, 255, 0.16);
                    border-color: rgba(10, 132, 255, 0.3);
                }

                .pill-left {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 13px;
                    font-weight: 600;
                    color: var(--text-primary);
                }

                .pill-badge {
                    font-size: 11px;
                    font-weight: 600;
                    color: var(--text-secondary);
                }

                .night-mode-pill.active .pill-badge {
                    color: var(--apple-blue);
                }

                /* Segmented Timer Picker */
                .timer-segmented {
                    display: flex;
                    background: rgba(0, 0, 0, 0.35);
                    padding: 3px;
                    border-radius: 12px;
                    border: 0.5px solid var(--card-border-subtle);
                    gap: 2px;
                }

                .timer-segment-btn {
                    flex: 1;
                    padding: 7px 0;
                    text-align: center;
                    font-size: 11px;
                    font-weight: 600;
                    color: var(--text-secondary);
                    border-radius: 9px;
                    border: none;
                    background: transparent;
                    cursor: pointer;
                    transition: all 0.2s ease;
                }

                .timer-segment-btn.active {
                    background: var(--card-surface-elevated);
                    color: var(--text-primary);
                    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
                }

                /* 4. Apple iOS Style Volume Control Hub */
                .volume-card {
                    background: var(--card-surface);
                    backdrop-filter: blur(30px);
                    -webkit-backdrop-filter: blur(30px);
                    border: 0.5px solid var(--card-border);
                    border-radius: 28px;
                    padding: 16px 18px;
                    display: flex;
                    flex-direction: column;
                    gap: 14px;
                    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
                }

                .volume-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }

                .volume-title-group {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 13px;
                    font-weight: 700;
                    color: var(--text-primary);
                }

                .volume-badge {
                    font-size: 12px;
                    font-weight: 700;
                    color: var(--text-primary);
                    background: var(--glass-bg);
                    border: 0.5px solid var(--card-border);
                    padding: 3px 10px;
                    border-radius: 10px;
                    min-width: 46px;
                    text-align: center;
                }

                .slider-wrapper {
                    width: 100%;
                    display: flex;
                    align-items: center;
                    padding: 4px 0;
                }

                .ios-slider {
                    -webkit-appearance: none;
                    appearance: none;
                    width: 100%;
                    height: 10px;
                    border-radius: 5px;
                    background: rgba(255, 255, 255, 0.14);
                    outline: none;
                    transition: background 0.2s;
                    cursor: pointer;
                }

                .ios-slider::-webkit-slider-thumb {
                    -webkit-appearance: none;
                    appearance: none;
                    width: 24px;
                    height: 24px;
                    border-radius: 50%;
                    background: #FFFFFF;
                    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.5);
                    cursor: pointer;
                    border: none;
                    transition: transform 0.1s ease;
                }

                .ios-slider::-webkit-slider-thumb:active {
                    transform: scale(1.2);
                }

                .ios-slider::-moz-range-thumb {
                    width: 24px;
                    height: 24px;
                    border-radius: 50%;
                    background: #FFFFFF;
                    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.5);
                    cursor: pointer;
                    border: none;
                }

                .volume-quick-actions {
                    display: flex;
                    gap: 8px;
                    justify-content: space-between;
                }

                .btn-vol-action {
                    flex: 1;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 4px;
                    padding: 9px 0;
                    border-radius: 12px;
                    background: var(--glass-bg);
                    border: 0.5px solid var(--card-border);
                    color: var(--text-primary);
                    font-size: 12px;
                    font-weight: 600;
                }

                .btn-vol-mute {
                    flex: 1.2;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 6px;
                    padding: 9px 0;
                    border-radius: 12px;
                    background: var(--glass-bg);
                    border: 0.5px solid var(--card-border);
                    color: var(--text-primary);
                    font-size: 12px;
                    font-weight: 600;
                }

                .btn-vol-mute.muted {
                    background: rgba(255, 69, 58, 0.18);
                    border-color: rgba(255, 69, 58, 0.4);
                    color: var(--apple-red);
                }

                /* 5. Playlist & Search Sheet */
                .playlist-sheet {
                    background: var(--card-surface);
                    backdrop-filter: blur(30px);
                    -webkit-backdrop-filter: blur(30px);
                    border: 0.5px solid var(--card-border);
                    border-radius: 28px;
                    padding: 18px 16px;
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
                }

                .search-field {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    background: rgba(255, 255, 255, 0.08);
                    border: 0.5px solid var(--card-border);
                    border-radius: 14px;
                    padding: 9px 12px;
                }

                .search-field input {
                    flex: 1;
                    background: none;
                    border: none;
                    outline: none;
                    font-size: 14px;
                    color: var(--text-primary);
                    text-align: right;
                }

                .search-field input::placeholder {
                    color: var(--text-tertiary);
                }

                .tracks-scroll-area {
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                    max-height: 300px;
                    overflow-y: auto;
                    padding-right: 2px;
                    -webkit-overflow-scrolling: touch;
                }

                .tracks-scroll-area::-webkit-scrollbar {
                    width: 4px;
                }
                .tracks-scroll-area::-webkit-scrollbar-thumb {
                    background: rgba(255, 255, 255, 0.12);
                    border-radius: 2px;
                }

                .track-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 10px 12px;
                    border-radius: 14px;
                    background: transparent;
                    border: 0.5px solid transparent;
                    cursor: pointer;
                    transition: all 0.15s ease;
                }

                .track-row:active {
                    background: var(--glass-pressed);
                }

                .track-row.playing {
                    background: rgba(255, 255, 255, 0.08);
                    border-color: rgba(255, 255, 255, 0.12);
                }

                .track-meta {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    flex: 1;
                    min-width: 0;
                }

                .track-num {
                    font-size: 12px;
                    font-weight: 600;
                    color: var(--text-tertiary);
                    width: 20px;
                    text-align: center;
                }

                .track-row.playing .track-num {
                    color: var(--apple-green);
                }

                .track-text-group {
                    display: flex;
                    flex-direction: column;
                    gap: 2px;
                    min-width: 0;
                    flex: 1;
                }

                .track-row-title {
                    font-size: 13.5px;
                    font-weight: 600;
                    color: var(--text-primary);
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .track-row-sub {
                    font-size: 11.5px;
                    color: var(--text-secondary);
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .btn-fav {
                    background: none;
                    border: none;
                    font-size: 18px;
                    color: var(--text-tertiary);
                    cursor: pointer;
                    padding: 4px 6px;
                    transition: transform 0.15s;
                }

                .btn-fav:active {
                    transform: scale(1.3);
                }

                .btn-fav.active {
                    color: #FFD60A;
                }

                .icon-svg {
                    fill: currentColor;
                    width: 22px;
                    height: 22px;
                }
            </style>
        </head>
        <body>
            <div class="app-wrapper">
                <!-- 1. Top Capsule -->
                <div class="top-capsule">
                    <div class="brand-info">
                        <div class="brand-icon">
                            <svg class="icon-svg" style="width:16px; height:16px;" viewBox="0 0 24 24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                        </div>
                        <span class="brand-title">تلفاز القرآن الكريم</span>
                    </div>
                    <div class="connection-badge" id="badge-status">
                        <div class="connection-dot" id="dot-status"></div>
                        <span id="txt-status">متصل</span>
                    </div>
                </div>

                <!-- 2. Hero Now Playing Card -->
                <div class="hero-card">
                    <div class="artwork-box">
                        <svg class="artwork-icon" viewBox="0 0 24 24"><path d="M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z"/></svg>
                        <div class="playing-overlay" id="wave-anim" style="display:none;">
                            <div class="wave-bar"></div>
                            <div class="wave-bar"></div>
                            <div class="wave-bar"></div>
                        </div>
                    </div>
                    
                    <div class="track-title" id="track-title">اختر تلاوة للبدء</div>
                    <div class="track-reciter" id="track-reciter">بث حي من التلفاز</div>

                    <div class="progress-wrapper">
                        <div class="progress-track">
                            <div class="progress-fill" id="progress-fill"></div>
                        </div>
                        <div class="progress-labels">
                            <span id="time-elapsed">00:00</span>
                            <span id="time-duration">00:00</span>
                        </div>
                    </div>
                </div>

                <!-- 3. Media Controls Hub -->
                <div class="controls-card">
                    <div class="playback-row">
                        <!-- Shuffle Toggle -->
                        <button class="btn-tap btn-toggle" id="btn-shuffle" title="وضع عشوائي">
                            <svg class="icon-svg" style="width:20px; height:20px;" viewBox="0 0 24 24"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.45 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
                        </button>

                        <!-- Previous -->
                        <button class="btn-tap btn-disc-secondary" id="btn-prev" title="السابق">
                            <svg class="icon-svg" viewBox="0 0 24 24"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
                        </button>

                        <!-- Play / Pause -->
                        <button class="btn-tap btn-disc-primary" id="btn-play-pause" title="تشغيل / إيقاف">
                            <svg class="icon-svg" id="play-icon" style="width:30px; height:30px;" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                            <svg class="icon-svg" id="pause-icon" style="width:30px; height:30px; display:none;" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                        </button>

                        <!-- Next -->
                        <button class="btn-tap btn-disc-secondary" id="btn-next" title="التالي">
                            <svg class="icon-svg" viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6zm9-12v12h2V6z"/></svg>
                        </button>

                        <!-- Repeat Toggle -->
                        <button class="btn-tap btn-toggle" id="btn-repeat" title="تكرار">
                            <svg class="icon-svg" style="width:20px; height:20px;" viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
                        </button>
                    </div>

                    <div class="actions-grid">
                        <!-- Night Audio Mode -->
                        <div class="night-mode-pill" id="btn-night-mode">
                            <div class="pill-left">
                                <svg class="icon-svg" style="width:16px; height:16px;" viewBox="0 0 24 24"><path d="M12.3 22h-.1c-5.5 0-10-4.5-10-10C2.2 6.8 6.5 2.5 11.7 2.2c.5 0 .9.3 1.1.7.2.4.1.9-.2 1.2-1.9 2-2.1 5.2-.4 7.4 1.7 2.3 4.9 2.9 7.3 1.4.4-.2.8-.2 1.1 0 .3.2.5.6.5 1 0 4.5-3.6 8.1-8.3 8.1zm-1-17.8c-3.9.5-6.9 3.8-6.9 7.8 0 4.4 3.6 8 8 8 3.5 0 6.5-2.3 7.6-5.5-2.2.4-4.5-.3-6-2.2-2.1-2.4-2.2-5.9-.7-8.1z"/></svg>
                                <span>نمط الاستماع الليلي</span>
                            </div>
                            <span class="pill-badge" id="night-mode-badge">معطل</span>
                        </div>

                        <!-- Sleep Timer Segmented -->
                        <div class="timer-segmented">
                            <button class="timer-segment-btn active" id="timer-off" onclick="setTimer(null)">إيقاف</button>
                            <button class="timer-segment-btn" id="timer-15" onclick="setTimer(15)">15 د</button>
                            <button class="timer-segment-btn" id="timer-30" onclick="setTimer(30)">30 د</button>
                            <button class="timer-segment-btn" id="timer-45" onclick="setTimer(45)">45 د</button>
                            <button class="timer-segment-btn" id="timer-60" onclick="setTimer(60)">ساعة</button>
                        </div>
                    </div>
                </div>

                <!-- 4. Apple iOS Style Volume Control Hub -->
                <div class="volume-card">
                    <div class="volume-header">
                        <div class="volume-title-group">
                            <svg class="icon-svg" id="vol-icon" style="width:18px; height:18px;" viewBox="0 0 24 24">
                                <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
                            </svg>
                            <span>مستوى الصوت</span>
                        </div>
                        <span class="volume-badge" id="vol-badge">100%</span>
                    </div>

                    <div class="slider-wrapper">
                        <input type="range" id="vol-slider" min="0" max="100" value="100" class="ios-slider" oninput="onVolumeSliderInput(this.value)" onchange="onVolumeSliderChange(this.value)">
                    </div>

                    <div class="volume-quick-actions">
                        <button class="btn-tap btn-vol-action" onclick="adjustVolume(-10)" title="خفض الصوت 10%">
                            <svg class="icon-svg" style="width:15px; height:15px;" viewBox="0 0 24 24"><path d="M19 13H5v-2h14v2z"/></svg>
                            <span>-10%</span>
                        </button>
                        <button class="btn-tap btn-vol-mute" id="btn-mute" onclick="toggleMute()" title="كتم / إلغاء الكتم">
                            <svg class="icon-svg" id="mute-icon-svg" style="width:16px; height:16px;" viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27l4.73 4.73H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>
                            <span id="mute-txt">كتم</span>
                        </button>
                        <button class="btn-tap btn-vol-action" onclick="adjustVolume(10)" title="رفع الصوت 10%">
                            <svg class="icon-svg" style="width:15px; height:15px;" viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                            <span>+10%</span>
                        </button>
                    </div>
                </div>

                <!-- 5. Playlist & Search Sheet -->
                <div class="playlist-sheet">
                    <div class="search-field">
                        <svg class="icon-svg" style="width:17px; height:17px; color:var(--text-tertiary);" viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                        <input type="text" id="inp-search" placeholder="ابحث باسم السورة، القارئ..." oninput="onSearchInput()">
                    </div>

                    <div class="tracks-scroll-area" id="playlist-box">
                        <!-- Loaded dynamically via JSON -->
                    </div>
                </div>
            </div>

            <script>
                let tracks = [];
                let currentTrackId = null;
                let isDraggingVolume = false;
                let volumeDebounceTimer = null;

                function sendCommand(action, params = {}) {
                    let url = '/api/command?action=' + action;
                    for (let key in params) {
                        url += '&' + key + '=' + encodeURIComponent(params[key]);
                    }
                    fetch(url)
                        .then(res => res.json())
                        .catch(err => console.error('Command error:', err));
                }

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

                /* Volume Control Functions */
                function onVolumeSliderInput(val) {
                    isDraggingVolume = true;
                    const num = parseInt(val, 10);
                    document.getElementById('vol-badge').innerText = num + '%';
                    updateVolumeIcon(num, num === 0);

                    clearTimeout(volumeDebounceTimer);
                    volumeDebounceTimer = setTimeout(() => {
                        sendCommand('set_volume', { vol: num });
                    }, 40);
                }

                function onVolumeSliderChange(val) {
                    isDraggingVolume = false;
                    const num = parseInt(val, 10);
                    sendCommand('set_volume', { vol: num });
                }

                function adjustVolume(delta) {
                    sendCommand('adjust_volume', { delta: delta });
                }

                function toggleMute() {
                    sendCommand('toggle_mute');
                }

                function updateVolumeIcon(vol, isMuted) {
                    const iconSvg = document.getElementById('vol-icon');
                    const muteBtn = document.getElementById('btn-mute');
                    const muteTxt = document.getElementById('mute-txt');

                    if (isMuted || vol === 0) {
                        muteBtn.classList.add('muted');
                        muteTxt.innerText = 'إلغاء الكتم';
                        iconSvg.innerHTML = '<path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27l4.73 4.73H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>';
                    } else {
                        muteBtn.classList.remove('muted');
                        muteTxt.innerText = 'كتم';
                        if (vol < 45) {
                            iconSvg.innerHTML = '<path d="M18.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM5 9v6h4l5 5V4L9 9H5z"/>';
                        } else {
                            iconSvg.innerHTML = '<path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>';
                        }
                    }
                }

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

                    if (filtered.length === 0) {
                        box.innerHTML = '<div style="text-align:center; padding:24px 0; color:var(--text-tertiary); font-size:12px;">لا توجد نتائج مطابقة</div>';
                        return;
                    }

                    filtered.forEach((t, idx) => {
                        const isPlayingThis = t.id === currentTrackId;
                        const isFav = t.isFavorite;
                        const row = document.createElement('div');
                        row.className = 'track-row' + (isPlayingThis ? ' playing' : '');
                        row.onclick = () => playTrack(t.id);

                        row.innerHTML = `
                            <div class="track-meta">
                                <span class="track-num">${'$'}{isPlayingThis ? '▶' : (idx + 1)}</span>
                                <div class="track-text-group">
                                    <div class="track-row-title">${'$'}{t.title}</div>
                                    <div class="track-row-sub">${'$'}{t.reciterOrSubtitle}</div>
                                </div>
                            </div>
                            <button class="btn-fav ${'$'}{isFav ? 'active' : ''}" onclick="toggleFavorite('${'$'}{t.filePath.replace(/'/g, "\\'")}', event)">
                                ${'$'}{isFav ? '★' : '☆'}
                            </button>
                        `;
                        box.appendChild(row);
                    });
                }

                function pollStatus() {
                    fetch('/api/status')
                        .then(res => res.json())
                        .then(status => {
                            // Connection status
                            document.getElementById('txt-status').innerText = 'متصل';
                            document.getElementById('badge-status').style.color = 'var(--apple-green)';
                            document.getElementById('badge-status').style.background = 'rgba(48, 209, 88, 0.12)';
                            document.getElementById('dot-status').style.background = 'var(--apple-green)';
                            document.getElementById('dot-status').style.boxShadow = '0 0 6px var(--apple-green)';

                            // Play / Pause Icon & Wave animation
                            const playIcon = document.getElementById('play-icon');
                            const pauseIcon = document.getElementById('pause-icon');
                            const waveAnim = document.getElementById('wave-anim');

                            if (status.isPlaying) {
                                playIcon.style.display = 'none';
                                pauseIcon.style.display = 'block';
                                waveAnim.style.display = 'flex';
                            } else {
                                playIcon.style.display = 'block';
                                pauseIcon.style.display = 'none';
                                waveAnim.style.display = 'none';
                            }

                            // Track Title / Reciter
                            if (status.currentTrack) {
                                document.getElementById('track-title').innerText = status.currentTrack.title;
                                document.getElementById('track-reciter').innerText = status.currentTrack.reciterOrSubtitle;
                                currentTrackId = status.currentTrack.id;
                            } else {
                                document.getElementById('track-title').innerText = 'اختر تلاوة للبدء';
                                document.getElementById('track-reciter').innerText = 'بث حي من التلفاز';
                                currentTrackId = null;
                            }

                            // Progress
                            const elapsed = status.currentPositionMs;
                            const duration = status.durationMs;
                            const pct = Math.min(100, Math.max(0, status.progress * 100));
                            document.getElementById('progress-fill').style.width = pct + '%';
                            document.getElementById('time-elapsed').innerText = formatDuration(elapsed);
                            document.getElementById('time-duration').innerText = formatDuration(duration);

                            // Toggle Buttons Active Status
                            toggleButtonActive('btn-shuffle', status.isShuffle);
                            toggleButtonActive('btn-repeat', status.repeatMode !== 'OFF');

                            // Night Mode Pill
                            const nightPill = document.getElementById('btn-night-mode');
                            const nightBadge = document.getElementById('night-mode-badge');
                            if (status.isNightMode) {
                                nightPill.classList.add('active');
                                nightBadge.innerText = 'مفعل';
                            } else {
                                nightPill.classList.remove('active');
                                nightBadge.innerText = 'معطل';
                            }

                            // Sleep Timer Segmented Control
                            document.querySelectorAll('.timer-segment-btn').forEach(btn => btn.classList.remove('active'));
                            if (status.sleepTimerMinutes) {
                                const activeSegment = document.getElementById('timer-' + status.sleepTimerMinutes);
                                if (activeSegment) activeSegment.classList.add('active');
                            } else {
                                document.getElementById('timer-off').classList.add('active');
                            }

                            // Volume Sync (only if user is not actively dragging the slider)
                            if (!isDraggingVolume && status.volumePercent !== undefined) {
                                const v = status.volumePercent;
                                document.getElementById('vol-slider').value = v;
                                document.getElementById('vol-badge').innerText = v + '%';
                                updateVolumeIcon(v, status.isMuted);
                            }

                            // Highlight active item in playlist
                            highlightPlayingTrack();
                        })
                        .catch(err => {
                            document.getElementById('txt-status').innerText = 'انقطع الاتصال';
                            document.getElementById('badge-status').style.color = 'var(--apple-red)';
                            document.getElementById('badge-status').style.background = 'rgba(255, 69, 58, 0.12)';
                            document.getElementById('dot-status').style.background = 'var(--apple-red)';
                            document.getElementById('dot-status').style.boxShadow = '0 0 6px var(--apple-red)';
                        });
                }

                function toggleButtonActive(id, isActive) {
                    const btn = document.getElementById(id);
                    if (isActive) {
                        btn.classList.add('active');
                    } else {
                        btn.classList.remove('active');
                    }
                }

                function highlightPlayingTrack() {
                    const rows = document.querySelectorAll('.track-row');
                    rows.forEach(r => r.classList.remove('playing'));
                }

                function formatDuration(ms) {
                    if (!ms || isNaN(ms) || ms < 0) return '00:00';
                    let totalSec = Math.floor(ms / 1000);
                    let min = Math.floor(totalSec / 60);
                    let sec = totalSec % 60;
                    return (min < 10 ? '0' + min : min) + ':' + (sec < 10 ? '0' + sec : sec);
                }

                function loadTracks() {
                    fetch('/api/tracks')
                        .then(res => res.json())
                        .then(data => {
                            tracks = data;
                            filterLocalPlaylist(document.getElementById('inp-search').value);
                        })
                        .catch(err => console.error('Failed to load tracks list:', err));
                }

                setInterval(pollStatus, 1000);
                setInterval(loadTracks, 3000);
                pollStatus();
                loadTracks();
            </script>
        </body>
        </html>
    """.trimIndent()

}
