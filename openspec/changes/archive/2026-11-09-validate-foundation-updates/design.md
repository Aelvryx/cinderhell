# Design

## Context

See proposal.md for motivation. PR #11 predates the preview 3 call/audio recovery fix. The Odin has the official preview 3 installed with user data. Release signing is confined to the GitHub preview-release environment.

## Goals / Non-Goals

Validate one fixed dependency batch against current main and preserve the installed preview application's identity and data. This maintenance release does not change gameplay contracts or native dependency pins.

## Decisions

- Retain the existing quarterly multi-ecosystem foundation group, one-open-PR limit, and cooldowns: these already address routine update noise. Keep security updates enabled independently.
- Request one recreation of the stale bot PR, integrate current main, then freeze the dependency versions while testing. Repeated refreshes would invalidate completed checks.
- Test the debug and minified variants, including the physical Odin. Create a separate release-validation profile/save before the signed upgrade, so the preservation check does not overwrite an existing save.
- Merge only the reviewed head with passing required CI. Build preview 4 through the existing signing workflow, verify checksums and signer, install the exact draft artifact in place, then publish it after device checks.

## Risks / Trade-offs

- Compiler/Compose/Room incompatibility -> JVM tests, lint, all APK variants, device instrumentation, and a release upgrade check.
- Native lifecycle regression -> OpenAL loopback test and repeated real game sessions, background/resume, and clean return to the launcher.
- User data loss -> retain application ID and signing certificate; use install -r without uninstalling or clearing data.
- Bot changes during validation -> record the reviewed commit and use an expected-head merge guard.

## Migration Plan

Upgrade preview 3 in place to preview 4. Preserve profiles, library, configuration, and save files. If a release gate fails, keep the release as a draft and fix forward before publishing; never reuse a published version tag or erase user data to make an upgrade pass.
