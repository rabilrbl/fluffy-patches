---
name: morphe-patching
description: Create, edit, and debug Morphe patches for Android apps. Use when writing new patches, fixing broken patches, or understanding the Morphe patcher API (bytecodePatch, resourcePatch, compatibleWith, classDefBy, smali injection).
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: morphe-patches
---

## What I do

- Create Morphe bytecode and resource patches following project conventions
- Inject smali instructions to modify app behavior
- Handle compatibility constants, package naming, and patch structure
- Debug common patch failures (class not found, method resolution, smali errors)

## When to use me

Use this skill when:
- Adding a new patch for an app
- Fixing a patch that crashes or doesn't apply
- Understanding how to target specific methods/classes in an APK
- Converting smali patches between formats

## Morphe Patch Structure

All patches live under `patches/src/main/kotlin/app/template/patches/<appname>/<category>/`.

### Bytecode Patch Pattern

```kotlin
package app.template.patches.<app>.<category>

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_<APP>

@Suppress("unused")
val patchName = bytecodePatch(
    name = "Human readable name",
    description = "Detailed description of what this patch does.",
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

### Resource Patch Pattern

```kotlin
package app.template.patches.<app>.<category>

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_<APP>

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

## Key Rules

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Patch files | PascalCase + `Patch` suffix | `RemoveRootDetectionPatch.kt` |
| Patch vals | camelCase + `Patch` suffix | `removeRootDetectionPatch` |
| Compatibility constants | SCREAMING_SNAKE_CASE | `COMPATIBILITY_JIOTV_MOBILE` |
| Multi-patch files | camelCase + `Patches` suffix | `miscPatches` |
| Packages | `app.template.patches.<app>.<category>` | `app.template.patches.jiotv.root` |

### Required Annotations

- All patch vals MUST have `@Suppress("unused")` — they are loaded via reflection
- Always use `classDefBy("Lfully/qualified/ClassName;")` with Dalvik descriptor format
- Method names use Java-style: `methodName` (not smali style)

### Smali Injection Patterns

#### Return false (bypass check)
```kotlin
.addInstructions(
    0,
    """
        const/4 v0, 0x0
        return v0
    """,
)
```

#### Return true
```kotlin
.addInstructions(
    0,
    """
        const/4 v0, 0x1
        return v0
    """,
)
```

#### Return null
```kotlin
.addInstructions(
    0,
    """
        const/4 v0, 0x0
        return-object v0
    """,
)
```

#### Return empty string
```kotlin
.addInstructions(
    0,
    """
        const-string v0, ""
        return-object v0
    """,
)
```

#### Return empty list
```kotlin
.addInstructions(
    0,
    """
        new-instance v0, Ljava/util/ArrayList;
        invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
        return-object v0
    """,
)
```

### Error Handling

- Use `runCatching { ... }.getOrNull()?.let { ... }` for optional class lookups
- `.first { }` is acceptable for required methods (throws if not found)
- Use `.filter { }.forEach { }` for patching multiple methods matching a pattern

### Code Style

- 4 space indentation
- Blank line between `compatibleWith()` and `execute {}`
- Blank line between separate `classDefBy` blocks
- Trailing commas on multi-line calls
- Follow IntelliJ IDEA style (enforced via ktlint)

## Adding a New App

1. Add `Compatibility` constant in `patches/src/main/kotlin/app/template/patches/shared/Constants.kt`
2. Create `patches/src/main/kotlin/app/template/patches/<appname>/<category>/` directory
3. Create `docs/<appname>/` directory and document initial APK analysis
4. Write patch files with `compatibleWith(NEW_COMPATIBILITY_CONSTANT)`
5. Run `./gradlew :patches:generatePatchesList` to regenerate metadata

## Build Commands

```bash
# Build the patch package
ANDROID_HOME="$HOME/Android/Sdk" GITHUB_ACTOR="$(gh api user --jq '.login')" GITHUB_TOKEN="$(gh auth token)" ./gradlew :patches:buildAndroid

# Generate patches metadata
./gradlew :patches:generatePatchesList

# Clean build artifacts
./gradlew :patches:clean
```

## Common Pitfalls

1. **Class not found**: Verify the class exists in the target APK version. Use JADX to decompile and confirm.
2. **Method not found**: Check method signature — overloads require matching by parameters, not just name.
3. **Smali syntax errors**: Dalvik instructions must be valid. Common mistakes: wrong register count, missing `;` in type descriptors.
4. **Patch not loading**: Ensure `@Suppress("unused")` is present and the val is at file level (not inside a class).
5. **Wrong compatibility**: Each patch must specify the correct `COMPATIBILITY_*` constant matching the target app version.

## Debugging Tips

- Use `adb logcat` to see patcher runtime errors
- Check the APK with JADX CLI to verify class/method existence before writing patches:
  ```bash
  jadx app.apk -d jadx_output --deobf
  find jadx_output/ -name "*.java" | xargs grep -n "ClassName\|methodName"
  ```
- Test patches on a real device/emulator via Morphe Manager
- Document findings in `docs/<appname>/` for future contributors
