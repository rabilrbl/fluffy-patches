---
name: revanced-to-morphe
description: Convert ReVanced patches to Morphe patches. Use when migrating patches from ReVanced's Kotlin DSL to Morphe's patcher API, or when adapting ReVanced patching techniques for use with Morphe Manager.
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: migration
---

## What I do

- Convert ReVanced patch syntax to Morphe patch syntax
- Translate ReVanced's `replaceMethod` and `injectCalls` to Morphe's smali injection
- Map ReVanced compatibility options to Morphe's `compatibleWith`
- Adapt ReVanced patching patterns for Morphe's architecture

## When to use me

Use this skill when:
- Migrating patches from a ReVanced patches repository
- Understanding how a ReVanced patch would work in Morphe
- Adapting ReVanced root detection bypasses for Morphe
- Converting ReVanced resource patches to Morphe format

## Key Differences

### ReVanced vs Morphe Architecture

| Aspect | ReVanced | Morphe |
|--------|----------|--------|
| Patch format | `.jar` (compiled Kotlin) | `.mpp` (Morphe Patch Package) |
| Patching engine | SMALI patching via dexlib2 | Smali injection via custom patcher |
| Compatibility | Version ranges via `@CompatiblePackage` | Constants via `compatibleWith()` |
| Bytecode patches | `replaceMethod`, `injectCalls` | `classDefBy().methods.first {}.toMutable().addInstructions()` |
| Resource patches | `resourcePatch` with XML DSL | `resourcePatch` with DOM API |

### Syntax Conversion

#### ReVanced Bytecode Patch

```kotlin
// ReVanced style
@Patch(name = "Remove ads", description = "Removes all ads.")
@CompatiblePackage("com.example.app")
class RemoveAdsPatch : BytecodePatch(
    setOf(RemoveAdsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        val result = findClassUsingFingerprint(RemoveAdsFingerprint)
            ?: throw PatchException("Could not find ads class")

        result.classDef.methods.forEach { method ->
            if (method.name == "showAd") {
                method.replaceImplementation {
                    returnVoid()
                }
            }
        }
    }
}
```

#### Morphe Equivalent

```kotlin
// Morphe style
@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes all ads.",
) {
    compatibleWith(COMPATIBILITY_EXAMPLE_APP)

    execute {
        classDefBy("Lcom/example/app/AdsManager;")
            .methods.filter { it.name == "showAd" }
            .forEach { method ->
                method.toMutable().addInstructions(
                    0,
                    """
                        return-void
                    """,
                )
            }
    }
}
```

### ReVanced Fingerprint to Morphe

ReVanced uses fingerprints to find classes/methods:

```kotlin
// ReVanced fingerprint
val removeAdsFingerprint = fingerprint(
    "com.example.app.AdsManager",
    "showAd",
    accessFlags = AccessFlags.PUBLIC,
    returnType = "V",
)
```

In Morphe, you target directly:

```kotlin
// Morphe direct targeting
classDefBy("Lcom/example/app/AdsManager;")
    .methods.first { it.name == "showAd" }
```

If the class name is obfuscated, use search patterns:

```kotlin
// Find by method signature pattern
classDefs
    .filter { cls ->
        cls.methods.any { m ->
            m.name == "showAd" && m.returnType == "V"
        }
    }
    .first()
```

### Common ReVanced Patterns and Morphe Equivalents

#### 1. Return Void (skip method)

**ReVanced:**
```kotlin
method.replaceImplementation {
    returnVoid()
}
```

**Morphe:**
```kotlin
method.toMutable().addInstructions(0, "return-void")
```

#### 2. Return False

**ReVanced:**
```kotlin
method.replaceImplementation {
    const(0)
    returnResult()
}
```

**Morphe:**
```kotlin
method.toMutable().addInstructions(
    0,
    """
        const/4 v0, 0x0
        return v0
    """,
)
```

#### 3. Return True

**ReVanced:**
```kotlin
method.replaceImplementation {
    const(1)
    returnResult()
}
```

**Morphe:**
```kotlin
method.toMutable().addInstructions(
    0,
    """
        const/4 v0, 0x1
        return v0
    """,
)
```

#### 4. Return Null

**ReVanced:**
```kotlin
method.replaceImplementation {
    const(0)
    returnObject()
}
```

**Morphe:**
```kotlin
method.toMutable().addInstructions(
    0,
    """
        const/4 v0, 0x0
        return-object v0
    """,
)
```

#### 5. Inject at Method Start

**ReVanced:**
```kotlin
method.injectCalls(
    Instruction(
        opcode = Opcode.INVOKE_STATIC,
        parameters = listOf("Ljava/lang/String;"),
        method = "Lcom/example/Logger;->log(Ljava/lang/String;)V",
    ),
    index = 0,
)
```

**Morphe:**
```kotlin
method.toMutable().addInstructions(
    0,
    """
        const-string v0, "message"
        invoke-static {v0}, Lcom/example/Logger;->log(Ljava/lang/String;)V
    """,
)
```

#### 6. Resource Patch

**ReVanced:**
```kotlin
resourcePatch {
    xml("res/xml/network_security_config.xml") {
        document.use { doc ->
            // modify XML
        }
    }
}
```

**Morphe:**
```kotlin
execute {
    document("res/xml/network_security_config.xml").use { doc ->
        // modify XML using DOM API
    }
}
```

### ReVanced Annotations to Morphe

| ReVanced | Morphe |
|----------|--------|
| `@Patch(name, description)` | `bytecodePatch(name, description) { }` |
| `@CompatiblePackage("...")` | `compatibleWith(COMPATIBILITY_*)` |
| `@CompatiblePackage(..., versionName = "...")` | Handled via `COMPATIBILITY_*` constants |
| `BytecodePatch(fingerprints)` | Direct `classDefBy()` targeting |
| `ResourcePatch()` | `resourcePatch(name, description) { }` |

### Migration Checklist

1. [ ] Identify the ReVanced patch class and its purpose
2. [ ] Find the target class/method in the APK (use JADX)
3. [ ] Create the Morphe patch file in `patches/src/main/kotlin/app/template/patches/<app>/<category>/`
4. [ ] Add `@Suppress("unused")` annotation
5. [ ] Use `bytecodePatch(name, description)` or `resourcePatch(name, description)`
6. [ ] Add `compatibleWith(COMPATIBILITY_*)` with correct constant
7. [ ] Convert the patching logic to Morphe's smali injection syntax
8. [ ] Test the patch on a real APK using Morphe Manager
9. [ ] Document any differences or special handling in `docs/<appname>/`

### Important Notes

- Morphe does NOT use fingerprints — target classes/methods directly
- Morphe patches are loaded via reflection, so `@Suppress("unused")` is required
- Smali instructions in Morphe are raw strings — ensure valid Dalvik syntax
- Morphe uses DOM API for resource patches, not XML DSL
- Always verify class/method existence in the target APK version before writing patches
