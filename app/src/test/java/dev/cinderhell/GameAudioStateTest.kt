package dev.cinderhell

import android.media.AudioManager
import org.junit.Assert.*
import org.junit.Test

class GameAudioStateTest {
    @Test fun normalAudioAllowsMixingWithoutBlockingGameplay() {
        val changes = mutableListOf<Boolean>()
        val state = GameAudioState(changes::add)
        state.resume(AudioManager.MODE_NORMAL)
        assertTrue(state.canPlay)
        assertEquals(listOf(false), changes)
    }

    @Test fun startingDuringACallMutesAudioButAllowsGameplay() {
        val changes = mutableListOf<Boolean>()
        val state = GameAudioState(changes::add)
        state.resume(AudioManager.MODE_IN_CALL)
        assertTrue(state.canPlay)
        assertEquals(listOf(true), changes)
        state.modeChanged(AudioManager.MODE_NORMAL)
        assertTrue(state.canPlay)
        assertEquals(listOf(true, false), changes)
    }

    @Test fun incomingAndVoipCallsMuteWithoutPausingTheForegroundGame() {
        for (mode in listOf(AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_CALL_SCREENING,
            AudioManager.MODE_CALL_REDIRECT, AudioManager.MODE_COMMUNICATION_REDIRECT)) {
            val changes = mutableListOf<Boolean>()
            val state = GameAudioState(changes::add)
            state.resume(AudioManager.MODE_NORMAL)
            state.modeChanged(mode)
            assertTrue(state.canPlay)
            assertEquals(listOf(false, true), changes)
        }
    }

    @Test fun endingCallInBackgroundCannotUnmuteOrResumeGameplay() {
        val changes = mutableListOf<Boolean>()
        val state = GameAudioState(changes::add)
        state.resume(AudioManager.MODE_IN_CALL)
        state.pause()
        state.modeChanged(AudioManager.MODE_NORMAL)
        assertFalse(state.canPlay)
        assertTrue(changes.all { it })
        state.resume(AudioManager.MODE_NORMAL)
        assertTrue(state.canPlay)
        assertFalse(changes.last())
    }

    @Test fun backgroundingNormalPlaybackMutesAndStopsGameplay() {
        val changes = mutableListOf<Boolean>()
        val state = GameAudioState(changes::add)
        state.resume(AudioManager.MODE_NORMAL)
        state.pause()
        assertEquals(listOf(false, true), changes)
        assertFalse(state.canPlay)
    }
}
