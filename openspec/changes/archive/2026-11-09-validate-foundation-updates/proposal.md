# Proposal

## Why

The maintainer wants the outstanding foundation dependency update reviewed,
tested, merged, and released in one maintenance pass. PR #11 predates preview 3
and must be validated together with its audio/session fixes and existing data.

## What Changes

- Audit Dependabot grouping, schedules, cooldowns, and security-update settings.
- Retain the existing one-PR quarterly foundation policy where it is correct;
  document the maintenance process and refresh the stale batch once.
- Integrate the refreshed update with current main and freeze that commit for
  verification. Correct compatibility issues only where testing finds them.
- Validate JVM, lint, native/minified builds, Odin instrumentation, save/profile
  preservation, lifecycle, and the exact signed preview 4 release APK.
- Update toolchain/changelog documentation, merge PR #11, publish preview 4,
  and leave the released build installed on the Odin.

## Capabilities

### New Capabilities

None. This is dependency maintenance and validation.

### Modified Capabilities

None. Existing gameplay, data-preservation, and release contracts still apply.

## Impact

Gradle wrapper and version catalog, pinned CI actions, maintenance/release
documentation, OpenSpec records, and developer-local test artifacts. Native
dependency revisions, application identity, and the preview signing key remain
unchanged.
