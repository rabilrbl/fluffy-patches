---
name: morphe-patching
description: Write and debug Morphe patches for this repo — bytecodePatch/resourcePatch structure, the forced-return and full-body-replacement idioms, smali instruction snippets, and common pitfalls. Use when adding a patch or fixing one that doesn't apply or doesn't take effect.
license: MIT
metadata:
  audience: developers
  workflow: morphe-patches
---

Patches live in `patches/src/main/kotlin/app/fluffy/patches/<appname>/<category>/` as
top-level `@Suppress("unused")` vals (loaded via reflection) calling
`compatibleWith(COMPATIBILITY_*)` — full skeletons and code style are in the repo
`AGENTS.md`, which always covers them. This skill focuses on the patcher idioms this
repo actually uses. For locating targets, use android-apk-analysis; for building and
device verification, use morphe-testing; for new app versions, use patch-version-bump.

## Gotchas

- **Prepending a return does NOT override the original body.** `addInstructions(0, ...)`
  inserts *before* the existing instructions, which still run afterwards and overwrite the
  forced result. To force a result, clear the body first, then insert (see next section).
  This caused a real bug: the patched Alarmy APK still contained the original method bodies.
- **JADX display names are not runtime descriptors.** Use `Lbi/c;`, never `bi.PremiumState`.
  Verify in smali (see android-apk-analysis gotchas).
- **Match methods on `returnType`**, not just name — overloads are common
  (`it.name == "r" && it.returnType == "Z"`).
- **Don't overlap patch definitions.** Two patches touching the same method conflict.
  When gates are interleaved (Alarmy gates ad init on either premium check), patch them
  together in one patch instead of defining overlapping patches.
- **Compatibility constants pin exact versions.** A patch only applies to APK versions
  listed in its `AppTarget` — verify you're testing the right APK version.

## The repo idiom: full body replacement

`UnlockPremiumPatch.kt` (Alarmy) is the canonical pattern — clear the body, then inject:

```kotlin
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium features by forcing both premium gates true.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        val premiumStateClass = mutableClassDefBy("Lbi/c;")

        premiumStateClass.methods.first { it.name == "r" && it.returnType == "Z" }
            .apply {
                removeInstructions(0, instructions.count())
                addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
    }
}
```

`mutableClassDefBy("Lpkg/Class;")` returns the class with mutable methods — mutate them
directly, no `.toMutable()` needed. Use `classDefBy(...)` only for read-only inspection.

## Smali snippet library

Injected instruction strings must be valid Dalvik. `p0` is `this` on instance methods;
type descriptors end in `;` (except primitives); register count must match usage.

```kotlin
// Return true / false
addInstructions(0, "const/4 v0, 0x1\nreturn v0")   // or 0x0

// Return null
addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")

// Return empty string
addInstructions(0, "const-string v0, \"\"\nreturn-object v0")

// Return empty list
addInstructions(
    0,
    """
        new-instance v0, Ljava/util/ArrayList;
        invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
        return-object v0
    """,
)

// Call an existing method, then dismiss/return (real logic, from BypassInstantApprovalPatch)
addInstructions(
    0,
    """
        sget-object v0, Lo/removeOnItemTouchListener;->r8lambda4IRRzyoWeWaykEOcgWGjbNoGAkw:Lo/removeOnItemTouchListener;
        iget-object v1, p0, Lcom/example/Dialog;->onClick:Lkotlin/jvm/functions/Function1;
        if-eqz v1, :cond_0
        invoke-interface {v1, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
        :cond_0
        invoke-virtual {p0}, Lcom/example/Dialog;->onDismissClick()V
        return-void
    """,
)
```

Copy identifiers (enum fields, R8-renamed lambdas) exactly from the target APK's smali —
they differ per app and per version.

## Resource patches

XML manipulation uses the DOM API:

```kotlin
execute {
    document("AndroidManifest.xml").use { doc ->
        // DOM manipulation
    }
}
```

## Error handling

- Optional classes (present in some versions only):
  `runCatching { classDefBy("Lmaybe/Missing;") }.getOrNull()?.let { ... }`
- `.first { }` on required methods is fine — failing loudly on a missing target is correct.
- `.filter { }.forEach { }` when patching every method matching a pattern.

## Prior art

Before designing a patch from scratch, check how the reference Morphe patch repos patch
the same app or SDK (see "Reference Patch Repositories" in AGENTS.md: MorpheApp,
Nai64, rushiranpise, hoo-dles, crimera/piko, De-Vanced). They use the same template, so
their patch files show proven target discovery and smali idioms you can adapt. Patches
migrated from ReVanced (piko, De-Vanced) also demonstrate translating fingerprint-based
ReVanced logic into direct targeting.

## Common pitfalls

1. Patch val not top-level or missing `@Suppress("unused")` — patch silently never loads.
2. Wrong class descriptor or method signature — re-verify against the exact APK version.
3. Invalid Dalvik in injected strings — build fails or app crashes; check registers and
   descriptor syntax.
4. Wrong `COMPATIBILITY_*` constant or missing version in `AppTarget` — patch not offered
   for the APK being patched.
5. Prepend-only injection (see gotchas) — patch builds fine but changes nothing at runtime.

Debug runtime failures with `adb logcat` (see morphe-testing for the full loop).
