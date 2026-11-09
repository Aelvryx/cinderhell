# Dependency maintenance

`.github/dependabot.yml` groups Gradle and GitHub Actions version updates into
one `foundation` pull request every quarter (January, April, July, and October),
with at most one open routine update PR. Actions have a 14-day cooldown; Gradle
updates use 30 days for major releases, 14 for minor, and 7 for patches.

Security alerts and automated security updates remain enabled independently.
They are not delayed by this quarterly version-update schedule or its cooldowns.
Native submodules remain pinned manually in `third_party/dependencies.lock.toml`:
updating them also requires source, checksum, license, and patch review.

KSP 2.3.13 is excluded by exact version because of its documented
[concurrent-execution regression](https://github.com/google/ksp/issues/3286).
Future KSP releases remain eligible for the normal grouped review.

## One validation pass per batch

1. Review the group against current main. If it is stale, request a single
   `@dependabot recreate` before adding maintainer commits; recreation can replace
   edits. Wait for the refreshed head, then integrate current main.
2. Review action commit pins and Gradle wrapper provenance, update the toolchain
   documentation and changelog, and freeze the versions for this validation pass.
   Do not repeatedly refresh dependency versions while testing.
3. Run the JVM tests, lint, bootstrap/Python checks, all APK variants, physical
   instrumentation, and native audio regression described in `toolchain.md`.
   Review the final diff and require the PR's build check on the exact head.
4. Follow `release.md`: merge the reviewed commit, build a signed draft preview,
   and verify its identity, signer, checksums, and source archive. Upgrade a device
   in place and check existing profiles/saves, Continue, controls, background and
   resume, and clean game exit before publishing that exact artifact.

Dependabot stops automatic rebases of old version PRs after 30 days. Maintainer
commits also prevent automatic rebases, keeping a validated batch stable. A
later scheduled batch is expected; urgent security changes may require a
separate review.

References: GitHub's [multi-ecosystem updates](https://docs.github.com/en/code-security/concepts/supply-chain-security/multi-ecosystem-updates),
[configuration options](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference),
and [PR commands](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-pull-request-comment-commands).
