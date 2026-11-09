package dev.cinderhell

import android.media.AudioManager

/** Audio ownership never controls whether a foreground game can render. */
internal class GameAudioState(private val onMuteChanged: (Boolean) -> Unit) {
    var canPlay = false
        private set

    fun resume(mode: Int) {
        canPlay = true
        onMuteChanged(mode != AudioManager.MODE_NORMAL)
    }

    fun modeChanged(mode: Int) {
        if (canPlay) onMuteChanged(mode != AudioManager.MODE_NORMAL)
    }

    fun pause() {
        canPlay = false
        onMuteChanged(true)
    }
}
