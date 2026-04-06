# JioTV Patches Documentation

## Overview

Patches for **JioTV** (`com.jio.jioplay.tv`) v7.1.7 (404), an anti-split APK.

## Document Index

| Document | Description |
|----------|-------------|
| [Reverse Engineering](reverse-engineering.md) | JADX CLI methodology, smali analysis, deobfuscation caveats |
| [pairip DRM](pairip-drm.md) | Native DRM library analysis, VM execution, bypass strategy |
| [Play Store Redirects](play-store-redirects.md) | All Play Store redirect paths and their bypasses |
| [Patch Reference](patch-reference.md) | Complete method type reference, class mappings, patch details |
| [Debugging Journey](debugging-journey.md) | Iterative debugging log with 21 iterations of failures and fixes |
| [Session 2026-04-04 Continued](session-2026-04-04-continued.md) | Continued session: Firebase manifest removal, ClassReference crash, APK repackaging workflow |
| [Session 2026-04-05](session-2026-04-05.md) | APKEditor approach, libpairipcorex.so testing, ARM64 device testing |
| [Session 2026-04-06 Emulator Debug Pass](session-2026-04-06-emulator-debug-pass.md) | Fresh AVD/tooling validation, emulator crash evidence, current hard blocker |
| [Emulator & Root Detection](emulator-root-detection.md) | Detection mechanisms and bypasses |
| [SSL Pinning](ssl-pinning.md) | Certificate pinning analysis and bypass |
| [External PairIP Research](external-pairip-research.md) | Consolidated findings from external sources |
| [Native Bypass Attempts](native-bypass-attempts.md) | All attempts to bypass libpairipcore.so |
| [Frida & unpaircore Attempts](frida-unpaircore-attempts.md) | Frida hooking and gamepwnage framework testing |
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

**PARTIAL PROGRESS on x86_64 AVD**: Smali patches work but Kotlin metadata initialization blocked. See [session-2026-04-05.md](session-2026-04-05.md) for latest details.

### What Works (x86_64 AVD)
- ✅ APKEditor workflow modifies APK without dex redistribution
- ✅ VMRunner.smali - Removed native library loading
- ✅ VMRunner.executeVM - Stub returning null
- ✅ SignatureCheck.verifyIntegrity - Returns void
- ✅ FirebaseInitProvider.onCreate - Returns false
- ✅ IklIsnnNWteL - 49 strings initialized to ""
- ✅ No native SIGSEGV crash

### What's Blocked
- ❌ `ExceptionInInitializerError` in `BaseActivity.onCreate` - Kotlin metadata not initialized
- The native library patches Kotlin runtime metadata in a way that smali cannot reproduce

### ARM64 Physical Device Status
- Original library aborts with `length_error was thrown in -fno-exceptions mode with message "vector"`
- Integrity check fails - likely SafetyNet/Play Integrity/device model detection

### Known Working Solutions (Require Root)
- **pairipfix** (LSPosed module): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level

### Next Steps
1. Deep reverse-engineering of `libpairipcore.so` to understand Kotlin metadata patching
2. Create mock x86_64 native library using Android NDK
3. Cross-compile ARM64 mock library

### Working APK
`/tmp/jiotv-patched4-aligned-debugSigned.apk` (test on x86_64 AVD)
