# Tasks

## 1. Windows setup

- [x] 1.1 Install Scoop Java 17, Python, Node, Android tools, pinned SDK/NDK/CMake, and OpenSpec; verify versions and bootstrap checksums.
- [x] 1.2 Document the verified Windows commands and LF checkout requirement in docs/toolchain.md.

## 2. Session recovery

- [x] 2.1 Add failing SessionStore regression tests for abandoned pending requests, live-process preservation, malformed descriptors, and exactly-once results; run testDebugUnitTest.
- [x] 2.2 Implement pending recovery and protect launcher handoff; rerun session tests and inspect lifecycle integration.

## 3. Independent game audio

- [x] 3.1 Add JVM tests for music mixing, muted foreground gameplay during calls, call end, and backgrounding; run testDebugUnitTest.
- [x] 3.2 Replace audio-focus ownership with call-mode monitoring and independent lifecycle control; compile and run JVM tests.
- [x] 3.3 Implement and test the native music/SFX output gate including mute-before-start, window overlap, and context reinitialization; build the debug APK.
- [x] 3.4 Record physical call/music validation on Android 16 and lifecycle evidence in docs/lifecycle-gate.md.

## 4. Integration

- [x] 4.1 Run bootstrap validation, Python script tests, JVM tests, OpenSpec strict validation, and Android instrumentation; report any existing platform limitations.
- [x] 4.2 Review the complete diff for session ownership races, native teardown, and preservation of saves; resolve material findings before delivery.
- [x] 4.3 Validate launching during an Android 16 call, receiving a call during gameplay, ending the call, and music mixing; record the result.

## Workflow follow-up

- Review and publish through the existing release workflow after physical call validation.
