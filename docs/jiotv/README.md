# JioTV Patches Documentation

## Overview

JioTV research here currently spans **two different targets** that need to stay separate:

- **371 split build**, `versionName 7.1.7`, `versionCode 371`, current Uptodown-style split install, uses `licensecheck3`
- **404 merged / antisplit path**, older APKMirror-style merged APK notes and older `licensecheck/*` assumptions

Read `targets.md` and `session-2026-04-08-latest-uptodown-xapk.md` first so you do not mix the 371 split baseline with the older 404 merged path.

## Document Index

| Document | Description |
|----------|-------------|
| [Targets](targets.md) | Clean split between the 371 split baseline and the older 404 merged / antisplit track |
| [Reverse Engineering](reverse-engineering.md) | JADX CLI methodology, smali analysis, deobfuscation caveats |
| [pairip DRM](pairip-drm.md) | Native DRM library analysis, VM execution, bypass strategy |
| [Play Store Redirects](play-store-redirects.md) | All Play Store redirect paths and their bypasses |
| [Patch Reference](patch-reference.md) | Complete method type reference, class mappings, patch details |
| [Debugging Journey](debugging-journey.md) | Iterative debugging log with 21 iterations of failures and fixes |
| [Session 2026-04-04 Continued](session-2026-04-04-continued.md) | Continued session: Firebase manifest removal, ClassReference crash, APK repackaging workflow |
| [Session 2026-04-05](session-2026-04-05.md) | APKEditor approach, libpairipcorex.so testing, ARM64 device testing |
| [Session 2026-04-08 Latest Uptodown XAPK](session-2026-04-08-latest-uptodown-xapk.md) | Confirms original 371 split install launches and re-signing alone reintroduces the native crash |
| [Session 2026-04-08 Frida Route](session-2026-04-08-frida-route.md) | Runtime-hook route, Magisk-rooted attach, and the current anti-Frida findings |
| [Next Session Handoff](next-session-handoff.md) | Compact resume note with the active baseline, confirmed blockers, and best next moves |
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

### 371 split baseline, current reality
- ✅ The untouched original split install launches on the x86_64 AVD
- ✅ The tested package on-device is `versionCode 371`, `versionName 7.1.7`
- ❌ Re-signing the untouched split set is enough to trigger the native `libpairipcore.so` crash
- ❌ That means the current Morphe APK-modification flow is blocked by signature-sensitive native checks before patch logic becomes the main issue

### 404 merged / antisplit track
- Historical notes remain useful for class mapping, older smali work, and prior failed approaches
- Do **not** assume those older `licensecheck/*` or VM-disabling edits apply cleanly to the 371 split target

### Practical direction
- Prefer runtime-hooking / installer-spoofing research for the 371 split build
- Treat old 404 merged patches as a separate research branch, not the active baseline

### Known Working Solutions (Require Root)
- **pairipfix** (LSPosed module): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level
