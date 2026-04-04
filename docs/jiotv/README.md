# JioTV Patches Documentation

## Overview

Patches for **JioTV** (`com.jio.jioplay.tv`) v7.1.7 (404), an anti-split APK.

## Document Index

| Document | Description |
|----------|-------------|
| [Reverse Engineering](reverse-engineering.md) | JADX methodology, smali analysis, deobfuscation caveats |
| [pairip DRM](pairip-drm.md) | Native DRM library analysis, VM execution, bypass strategy |
| [Play Store Redirects](play-store-redirects.md) | All Play Store redirect paths and their bypasses |
| [Patch Reference](patch-reference.md) | Complete method type reference, class mappings, patch details |
| [Debugging Journey](debugging-journey.md) | Iterative debugging log with 21 iterations of failures and fixes |
| [Session 2026-04-04 Continued](session-2026-04-04-continued.md) | Continued session: Firebase manifest removal, ClassReference crash, APK repackaging workflow |
| [Emulator & Root Detection](emulator-root-detection.md) | Detection mechanisms and bypasses |
| [SSL Pinning](ssl-pinning.md) | Certificate pinning analysis and bypass |
| [External PairIP Research](external-pairip-research.md) | Consolidated findings from external sources |
| [Native Bypass Attempts](native-bypass-attempts.md) | All attempts to bypass libpairipcore.so |
| [APKM Bundle Analysis](apkm-bundle-analysis.md) | APKM bundle structure and split APK findings |

## Quick Start

### Build Patches
```bash
ANDROID_HOME=$HOME/Android/Sdk \
GITHUB_ACTOR=<user> \
GITHUB_TOKEN=<token> \
./gradlew :patches:buildAndroid
```

### Test with morphe-cli
```bash
java -jar morphe-cli-1.6.3-all.jar patch \
  -p patches/build/libs/patches-1.0.0-dev.8.mpp \
  --force \
  -t /tmp/morphe-tmp \
  -o /tmp/morphe-tmp/output.apk \
  JioTV_v7.1.7\(404\)_antisplit.apk
```

## Patch Summary

| Patch | Category | Type | Description |
|-------|----------|------|-------------|
| Disable pairip license check (manifest) | playstore | Resource | Removes `LicenseContentProvider` from manifest |
| Remove Play Store license check | playstore | Bytecode | Bypasses pairip DRM, license checking, and Play Core updates |
| Remove emulator detection | emulator | Bytecode | Bypasses Build.FINGERPRINT/MODEL/BRAND checks, BlueStacks/Fire TV detection |
| Remove root detection | root | Bytecode | Bypasses root/Xposed detection checks |
| Remove certificate pinning | sslpinning | Bytecode | Disables Firebase-controlled SSL pinning and OkHttp CertificatePinner |
| Enable cleartext traffic | misc | Resource | Sets `usesCleartextTraffic=true` and rewrites network security config |
| Disable FirebaseInitProvider | misc | Resource | Removes FirebaseInitProvider from AndroidManifest.xml to prevent crash |

## Current Status

**BLOCKED**: Morphe's dex redistribution corrupts Kotlin class references, causing `ClassReference.<clinit>` NPE crashes. See [Session 2026-04-04 Continued](session-2026-04-04-continued.md) for details.

### What Works
- Stub native library (`libpairipcore.so`, 6472 bytes ARM64 ELF) eliminates native VM crash
- FirebaseInitProvider can be removed from AndroidManifest.xml via resource patch
- All morphe patches compile and apply successfully
- APK can be properly aligned, signed, and installed

### What Doesn't Work
- Morphe's dex redistribution corrupts Kotlin `ClassReference.<clinit>` (null class name NPE)
- Bytecode patches on Firebase classes are unreliable due to dex redistribution across 12+ dex files
- Any approach involving morphe recompiling dex files breaks the APK

### Known Working Solutions (Require Root)
- **pairipfix** (LSPosed module): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level

### Non-Root Approaches (All Failed So Far)
- CRC32 restoration — structural hash verification beyond CRC32
- Stub native library — breaks Firebase initialization (when used alone)
- Resource-only patches — morphe-cli always recompiles dex files
- Apktool rebuild — also recompiles dex files
- Manual binary XML — corrupts manifest structure
- Morphe + stub — Firebase crashes despite bytecode patch
- APKM base.apk + stub split — base.apk can't install standalone
- Morphe with Firebase manifest removal — ClassReference.<clinit> NPE from dex redistribution

### Recommended Next Approaches
- **APKEditor + libpairipcorex.so**: Direct APK modification without dex redistribution (Snailsoft approach)
- **Frida/LSPosed**: Runtime hooking (requires root)
- **baksmali/smali direct editing**: Modify dex files without redistribution
