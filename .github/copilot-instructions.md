# GitHub Copilot Instructions for fluffy-patches

## Project Overview

**fluffy-patches** is a Morphe Patches repository for Android apps. Patches are compiled into `.mpp` files consumed by Morphe Manager. Each app gets its own subdirectory under `patches/src/main/kotlin/app/template/patches/<appname>/`.

## Build Commands

### Required Environment Variables

| Variable | Purpose |
|----------|---------|
| `ANDROID_HOME` | Path to Android SDK (required for `buildAndroid` task) |
| `GITHUB_ACTOR` | GitHub username for accessing Morphe's private Maven registry |
| `GITHUB_TOKEN` | GitHub personal access token with `read:packages` scope |

### Commands

```bash
# Build the patch package (.mpp file)
./gradlew :patches:buildAndroid

# Generate patches metadata JSON (patches-list.json)
./gradlew :patches:generatePatchesList

# Full build (all modules)
./gradlew build

# Clean build artifacts
./gradlew :patches:clean
```

Build output: `patches/build/libs/patches-{version}.mpp`

**Note:** There are no automated tests. Patches are validated manually via Morphe Manager.

## Testing

- All patch updates must be tested on an APK using the Morphe CLI before committing.
- Use `adb` commands to install and verify the patched APK on a device/emulator when available.
- Any scripts added to `scripts/` must also be tested against a real APK and verified via ADB.

## Code Style

- **Indentation:** 4 spaces
- **Style:** IntelliJ IDEA (enforced via ktlint in `.editorconfig`)
- **Wildcard imports:** Allowed (`ktlint_standard_no-wildcard-imports = disabled`)
- **Trailing commas:** Required on multi-line calls
- **Blank lines:** Between `compatibleWith()` and `execute {}`, and between separate `classDefBy` blocks

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Patch files | PascalCase + `Patch` suffix | `RemoveRootDetectionPatch.kt` |
| Patch vals | camelCase + `Patch` suffix | `removeRootDetectionPatch` |
| Compatibility constants | SCREAMING_SNAKE_CASE | `COMPATIBILITY_JIOTV_MOBILE` |
| Multi-patch files | camelCase + `Patches` suffix | `miscPatches` |
| Packages | `app.template.patches.<app>.<category>` | `app.template.patches.jiotv.root` |

All patch vals must have `@Suppress("unused")` — they are loaded via reflection.

## Patch Patterns

### Bytecode Patch

```kotlin
@Suppress("unused")
val patchName = bytecodePatch(
    name = "Human readable name",
    description = "Detailed description.",
) {
    compatibleWith(COMPATIBILITY_TARGET)

    execute {
        classDefBy("Lcom/package/ClassName;")
            .methods.first { it.name == "methodName" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
    }
}
```

### Resource Patch

```kotlin
@Suppress("unused")
val patchName = resourcePatch(
    name = "Human readable name",
    description = "Detailed description.",
) {
    compatibleWith(COMPATIBILITY_TARGET)

    execute {
        document("AndroidManifest.xml").use { doc ->
            // DOM manipulation
        }
    }
}
```

### Error Handling

- Use `runCatching { ... }.getOrNull()?.let { ... }` for optional class lookups
- `.first { }` is acceptable for required methods (throws if not found)
- Use `.filter { }.forEach { }` for patching multiple methods matching a pattern

## APK Analysis with JADX CLI

Use JADX CLI to decompile and analyze target APKs before writing patches:

```bash
# Full decompilation with deobfuscation
jadx app.apk -d jadx_output --deobf

# Search for a class in decompiled output
find jadx_output/ -name "*.java" | xargs grep -l "ClassName"

# Search for a method call
find jadx_output/ -name "*.java" | xargs grep -n "methodName("

# Search for a string literal
find jadx_output/ -name "*.java" | xargs grep -l "string to find"

# Get AndroidManifest.xml
cat jadx_output/resources/AndroidManifest.xml

# Find all classes in a package
find jadx_output/ -path "*/com/example/*" -name "*.java"
```

Always verify class/method existence in the target APK before writing patches.

## Available Skills

This project includes agent skills loaded on-demand via the `skill` tool. Use these when the task matches their description:

| Skill | Description | When to Use |
|-------|-------------|-------------|
| `morphe-patching` | Create, edit, and debug Morphe patches | Writing new patches, fixing broken patches, smali injection |
| `android-apk-analysis` | Analyze Android APK structure, decompile with JADX | Reverse-engineering APKs, finding patch targets |
| `revanced-to-morphe` | Convert ReVanced patches to Morphe patches | Migrating from ReVanced, adapting patching techniques |
| `openspec-explore` | Think through ideas and clarify requirements | Exploring ideas before or during a change |
| `openspec-propose` | Propose a new change with design, specs, and tasks | Quickly describing what to build |
| `openspec-apply-change` | Implement tasks from an OpenSpec change | Starting or continuing implementation |
| `openspec-archive-change` | Archive a completed change | Finalizing after implementation is done |

To use a skill, invoke it with the `skill` tool. The full instructions will be loaded automatically.

## Documentation

All knowledge, findings, and debugging notes must be documented under `docs/<appname>/` with category-based folders and files. Create new markdown files as you discover detection mechanisms, failed patch attempts, smali patterns, and APK observations.

## Adding a New App

1. Add `Compatibility` constant in `patches/src/main/kotlin/app/template/patches/shared/Constants.kt`
2. Create `patches/src/main/kotlin/app/template/patches/<appname>/<category>/` directory
3. Create `docs/<appname>/` directory and document initial APK analysis
4. Write patch files with `compatibleWith(NEW_COMPATIBILITY_CONSTANT)`
5. Run `./gradlew :patches:generatePatchesList` to regenerate metadata

## Release Process

Releases use semantic-release on push to `main` (stable) or `dev` (pre-release). Use conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`. After release, `main` is auto-backmerged into `dev`.
