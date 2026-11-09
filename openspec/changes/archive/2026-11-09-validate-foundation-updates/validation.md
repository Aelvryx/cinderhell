# Validation evidence

## Upgrade baseline

On 2026-11-09, the official preview 3 (1000003) was running on the AYN Odin2
Portal, Android 13. A separate profile named `R` was created for validation,
without editing the existing Freedoom profile. A MAP01 save was created in its
empty first manual slot. The load menu showed that save, Quit returned
SDL_main status 0 and terminated the game process, and the launcher displayed
the save under Continue. Baseline screenshots are retained locally in build/.

## Platform-independent checks

- Bootstrap invariants passed; all three Python release-version tests passed.
- The corpus generator initially produced a CRLF checksum manifest on Windows.
  Writing its UTF-8 bytes explicitly fixed the observed failure: all nine
  generated files then passed Git Bash sha256sum --check without normalization.
- On the Odin, native OpenAL loopback tests passed: default mute, independent
  call/window reasons, actual stereo PCM silence/restoration, deferred updates,
  context reinitialization, and 100 teardown races (1,634,295 UI updates).
- The Dependabot audit confirmed one quarterly foundation group and the
  configured cooldowns. GitHub reported both security alerts and automated
  security updates enabled. The schedule/grouping configuration was retained.

## Frozen dependency batch

The single recreate request completed both ecosystem jobs. The Actions job
deferred the existing group, while the Gradle job selected newer stable
versions but did not update PR #11. The maintainer integration therefore kept
the reviewed Actions pins, merged current main, and carried the versions from
the successful Gradle scan manually: Gradle 9.8.2, AGP 9.5.1, Kotlin 2.4.21,
KSP 2.3.13, core-ktx 1.19.1, Navigation 2.10.2, Compose BOM 2026.09.00,
Room 2.8.5, and serialization 1.12.0. Review then identified KSP 2.3.13's
documented concurrent-execution regression (google/ksp#3286). The final batch
retains KSP 2.3.12 and excludes only 2.3.13 in Dependabot.

The wrapper JAR matched Gradle's published SHA-256
`3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`.
The wrapper properties also pin the official distribution checksum.

The final debug APK and instrumentation APK passed all 13 AndroidJUnitRunner
tests on the Odin, including launcher/controller navigation, library/schema
behavior, and session protocol recovery. The separate debug packages were used;
the official preview 3 data remained intact for the signed upgrade gate.

The final Windows build passed `testDebugUnitTest lintDebug assembleDebug
assembleRelease assemblePreview assembleDebugAndroidTest`: 53 JVM tests,
zero failures/errors; lint had zero errors and 10 warnings. All three app APKs
passed arm64/native/assets/legal-content inspection and the local preview
signature verified. OpenSpec strict validation passed all seven active items.
Independent compatibility/diff review found no remaining blockers after the
KSP exception. The initial lint attempt encountered a stale Windows JAR lock
held by a previous Gradle daemon; stopping both Gradle versions resolved it.

The signed preview 4 artifact and post-upgrade device results will be recorded
in the release notes after the signing workflow and physical publication gate.
