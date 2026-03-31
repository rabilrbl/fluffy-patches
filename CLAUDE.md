# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**fluffy-patches** is a [Morphe Patches](https://github.com/MorpheApp/morphe-patches) repository containing patches for multiple Android apps. Each app gets its own subdirectory under `patches/`. Patches are compiled into a `.mpp` (Morphe Patch Package) file consumed by Morphe Manager.

## Build Commands

```bash
# Build the patch package (.mpp file)
./gradlew :patches:buildAndroid

# Generate patches metadata JSON files (patches-list.json)
./gradlew :patches:generatePatchesList

# Full build (compiles all modules)
./gradlew build

# Clean build artifacts
./gradlew :patches:clean
```

Build output: `patches/build/libs/patches-{version}.mpp`

## Architecture

### Module Layout

```
patches/src/main/kotlin/app/template/patches/
├── shared/Constants.kt        # Compatibility targets for all supported apps
└── <appname>/                 # One directory per target app
    ├── <category>/            # Logical grouping (e.g. playstore/, root/, misc/)
    │   └── SomePatch.kt
    └── ...
extensions/extension/          # Java extensions for complex runtime logic (template, not actively used)
```

Each app's patches live in their own subdirectory. Add a new `Compatibility` constant in `shared/Constants.kt` when adding support for a new app.

### Patch Types

**Bytecode patches** (`bytecodePatch`) locate classes by smali name (`Lcom/package/ClassName;`), find methods, and inject/replace Dalvik instructions:

```kotlin
val myPatch = bytecodePatch(name = "...", description = "...") {
    compatibleWith(MY_APP_TARGET)
    execute {
        classDefBy("Lcom/package/ClassName;")
            .methods.first { it.name == "methodName" }
            .toMutable()
            .addInstructions(0, "const/4 v0, 0x0\nreturn v0")
    }
}
```

**Resource patches** (`resourcePatch`) modify XML files (AndroidManifest.xml, network config, etc.) via DOM manipulation.

Patches can declare `dependsOn(otherPatch)` to compose behaviors.

### Adding a New App

1. Add a `Compatibility(...)` constant in `shared/Constants.kt` with the app's package name, APK type, and icon color.
2. Create `patches/<appname>/` with patch files grouped by category.
3. Reference the new constant in each patch's `compatibleWith(...)` call.

### Patch Metadata

`PatchListGenerator` (in `patches/src/main/kotlin/app/morphe/util/`) reads compiled patch classes and generates `patches-list.json`. This runs automatically as part of `generatePatchesList`.

## Release Process

Releases are fully automated via semantic-release on push to `main` or `dev` branches:
- `main` → stable release
- `dev` → pre-release
- After release, `main` is auto-backmerged into `dev`

Use conventional commit messages: `feat:`, `fix:`, `chore:`, `refactor:`.

Version is stored in `gradle.properties` and updated automatically by CI.

## Key Files

| File | Purpose |
|------|---------|
| `patches/build.gradle.kts` | Patch module config, metadata (author, license, website) |
| `settings.gradle.kts` | Root project config, Morphe plugin version |
| `gradle/libs.versions.toml` | Dependency versions (morphe-patcher, Gson, smali) |
| `patches-list.json` | Generated patch metadata consumed by Morphe Manager |
| `patches-bundle.json` | Release metadata (version, download URL) for Morphe Manager |
| `.releaserc` | Semantic-release configuration |
