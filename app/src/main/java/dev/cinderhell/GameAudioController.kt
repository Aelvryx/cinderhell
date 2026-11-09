package dev.cinderhell

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Mix with other media; mute only this game's output during calls/backgrounding. */
internal class GameAudioController(context: Context, onMuteChanged: (Boolean) -> Unit) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var lastMuted: Boolean? = null
    private val state = GameAudioState { muted ->
        if (lastMuted != muted) {
            lastMuted = muted
            Log.i(TAG, "Game audio muted: $muted")
            onMuteChanged(muted)
        }
    }
    private var modeListener: AudioManager.OnModeChangedListener? = null
    val canPlay: Boolean get() = state.canPlay

    private val pollMode = object : Runnable {
        override fun run() {
            if (!state.canPlay) return
            state.modeChanged(audioManager.mode)
            handler.postDelayed(this, 250)
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= 31 && modeListener == null) {
            val listener = AudioManager.OnModeChangedListener {
                // Read current mode instead of trusting an old queued callback.
                state.modeChanged(audioManager.mode)
            }
            modeListener = listener
            audioManager.addOnModeChangedListener({ handler.post(it) }, listener)
        }
        state.resume(audioManager.mode)
        if (Build.VERSION.SDK_INT < 31) {
            handler.removeCallbacks(pollMode)
            handler.postDelayed(pollMode, 250)
        }
    }

    fun stop() {
        state.pause()
        handler.removeCallbacks(pollMode)
        if (Build.VERSION.SDK_INT >= 31) {
            modeListener?.let(audioManager::removeOnModeChangedListener)
            modeListener = null
        }
    }

    private companion object { const val TAG = "CinderhellAudio" }
}
