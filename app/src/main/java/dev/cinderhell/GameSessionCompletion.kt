package dev.cinderhell

/** Coordinates the UI/native threads without publishing two terminal results. */
internal class GameSessionCompletion(private val terminate: () -> Unit) {
    private var resultWritten = false
    private var destroyed = false
    private var nativeDone = false
    private var terminated = false

    @Synchronized
    fun publishResult(write: () -> Unit) {
        if (resultWritten) return
        write()
        resultWritten = true
    }

    @Synchronized
    fun nativeFinished() {
        nativeDone = true
        terminateIfFinished()
    }

    @Synchronized
    fun activityDestroyed(nativeStarted: Boolean) {
        destroyed = true
        if (!nativeStarted) nativeDone = true
        terminateIfFinished()
    }

    private fun terminateIfFinished() {
        if (destroyed && nativeDone && !terminated) {
            terminated = true
            terminate()
        }
    }
}
