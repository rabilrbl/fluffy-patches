---
name: patch-version-bump
description: Update existing patches for a new release of a target app (Alarmy, BlockerX, ...) — re-verify obfuscated class/method targets against the new APK, update the Compatibility constants and app docs, then build and test. Use when a supported app ships a new version or patches stop matching.
license: MIT
metadata:
  audience: developers
  workflow: maintenance
---

Supporting a new app version is the most common recurring task in this repo. Obfuscated
targets move between versions, so every bump is a mini re-analysis, not a version-string
edit. Skills chain here: android-apk-analysis for re-locating targets, morphe-patching
for editing patches, morphe-testing for verification.

## Checklist

Work through in order; don't skip the re-verification steps:

- [ ] Obtain the exact APK version to support (XAPK/APKM → merge with antisplit first,
      patch and test the merged APK)
- [ ] Decompile with JADX (`android-apk-analysis` skill)
- [ ] Read the previous analysis in `docs/<appname>/README.md` — patch targets, methods,
      and how gates interact
- [ ] Re-locate every patch target in the new APK. Obfuscated classes rename between
      versions; find them by string literals and usage patterns, then confirm the runtime
      descriptor in smali
- [ ] Update patch files using the full-body-replacement idiom (`morphe-patching` skill)
- [ ] Update `patches/src/main/kotlin/app/fluffy/patches/shared/Constants.kt` — set
      `AppTarget(version = "X.Y.Z")` on the app's `Compatibility` (targets is a list; keep
      only versions you actually support)
- [ ] Update `docs/<appname>/README.md`: the `**Target:**` version line plus any renamed
      classes, moved gates, or new findings
- [ ] Build and test against the new APK (`morphe-testing` skill) — every patch, not just
      the ones you edited
- [ ] Commit as `bump(<app>): support vX.Y.Z`

## Why targets break between versions

- Obfuscated short names (`bi.c`) are reassigned as code shifts — expect renames on every
  release of heavily obfuscated apps.
- R8 renames lambda classes/fields to stable-per-build identifiers (`r8lambda...`) that
  change per version — copy them fresh from the new APK's smali.
- Gate logic can move to a different class entirely (e.g. premium state moving from a
  prefs delegate to a state class). Trace from the UI/enforcement end back to the source.
- What has stayed stable so far: well-named preference getters
  (`getSUB_STATUS()Z` in BlockerX) survive version bumps better than obfuscated names —
  but still verify.
- When a target can't be re-located, check the reference Morphe patch repos
  (see "Reference Patch Repositories" in AGENTS.md) — another repo may already support
  the new version or have traced the renamed class.

## Commit conventions

- `bump(<app>): support vX.Y.Z` — the intended type for version-support updates; it lands
  in the "🚀 Updated App Support" section of release notes and releases as a patch.
- `fix(<app>): <what broke>` — for patch corrections at the same target version; also
  releases as a patch. History used this for version bumps before the `bump` type was
  configured; prefer `bump:` for pure version support.

## Gotchas

- **Update the docs `**Target:**` line** — it's the first thing the next version bump reads.
- **Interleaved gates get patched together.** If one behavior is gated on multiple checks
  (Alarmy ad init runs if either premium or remove-ad check passes), force all of them in
  one patch; overlapping patch definitions on the same methods conflict.
- **A new version is a new compatibility surface.** Patches are only verified against the
  versions listed in `AppTarget` — don't claim support for versions you didn't test.
- If the new version adds new protections (e.g. pairip integrity checks), document them in
  `docs/<appname>/` even when out of scope — history shows bypass patches get removed when
  they stop working; leave the reasoning behind.
