# Proposal

## Why

Issue #13 reports a black screen during phone calls and stranded pending sessions blocking retries. Audio availability must not stop foreground gameplay, and abandoned launches must recover promptly.

## What Changes

- Keep foreground gameplay running during calls, muting only Cinderhell's sound effects and music.
- Mix game audio with other media without requesting exclusive audio focus or pausing the other player.
- Restore game audio after a call; pause gameplay and mute sound when backgrounded.
- Recover orphaned launch requests promptly and protect launches being handed off to the game process.
- Make session completion single-shot and terminate the private process even after delayed native shutdown.
- Document and verify Windows development with Scoop, the pinned Android toolchain, and OpenSpec.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `native-game-runtime`: Independent audio muting, coexistence with music, lifecycle and abandoned-launch recovery.

## Impact

Android audio/lifecycle handling, a native OpenAL mute bridge, the Woof Android patch, session storage/coordinator, regression tests, and development documentation. No dependency upgrades, telephony permissions, save migration, application ID change, or release publication.
