package dev.cinderhell.session

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun pendingSessionIsConsumedExactlyOnceAndResultIsConsumedExactlyOnce() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())

        assertEquals(descriptor, fixture.store.consumePending(descriptor.sessionId))
        val replay = runCatching {
            fixture.store.consumePending(descriptor.sessionId)
        }.exceptionOrNull()
        assertTrue(replay is SessionValidationException)

        fixture.store.recordResult(
            cleanResult(descriptor, fixture.now),
        )
        assertEquals(
            listOf(SessionOutcome.CLEAN_EXIT),
            fixture.store.consumeResults().map(GameSessionResult::outcome),
        )
        assertTrue(fixture.store.consumeResults().isEmpty())
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun expiredAndForgedRequestsAreRejected() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        fixture.now += SESSION_TTL_MILLIS + 1

        val expired = runCatching {
            fixture.store.consumePending(descriptor.sessionId)
        }.exceptionOrNull() as SessionValidationException
        assertEquals(SessionFailureCode.EXPIRED_REQUEST, expired.code)

        val outside = temporaryFolder.newFile("outside.wad").apply {
            writeText("not private")
        }
        val forged = fixture.request().copy(
            orderedContent = listOf(
                fixture.content.copy(path = outside.absolutePath),
            ),
        )
        assertTrue(
            runCatching { fixture.store.createPending(forged) }.exceptionOrNull()
                is SessionValidationException,
        )
    }

    @Test
    fun orphanedActiveSessionBecomesARecoverableInterruptedResult() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        fixture.store.consumePending(descriptor.sessionId)

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        val result = fixture.store.consumeResults().single()
        assertEquals(SessionOutcome.INTERRUPTED, result.outcome)
        assertEquals(SessionFailureCode.PROCESS_LOST, result.failureCode)
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun orphanedPendingSessionIsRecoveredBeforeExpiryAndAllowsImmediateRelaunch() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        val save = File(descriptor.saveDirectory).resolve("save0.dsg").apply {
            writeText("saved progress")
        }
        val config = File(descriptor.configPath).apply { writeText("player settings") }
        val screenshot = File(descriptor.screenshotDirectory).resolve("shot.png").apply {
            writeText("screenshot")
        }

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        assertFalse(fixture.store.hasPendingOrActiveSession())
        val result = fixture.store.consumeResults().single()
        assertEquals(descriptor.sessionId, result.sessionId)
        assertEquals(descriptor.nonce, result.nonce)
        assertEquals(descriptor.profileId, result.profileId)
        assertEquals(fixture.now, result.completedAtEpochMillis)
        assertEquals(SessionOutcome.INTERRUPTED, result.outcome)
        assertEquals(SessionFailureCode.PROCESS_LOST, result.failureCode)
        assertEquals("saved progress", save.readText())
        assertEquals("player settings", config.readText())
        assertEquals("screenshot", screenshot.readText())
        assertEquals("IWAD fixture", File(fixture.content.path).readText())
        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
        assertTrue(fixture.store.consumeResults().isEmpty())
        fixture.store.createPending(fixture.request())
    }

    @Test
    fun runningProcessPreservesPendingAndActiveDescriptorsEvenAfterExpiry() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val descriptor = fixture.store.createPending(fixture.request())
            if (active) fixture.store.consumePending(descriptor.sessionId)
            val file = if (active) fixture.paths.activeSession(descriptor.sessionId)
            else fixture.paths.pendingSession(descriptor.sessionId)
            val original = file.readText()
            fixture.now += SESSION_TTL_MILLIS + 1

            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = true)

            assertEquals(original, file.readText())
            assertTrue(fixture.store.hasPendingOrActiveSession())
            assertTrue(fixture.store.consumeResults().isEmpty())
        }
    }

    @Test
    fun malformedOrphanDescriptorsDoNotBlockRecoveryOfOtherSessions() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val descriptor = fixture.store.createPending(fixture.request())
            val directory = if (active) fixture.paths.activeSessions else fixture.paths.pendingSessions
            val invalidDescriptors = listOf(
                "not json",
                Json.encodeToString(descriptor.copy(sessionId = "../outside")),
                Json.encodeToString(descriptor.copy(sessionId = "2".padStart(32, '0'), nonce = "invalid")),
                Json.encodeToString(descriptor.copy(sessionId = "3".padStart(32, '0'), profileId = "../outside")),
                Json.encodeToString(descriptor.copy(sessionId = "4".padStart(32, '0'), schemaVersion = 99)),
                Json.encodeToString(descriptor.copy(sessionId = "5".padStart(32, '0'), nonce = "")),
            )
            invalidDescriptors.forEachIndexed { index, value ->
                directory.resolve("${index.toString().padStart(32, '0')}.json").writeText(value)
            }

            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

            assertFalse(fixture.store.hasPendingOrActiveSession())
            assertEquals(listOf(descriptor.sessionId), fixture.store.consumeResults().map { it.sessionId })
        }
    }

    @Test
    fun mismatchedDescriptorCannotOverwriteAnotherSessionsResult() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val descriptor = fixture.store.createPending(fixture.request())
            fixture.store.consumePending(descriptor.sessionId)
            val expected = cleanResult(descriptor, fixture.now)
            fixture.store.recordResult(expected)
            val directory = if (active) fixture.paths.activeSessions else fixture.paths.pendingSessions
            val mismatched = directory.resolve("${"0".repeat(32)}.json")
            mismatched.writeText(Json.encodeToString(descriptor))

            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

            assertFalse(mismatched.exists())
            assertEquals(listOf(expected), fixture.store.consumeResults())
            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
            assertTrue(fixture.store.consumeResults().isEmpty())
        }
    }

    @Test
    fun recoveryPreservesAlreadyPublishedResultsAndRemovesLeftoverDescriptors() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val descriptor = fixture.store.createPending(fixture.request())
            val original = fixture.paths.pendingSession(descriptor.sessionId).readText()
            fixture.store.consumePending(descriptor.sessionId)
            val expected = cleanResult(descriptor, fixture.now)
            fixture.store.recordResult(expected)
            val leftover = if (active) fixture.paths.activeSession(descriptor.sessionId)
            else fixture.paths.pendingSession(descriptor.sessionId)
            leftover.writeText(original)

            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

            assertFalse(fixture.store.hasPendingOrActiveSession())
            assertEquals(listOf(expected), fixture.store.consumeResults())
            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
            assertTrue(fixture.store.consumeResults().isEmpty())
        }
    }

    @Test
    fun duplicatePendingAndActiveDescriptorsProduceOnlyOneInterruptedResult() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        val original = fixture.paths.pendingSession(descriptor.sessionId).readText()
        fixture.store.consumePending(descriptor.sessionId)
        fixture.paths.pendingSession(descriptor.sessionId).writeText(original)

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        assertFalse(fixture.store.hasPendingOrActiveSession())
        assertEquals(listOf(SessionOutcome.INTERRUPTED), fixture.store.consumeResults().map { it.outcome })
        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
        assertTrue(fixture.store.consumeResults().isEmpty())
    }

    @Test
    fun recoveryPreservesStartupFailureResult() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        val original = fixture.paths.pendingSession(descriptor.sessionId).readText()
        fixture.store.recordStartupFailure(
            sessionId = descriptor.sessionId,
            nonce = descriptor.nonce,
            profileId = descriptor.profileId,
            code = SessionFailureCode.NATIVE_STARTUP_FAILURE,
            userMessage = "Could not start",
        )
        fixture.paths.pendingSession(descriptor.sessionId).writeText(original)

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        assertFalse(fixture.store.hasPendingOrActiveSession())
        val result = fixture.store.consumeResults().single()
        assertEquals(SessionOutcome.STARTUP_FAILURE, result.outcome)
        assertEquals(SessionFailureCode.NATIVE_STARTUP_FAILURE, result.failureCode)
        assertEquals("Could not start", result.userMessage)
        assertTrue(fixture.store.consumeResults().isEmpty())
    }

    @Test
    fun orphanRecoveryDoesNotRequireGameContentToStillExist() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val descriptor = fixture.store.createPending(fixture.request())
            if (active) fixture.store.consumePending(descriptor.sessionId)
            assertTrue(File(fixture.content.path).delete())

            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

            assertFalse(fixture.store.hasPendingOrActiveSession())
            assertEquals(SessionFailureCode.PROCESS_LOST, fixture.store.consumeResults().single().failureCode)
        }
    }

    @Test
    fun launchWinningQueuedRecoveryClearsOldOrphanAndPreservesNewRequest() {
        for (active in listOf(false, true)) {
            val fixture = fixture()
            val guard = SessionLaunchGuard()
            val orphan = fixture.store.createPending(fixture.request())
            if (active) fixture.store.consumePending(orphan.sessionId)
            val queuedRecovery = guard.recoveryTicket()
            val launch = guard.beginLaunch {
                fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
            }
            val recover = {
                fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
                fixture.store.consumeResults()
            }

            guard.recoverIfIdle(queuedRecovery, recover)
            val next = fixture.store.createPending(fixture.request())
            guard.endLaunch(launch)
            guard.recoverIfIdle(queuedRecovery, recover)

            assertTrue(fixture.paths.pendingSession(next.sessionId).isFile)
            assertTrue(fixture.paths.sessionResult(orphan.sessionId).isFile)
            assertFalse(fixture.paths.sessionResult(next.sessionId).exists())
            assertFalse(fixture.paths.pendingSession(orphan.sessionId).exists())
            assertFalse(fixture.paths.activeSession(orphan.sessionId).exists())
            val results = guard.recoverIfIdle(guard.recoveryTicket()) {
                fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = true)
                fixture.store.consumeResults()
            }
            val recovered = checkNotNull(results).single()
            assertEquals(orphan.sessionId, recovered.sessionId)
            assertEquals(orphan.profileId, recovered.profileId)
            assertEquals(SessionOutcome.INTERRUPTED, recovered.outcome)
            assertEquals(SessionFailureCode.PROCESS_LOST, recovered.failureCode)
            assertEquals(next, fixture.store.consumePending(next.sessionId))
            assertTrue(fixture.store.consumeResults().isEmpty())
        }
    }

    @Test
    fun recoveryQueuedBeforeOrDuringLaunchCannotRemoveTheNewPendingSession() {
        val fixture = fixture()
        val guard = SessionLaunchGuard()
        val beforeLaunch = guard.recoveryTicket()
        val launch = guard.beginLaunch()
        val descriptor = fixture.store.createPending(fixture.request())
        val duringLaunch = guard.recoveryTicket()
        val recover = {
            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
            fixture.store.consumeResults()
        }

        guard.recoverIfIdle(beforeLaunch, recover)
        guard.endLaunch(launch)
        guard.recoverIfIdle(beforeLaunch, recover)
        guard.recoverIfIdle(duringLaunch, recover)

        assertTrue(fixture.paths.pendingSession(descriptor.sessionId).isFile)
        assertTrue(fixture.store.consumeResults().isEmpty())
        val recovered = guard.recoverIfIdle(guard.recoveryTicket(), recover)
        assertEquals(listOf(descriptor.sessionId), recovered?.map { it.sessionId })
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun failedLaunchReleasesRecoveryGuardWithoutReleasingAnotherLaunch() {
        val fixture = fixture()
        val guard = SessionLaunchGuard()
        val firstLaunch = guard.beginLaunch()
        guard.endLaunch(firstLaunch)
        val nextLaunch = guard.beginLaunch()
        val descriptor = fixture.store.createPending(fixture.request())
        guard.endLaunch(firstLaunch)
        val recoveryWhileLaunching = guard.recoveryTicket()
        guard.endLaunch(nextLaunch)
        val recover = {
            fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)
            fixture.store.consumeResults()
        }

        guard.recoverIfIdle(recoveryWhileLaunching, recover)

        assertTrue(fixture.paths.pendingSession(descriptor.sessionId).isFile)
        assertEquals(
            listOf(descriptor.sessionId),
            guard.recoverIfIdle(guard.recoveryTicket(), recover)?.map { it.sessionId },
        )
    }

    @Test
    fun taskSpecificOldTemporaryFilesAreCleaned() {
        val fixture = fixture()
        val part = fixture.paths.pendingSessions.resolve(".session.part").apply {
            writeText("incomplete")
            setLastModified(fixture.now - SESSION_TTL_MILLIS - 1)
        }

        fixture.store.cleanupTemporaryAndExpiredFiles()

        assertFalse(part.exists())
    }

    private fun fixture(): Fixture {
        val root = temporaryFolder.newFolder("app-${System.nanoTime()}")
        val paths = AppPaths(root)
        paths.ensureDirectories()
        val game = paths.runtime.resolve("freedoom2.wad").apply {
            parentFile?.mkdirs()
            writeText("IWAD fixture")
        }
        var currentTime = 1_700_000_000_000L
        val store = SessionStore(paths, now = { currentTime })
        return Fixture(
            paths = paths,
            store = store,
            content = SessionContent(
                contentId = "freedoom-fixture",
                role = ContentRole.GAME,
                path = game.canonicalPath,
                sha256 = "a".repeat(64),
            ),
            getNow = { currentTime },
            setNow = { currentTime = it },
        )
    }

    private fun cleanResult(
        descriptor: GameSessionDescriptor,
        now: Long,
    ) = GameSessionResult(
        sessionId = descriptor.sessionId,
        nonce = descriptor.nonce,
        completedAtEpochMillis = now,
        outcome = SessionOutcome.CLEAN_EXIT,
    )

    private class Fixture(
        val paths: AppPaths,
        val store: SessionStore,
        val content: SessionContent,
        private val getNow: () -> Long,
        private val setNow: (Long) -> Unit,
    ) {
        var now: Long
            get() = getNow()
            set(value) = setNow(value)

        fun request(profileId: String = "profile-one"): SessionRequest {
            val config = paths.profileConfig(profileId)
            checkNotNull(config.parentFile).mkdirs()
            val saves = paths.profileSaves(profileId).apply(File::mkdirs)
            val screenshots = paths.profileScreenshots(profileId).apply(File::mkdirs)
            return SessionRequest(
                profileId = profileId,
                presetVersion = 1,
                orderedContent = listOf(content),
                configPath = config.canonicalPath,
                saveDirectory = saves.canonicalPath,
                screenshotDirectory = screenshots.canonicalPath,
            )
        }
    }
}
