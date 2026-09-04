---
name: android-apk-analysis
description: Analyze Android APK structure with JADX, locate patch targets (classes, methods, strings), and map security mechanisms like root, emulator, SSL pinning, and integrity checks. Use when reverse-engineering an APK or finding the code to patch before writing a Morphe patch.
license: MIT
metadata:
  audience: developers
  workflow: reverse-engineering
---

Ground every patch in verified decompiled code. This skill covers locating and verifying
targets. For writing the patch, switch to the morphe-patching skill; for build/apply/verify
steps, use morphe-testing.

## Workflow

1. Inspect the APK (package, version, dex layout)
2. Decompile with JADX
3. Search for the behavior to change
4. Verify exact runtime descriptors in smali
5. Record targets in `docs/<appname>/`

## Gotchas

- **JADX display names are not runtime descriptors.** JADX may rename an obfuscated class
  for display (e.g. `bi.PremiumState`) while the runtime descriptor is `Lbi/c;`. Patches
  must use the runtime descriptor. Always confirm the real name in smali (via apktool or
  the JADX "smali" view) before writing `classDefBy("...")`.
- **Split APKs (XAPK/APKM) must be merged first.** `base.apk` alone lacks native libraries
  and configs. Merge with an antisplit tool and patch the merged APK — the same one used
  for device testing.
- **Obfuscated names change between app versions.** `Lbi/c;` in one version may be `Lbi/e;`
  in the next. Locate code by string literals and usage patterns, not just names, and always
  verify against the exact version being supported.
- **R8 renames lambdas and enum fields** to identifiers like
  `r8lambda4IRRzyoWeWaykEOcgWGjbNoGAkw` or unrelated-looking class names
  (`Lo/removeOnItemTouchListener;`). Reference them exactly as they appear in smali.
- **Classes can live in any dex file** (e.g. `classes18.dex`). Grep the entire decompiled
  tree, never a single file.

## Inspect the APK

```bash
# Package name, version name/code, min SDK
aapt dump badging app.apk

# Dex file layout
unzip -l app.apk | grep ".dex"

# Decode resources and smali (for descriptor verification)
apktool d app.apk -o apktool_output
```

## Decompile with JADX

```bash
# Standard decompilation
jadx app.apk -d jadx_output

# With deobfuscation heuristics for obfuscated apps
jadx app.apk -d jadx_output --deobf

# When decompilation fails on some methods, show the raw code anyway
jadx app.apk -d jadx_output --show-bad-code
```

Get `AndroidManifest.xml` from `jadx_output/resources/AndroidManifest.xml` — entry points,
permissions, and app components.

## Search Recipes

```bash
# Find a class by name
find jadx_output/ -name "*.java" | xargs grep -l "ClassName"

# Find callers of a method
find jadx_output/ -name "*.java" | xargs grep -n "methodName("

# Find a string literal (feature labels, gate messages)
find jadx_output/ -name "*.java" | xargs grep -rn "feature locked"

# All classes in a package
find jadx_output/ -path "*/com/example/*" -name "*.java"

# Classes implementing/extending a type
find jadx_output/ -name "*.java" | xargs grep -l "implements SomeInterface"
find jadx_output/ -name "*.java" | xargs grep -l "extends SomeBaseClass"
```

## Where to Look

**Entry points** (from AndroidManifest.xml): main activity, `Application.onCreate`,
services, receivers, content providers.

**Security mechanisms** — search for:

| Mechanism | Indicators |
|-----------|------------|
| SSL pinning | `CertificatePinner`, `TrustManager`, `X509TrustManager`, `network_security_config` |
| Root detection | `su`, `Superuser`, `RootTools`, `Magisk` |
| Emulator detection | `Build.FINGERPRINT`, `Build.MODEL`, `TelephonyManager` |
| Integrity/attestation | `SafetyNet`, `PlayIntegrity`, signature checks |
| Debugger detection | `Debug.isDebuggerConnected` |

**Feature targets**: premium/entitlement gates (SharedPreferences getters, RevenueCat
entitlements), ad initialization, analytics/telemetry, license checks.

## Reading Smali

```smali
# Type descriptors
Z    -> boolean          I    -> int
V    -> void             J    -> long
Ljava/lang/String;       Ljava/util/List;

# Registers
v0, v1, ...   local registers
p0            this (instance methods); p1, p2 ... parameters
```

Prefer JADX's Java view for understanding logic; drop to smali only to verify descriptors,
instruction order, and exact identifiers.

## Documenting

Record findings in `docs/<appname>/README.md` (create per-app docs on first analysis):

- Runtime descriptors (`Lbi/c;`) **and** JADX display names — both, so future readers
  aren't confused by either
- Method signatures and what each gate controls
- Key strings used for detection/gating
- Failed approaches and why they failed
- APK architecture notes (split APKs, dex layout)
