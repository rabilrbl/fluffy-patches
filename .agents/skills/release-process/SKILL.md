---
name: release-process
description: Commit-message conventions and release rules for this repo — semantic-release on main/dev, the bump type for app version support, and the generated files that must never be hand-edited. Use when writing commits or when about to edit patches-list.json, patches-bundle.json, CHANGELOG.md, gradle.properties, or the README patch tables.
license: MIT
metadata:
  audience: developers
  workflow: release
---

Releases are fully automated via semantic-release (config in `.releaserc`, workflow in
`.github/workflows/release.yml`). Your job is correct commits and clean PRs — never
manual release chores.

## Commit conventions

Conventional commits are enforced by the release tooling — the type determines the release:

| Type | Release | Notes |
|------|---------|-------|
| `feat` | minor | New patches: `feat(blockerx): add ...` |
| `fix` | patch | Patch corrections: `fix(alarmy): ...` |
| `bump` | patch | New target app versions: `bump(alarmy): support v26.33.0` — appears under "🚀 Updated App Support" in release notes |
| `perf` | patch | |
| `chore`, `refactor`, `docs`, `test` | none | No release triggered |

Scope with the app name when the change is app-specific. See the patch-version-bump skill
for version-support commits.

## Release flow

- Push to `main` → stable release `vX.Y.Z`.
- Push to `dev` → pre-release `vX.Y.Z-dev.N`.
- Other branches/chore-only pushes: the workflow just verifies compilation.
- After a stable release, `main` is automatically back-merged into `dev`.
- PRs target `dev` (see CONTRIBUTING.md).

The release workflow: runs the Gradle build, regenerates `patches-list.json`, stamps the
release version into it, regenerates the README patches section, commits everything as
`chore: Release vX.Y.Z [skip ci]`, and attaches `patches-<version>.mpp` to the GitHub
release with build-provenance attestation.

## Generated files — never hand-edit

These are owned by the release tooling (semantic-release plugins and
`.github/scripts/generate_patches_readme.py`):

- `patches-list.json` — regenerated + version-stamped during release
- `patches-bundle.json` — release/changelog metadata for Morphe Manager
- `CHANGELOG.md`
- `gradle.properties` — the version field is managed by the Gradle semantic-release plugin
- `README.md` between the `<!-- PATCHES_START -->` / `<!-- PATCHES_END -->` markers

Running `./gradlew :patches:generatePatchesList` locally for testing is fine — just don't
commit hand-tweaked versions of these files; the release regenerates them.
