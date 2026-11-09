# Tasks

## 1. Freeze and document the maintenance batch

- [x] 1.1 Audit grouping, schedule, cooldowns, and security update settings; document the verified maintenance policy.
- [x] 1.2 Refresh PR #11 once, integrate current main, and verify the resulting version/action/wrapper diff against upstream metadata.
- [x] 1.3 Record preview 4 changes and the validation profile/save baseline before updating the signed app.

## 2. Validate the integrated code

- [x] 2.1 Run JVM tests, lint, bootstrap/Python checks, all APK variants, and OpenSpec validation; record the results.
- [x] 2.2 Run Android instrumentation and the native OpenAL loopback test on the Odin; require zero failures.
- [x] 2.3 Review the final implementation diff and resolve any blocking findings before the merge gate.

## Workflow follow-up

- Archive the completed implementation change and validate the archive before the final PR check.
- Require successful PR CI on the frozen head, merge that reviewed head, and create the preview 4 tag; require successful main and release workflows.
- Verify draft APK/source checksums, signer, identity, version, and contents; install the exact signed APK in place on the Odin.
- Verify the baseline profile/save survives, game controls/rendering and background/resume work, and repeated sessions return cleanly to the launcher before publishing.
- Record artifact hashes and device results in release notes; publish the prerelease and clean up the merged branch.
