# Design

## Context

See proposal.md for issue #13. The maintainer explicitly chose mixing with music and muting game audio during calls while gameplay continues. SDL's native pause stops rendering as well as gameplay, so audio availability must not drive that lifecycle method.

## Goals / Non-Goals

Separate audio output from foreground gameplay, preserve other media playback, and recover abandoned sessions without affecting configs or saves. Do not add telephony permissions, alter system volume, or change native dependencies.

## Decisions

- Remove the exclusive audio-focus request. It interrupts other media and cannot implement the chosen mixing policy. Pausing gameplay or returning a startup error for busy audio were rejected by the maintainer.
- Observe AudioManager.mode with its callback on API 31+, and poll only while resumed on API 26-30. Normal mode allows mixing; ringing/communication modes mute only game output. Re-read mode on every resume and callback, so stale notifications cannot restore sound during a call.
- Use a synchronized native OpenAL output gate shared by music and SFX. Set the call/background mute before starting SDL. Combine it with Woof's window-focus mute; regaining window focus must not override a call. Protect context attachment/detachment against Java-thread callbacks.
- Resume SDL explicitly on foreground resume regardless of call status, after initializing muting. Pause SDL and mute output on backgrounding.
- Claim the session in onCreate before starting the native thread. Recover orphaned pending/active envelopes only when the game process is absent, and guard launch handoff against concurrent recovery. A launch that supersedes queued recovery must first recover old orphans.
- Preserve the first terminal result under a lock. Coordinate native completion and activity destruction in either order so delayed shutdown still terminates the game process. Orphans use INTERRUPTED/PROCESS_LOST; cancellation before startup uses startup failure.

## Risks / Trade-offs

- Some apps or OEMs may report communication mode inconsistently -> validate physical telephony on Android 16 and document tested versions. No call-content or phone-state permission is needed.
- API 26-30 has no mode callback -> a 250 ms foreground-only poll bounds detection latency.
- Native muting and context destruction may race -> serialize registration, muting, and detachment; test actual audio output with OpenAL loopback.
- Process-liveness is a snapshot -> guard same-launcher handoff; launcher-process death while Android still queues an activity remains an OS-boundary limitation.
- Windows CRLF conversion changes checksummed Woof assets -> disable conversion when cloning and verify bootstrap hashes.

## Migration Plan

No data migration. Test a separate debug package on Android 13 and Android 16. Keep the source change local for review; use the existing release workflow when publishing is requested.
