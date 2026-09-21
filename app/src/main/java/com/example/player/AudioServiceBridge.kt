package com.example.player

/**
 * Lightweight, zero-overhead bridge between QuranAudioService and QuranPlayerViewModel.
 * Avoids complex IPC and keeps RAM usage at absolute zero on 1GB TV devices.
 */
object AudioServiceBridge {
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null
}
