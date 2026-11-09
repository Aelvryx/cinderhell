package dev.cinderhell

import org.junit.Assert.*
import org.junit.Test

class GameSessionCompletionTest {
    @Test fun nativeCompletionAfterActivityDestructionStillTerminatesProcess() {
        var terminations = 0
        val completion = GameSessionCompletion { terminations++ }
        completion.activityDestroyed(nativeStarted = true)
        assertEquals(0, terminations)
        completion.nativeFinished()
        assertEquals(1, terminations)
    }

    @Test fun earlyCompletionWaitsForActivityTeardown() {
        var terminations = 0
        val completion = GameSessionCompletion { terminations++ }
        completion.nativeFinished()
        assertEquals(0, terminations)
        completion.activityDestroyed(nativeStarted = true)
        assertEquals(1, terminations)
    }

    @Test fun cancelledStartupTerminatesWithoutNativeCompletion() {
        var terminations = 0
        val completion = GameSessionCompletion { terminations++ }
        completion.activityDestroyed(nativeStarted = false)
        completion.nativeFinished()
        assertEquals(1, terminations)
    }

    @Test fun firstTerminalResultSurvivesLaterNativeCompletion() {
        val results = mutableListOf<String>()
        val completion = GameSessionCompletion {}
        completion.publishResult { results += "audio unavailable" }
        completion.publishResult { results += "generic interruption" }
        assertEquals(listOf("audio unavailable"), results)
    }
}
