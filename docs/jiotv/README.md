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
| [Debugging Journey](debugging-journey.md) | Iterative debugging log with failures and fixes |
| [Emulator & Root Detection](emulator-root-detection.md) | Detection mechanisms and bypasses |
| [SSL Pinning](ssl-pinning.md) | Certificate pinning analysis and bypass |
| [External PairIP Research](external-pairip-research.md) | Consolidated findings from external sources (GitHub, forums, blogs) |
| [Native Bypass Attempts](native-bypass-attempts.md) | All attempts to bypass libpairipcore.so native integrity checks |

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
  -p patches/build/libs/patches-1.0.0-dev.1.mpp \
  --force \
  -t /tmp/morphe-tmp \
  -o /tmp/morphe-tmp/output.apk \
  JioTV_v7.1.7\(404\)_antisplit.apk
```

## Patch Summary

| Patch | Category | Type | Description |
|-------|----------|------|-------------|
| Disable pairip license check (manifest) | playstore | Resource | Removes `LicenseContentProvider` from manifest |
| Remove Play Store license check | playstore | Bytecode | Bypasses pairip DRM, server-driven updates, and Google Play Core in-app updates |
| Remove emulator detection | emulator | Bytecode | Bypasses Build.FINGERPRINT/MODEL/BRAND checks, BlueStacks/Fire TV detection |
| Remove root detection | root | Bytecode | Bypasses root/Xposed detection checks |
| Remove certificate pinning | sslpinning | Bytecode | Disables Firebase-controlled SSL pinning and OkHttp CertificatePinner |
| Enable cleartext traffic | misc | Resource | Sets `usesCleartextTraffic=true` and rewrites network security config |

## Current Status

**BLOCKED**: Native VM integrity checks in `libpairipcore.so` prevent any dex-level patching from working. See [Native Bypass Attempts](native-bypass-attempts.md) for details.

### Known Working Solutions (Require Root)
- **pairipfix** (LSPosed module): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level

### Non-Root Approaches (All Failed So Far)
- CRC32 restoration (SafaSafari method) — structural hash verification beyond CRC32
- Stub native library replacement — breaks Firebase initialization
- Resource-only patches — morphe-cli always recompiles dex files
- Apktool rebuild — also recompiles dex files

### Next Approaches to Try
- Manual binary XML modification (no dex recompilation)
- Native library patching (reverse engineer libpairipcore.so)
- Frida runtime hooking to understand VM behavior
