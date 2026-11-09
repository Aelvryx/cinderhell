# Physical lifecycle gate

Verified on 2026-07-25 using an AYN Thor running Android 13. The debug build
used the pinned native/runtime revisions in `third_party/dependencies.lock.toml`.

## Results

- Background/foreground: pressing Home caused SDL `onPause`, `nativePause`,
  `surfaceDestroyed`, and audio-focus abandonment. Returning through Recents
  caused `surfaceCreated`, `nativeResume`, and a new focus grant. The same
  `:game` PID remained active and no second engine thread was created.
- Screen off/on: the display cycle produced the same pause/destroy and
  create/resume sequence. Rendering resumed in the existing game process.
- Audio interruption: the debug-only transient-focus probe produced
  `AUDIOFOCUS_LOSS_TRANSIENT`, `nativePause`, `AUDIOFOCUS_GAIN`, and
  `nativeResume` while `GameActivity` remained foreground.
- Process death: killing only the `:game` PID removed the activity, returned to
  the still-running launcher, converted the orphaned active descriptor into an
  interrupted result, persisted `diagnostics/latest.json`, and displayed:
  “The previous game session stopped unexpectedly. Your saves are still
  available.”
- Android Back: a hardware Back key was translated before SDL surface
  dispatch and opened the Woof menu. Selecting and confirming Quit returned
  from `SDL_main` with status 0, removed the `:game` process, restored launcher
  system bars, and displayed “Game closed safely.”
- High refresh: the activity requested the device's supported 120.00001 Hz
  mode and `dumpsys display` reported active mode 2 at 120.00001 Hz.
  SurfaceFlinger recorded 126 consecutive BLAST presentation samples with a
  mean interval of 8.323 ms (7.563–9.097 ms) against an 8.333 ms refresh
  period.

The release manifest contains no lifecycle-test receiver. The transient audio
focus probe is declared only by `app/src/debug/AndroidManifest.xml`.

## Issue #13 regression checks

The change separates foreground gameplay from audio output: game music and SFX
mix with other media, mute during calls, and restore after calls without stopping
rendering. Backgrounding still pauses gameplay and mutes sound.

On 2026-11-09, investigation on an AYN Odin2 Portal (Android 13) verified the
Windows toolchain, rendered bundled Freedoom, and passed 13 instrumentation tests.
A Xiaomi 25010PN30G (Android 16) confirmed a real phone call returns delayed audio
focus before the native game thread starts, reproducing the cause of issue #13.
These investigation builds preceded the final independent-audio implementation.

The revised build passes 53 JVM tests and all 13 instrumentation tests on the
Xiaomi Android 16 phone. The maintainer confirmed that music in another app and
Freedoom's audio play together. Physical call checks also passed:

- An incoming call muted game output before the activity paused. Returning to
  Doom during the connected call resumed rendering in the same process and
  remained silent, including after regaining window focus.
- Force-stopping and reopening the debug app recovered the interrupted session
  as `PROCESS_LOST`. Play launched a new game during the connected call, with
  mute initialized before the single native engine thread started. The pending
  directory was empty and only the new active descriptor remained.
- The caller hung up while Doom stayed foreground. Game sound returned without
  a restart or freeze, and the game process remained the same.

Native OpenAL loopback tests render actual samples to memory and verify initial
mute, both music/SFX source gains, overlapping mute reasons, mute during deferred
updates, context restoration/reinitialization, and 100 concurrent teardown
cycles. Removing the app mute or deferred-update flush made the corresponding
regression fail. Bootstrap validation, three Python tests, strict OpenSpec
validation, and Android lint also passed. Lint retains 19 existing warnings
about dependency versions and platform compatibility; no errors remain.

JVM regressions cover muted foreground gameplay during calls, background mute,
session orphan recovery, result preservation, launch handoff, and late native
shutdown. Call ending while the game stays backgrounded is covered by the state
tests, not a separate physical call check. The API 26-30 polling fallback has not
been physically tested. Record only device model and Android version; keep
device identifiers and call details out of committed evidence.
