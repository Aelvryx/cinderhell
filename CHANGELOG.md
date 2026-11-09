# Changelog

All notable Cinderhell changes are recorded here. The project uses
[Semantic Versioning](https://semver.org/) with numbered preview builds.

## [Unreleased]

### Planned

- Bluetooth gamepad compatibility, rumble, and reconnect validation.
- Explicit user-controlled backup/export and restore.

## [0.1.0-preview.4] - 2026-11-09

### Changed

- Updated the grouped Android build dependencies and pinned GitHub Actions.
- Documented the quarterly dependency maintenance and signed-upgrade checks.

### Fixed

- Made the generated compatibility-corpus checksum manifest identical on
  Windows and Linux so Git Bash can verify it on Windows.

## [0.1.0-preview.3] - 2026-11-09

### Fixed

- Fixed a black screen when launching Doom or returning to it during a phone
  call. Gameplay now continues while the game's own audio is muted, and sound
  returns after the call ends.
- Recovered abandoned launch requests immediately so an interrupted session
  cannot leave Play blocked until its request expires.
- Preserved the first session result and terminated the game process after
  delayed native shutdown, allowing the next game to launch safely.

### Changed

- Game audio now mixes with music playing in other apps.
- Documented Windows/Scoop setup and refreshed the OpenSpec development workflow.

### Validation

- Verified music mixing, incoming calls, launching during a connected call,
  and sound restoration on a physical Android 16 phone.
- Passed 53 JVM tests, 13 device tests, and native audio/teardown regressions.

## [0.1.0-preview.2] - 2026-07-26

### Changed

- Reworked the launcher around a selected-profile Play hero, responsive
  game/profile cards, explicit controller states, and consistent route and
  operational feedback.
- Added a complete no-device verification gate for periods when the target
  handheld is unavailable.
- Aligned Compose compile and runtime libraries and hardened physical
  instrumentation against a sleeping target device.

## [0.1.0-preview.1] - Unpublished

### Added

- Controller-first Kotlin/Compose launcher with Play and Continue.
- Isolated SDL3/Woof game sessions for `arm64-v8a`.
- Bundled Freedoom and Android system-document content imports.
- Ordered WAD/PK3/ZIP/DEH/BEX play profiles and curated presets.
- AYN Thor controller and Android lifecycle validation.
- Reproducible, signed GitHub draft-prerelease pipeline with corresponding
  source and checksums.

[Unreleased]: https://github.com/Aelvryx/cinderhell/compare/v0.1.0-preview.4...HEAD
[0.1.0-preview.4]: https://github.com/Aelvryx/cinderhell/compare/v0.1.0-preview.3...v0.1.0-preview.4
[0.1.0-preview.3]: https://github.com/Aelvryx/cinderhell/compare/v0.1.0-preview.2...v0.1.0-preview.3
[0.1.0-preview.2]: https://github.com/Aelvryx/cinderhell/compare/v0.1.0-preview.1...v0.1.0-preview.2
[0.1.0-preview.1]: https://github.com/Aelvryx/cinderhell/tree/v0.1.0-preview.1
