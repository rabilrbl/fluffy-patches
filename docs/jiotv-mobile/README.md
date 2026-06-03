# JioTV Mobile Patches Documentation

## Overview

JioTV research here currently spans **two different targets** that need to stay separate:

- **371 split build**, `versionName 7.1.7`, `versionCode 371`, current Uptodown-style split install, uses `licensecheck3`
- **404 merged / antisplit path**, older APKMirror-style merged APK notes and older `licensecheck/*` assumptions

Read [targets.md](targets.md) and [session-2026-04-08-latest-uptodown-xapk.md](sessions/session-2026-04-08-latest-uptodown-xapk.md) first so you do not mix the 371 split baseline with the older 404 merged path.

## Core Documents

| Document | Description |
|----------|-------------|
| [Targets](targets.md) | Clean split between the 371 split baseline and the older 404 merged / antisplit track |
| [Patch Reference](patch-reference.md) | Complete method type reference, class mappings, patch details |
| [Debugging Journey](debugging-journey.md) | Iterative debugging log with 21 iterations of failures and fixes |
| [Next Session Handoff](next-session-handoff.md) | Compact resume note with the active baseline, confirmed blockers, and best next moves |

## Research

| Document | Description |
|----------|-------------|
| [Reverse Engineering](research/reverse-engineering.md) | JADX CLI methodology, smali analysis, deobfuscation caveats |
| [Pairip DRM](research/pairip-drm.md) | Native DRM library analysis, VM execution, bypass strategy |
| [Play Store Redirects](research/play-store-redirects.md) | All Play Store redirect paths and their bypasses |
| [Emulator & Root Detection](research/emulator-root-detection.md) | Detection mechanisms and bypasses |
| [SSL Pinning](research/ssl-pinning.md) | Certificate pinning analysis and bypass |
| [External PairIP Research](research/external-pairip-research.md) | Consolidated findings from external sources |
| [Native Bypass Attempts](research/native-bypass-attempts.md) | All attempts to bypass libpairipcore.so |
| [Frida & unpaircore Attempts](research/frida-unpaircore-attempts.md) | Frida hooking and gamepwnage framework testing |
| [APKM Bundle Analysis](research/apkm-bundle-analysis.md) | APKM bundle structure and split APK findings |
| [Pairip Hash Change Bypass Research](research/pairip-hash-change-bypass-research.md) | Research on bypassing pairip via hash modification |
| [API Verification Results](research/api-verification-results.md) | Curl-verified endpoint status, auth flow, request/response schemas |
| [OpenAPI Spec](research/openapi-spec.yaml) | Complete API specification reverse-engineered from APK smali |

## Sessions

| Document | Description |
|----------|-------------|
| [2026-04-04 Continued](sessions/session-2026-04-04-continued.md) | Firebase manifest removal, ClassReference crash, APK repackaging workflow |
| [2026-04-05](sessions/session-2026-04-05.md) | APKEditor approach, libpairipcorex.so testing, ARM64 device testing |
| [2026-04-08 Uptodown XAPK](sessions/session-2026-04-08-latest-uptodown-xapk.md) | Confirms original 371 split install launches and re-signing alone reintroduces the native crash |
| [2026-04-08 Frida Route](sessions/session-2026-04-08-frida-route.md) | Runtime-hook route, Magisk-rooted attach, and the current anti-Frida findings |
| [2026-04-30 Dump Attempts](sessions/session-2026-04-30-pairip-dump-attempts.md) | Failed runtime dump approaches for libpairipcore.so |
| [2026-04-30 Smali Patching v2](sessions/session-2026-04-30-v2-smali-patching.md) | Early smali patching attempts |
| [2026-05-01 Pairip String Encryption Bypass](sessions/session-2026-05-01-pairip-string-encryption-bypass.md) | Clinit injection strategy |
| [2026-05-01 Clinit v3 Crashlytics](sessions/session-2026-05-01-clinit-v3-crashlytics.md) | v3 script, build fixes, Firebase NPE fixes |
| [2026-05-01b HPROF Heap String Extraction](sessions/session-2026-05-01b-hprof-heap-string-extraction.md) | HPROF parser fix, 1,472 strings extracted |
| [2026-05-09 Direct DEX Patching](sessions/session-2026-05-09-direct-dex-patching.md) | Direct DEX patching approach |
| [2026-05-09 v2 Clinit Caller Patching](sessions/session-2026-05-09-v2-clinit-caller-patching.md) | Clinit caller patching approach |
| [2026-05-09 v3 License + SSL Bypass](sessions/session-2026-05-09-v3-license-ssl-bypass.md) | License check + SSL pinning bypass — app functional but content playback crashes |
| [2026-05-09 v4 Working App](sessions/session-2026-05-09-v4-working-app.md) | Working app: pairip bypass + Firebase fix + OkHttp pkOPEgq init — launches to HomeActivity |
| [2026-05-10 v23 Merged APK](sessions/session-2026-05-10-v23-merged-apk.md) | Merged XAPK build — all split resources included, apktool full rebuild |
| [2026-05-10 v23–v29 Pairip Strings](sessions/session-2026-05-10-v23-v29-merged-pairip-strings.md) | VMRunner bypass, string class clinit patches, Firebase NPE cascade fixes |

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
| Bypass pairip VM | playstore | Bytecode | VMRunner.<clinit> returns void, invoke() returns null, StartupLauncher.launch() returns void |
| Bypass license check | playstore | Bytecode | LicenseClientV3: initializeLicenseCheck, processResponse, showPaywall, showErrorDialog → return-void |
| Remove certificate pinning | sslpinning | Bytecode | APIManager SSL pin bypass + JioPlayer cert pin bypass |
| Force-init pairip strings | misc | Bytecode | pkOPEgq + kEpRxMC `<clinit>` assign empty strings to all fields, JioTVApplication.attachBaseContext force-inits |
| Enable cleartext traffic | misc | Resource | Sets `usesCleartextTraffic=true` and rewrites network security config |
| Remove split metadata | misc | Resource | Removes split APK metadata that prevents install on some devices |

## Current Status

### 371 split baseline, current reality
- **v29 merged XAPK build** — all split resources, pairip VM neutralized, string class clinit patches
- VMRunner.<clinit> returns void, StartupLauncher.launch() returns void
- pkOPEgq + kEpRxMC pairip string classes default all 96 fields to empty strings via `<clinit>`
- FirebaseInitProvider restored with exception handler in onCreate()
- SystemPropsKt.systemProp() null guard for pairip-decrypted property keys
- LicenseClientV3 fully bypassed, SSL pinning bypassed (APIManager + JioPlayer)
- Application class changed to JioTVApplication (bypasses pairip at manifest level)
- **NOT TESTED** — v29 built and installed but emulator killed before verification
- Empty string defaults may cause auth failures if pairip fields hold real API tokens/URLs

### 404 merged / antisplit track
- Historical notes remain useful for class mapping, older smali work, and prior failed approaches
- Do **not** assume those older `licensecheck/*` or VM-disabling edits apply cleanly to the 371 split target

### Practical direction
- v29 built from merged XAPK — all split resources, pairip VM neutralized, string class clinit patches
- Key insight: split APK resources must be merged before patching — base.apk alone is incomplete
- Key insight: apktool full rebuild works when starting from merged APK (complete resources.arsc)
- Key insight: ExceptionInInitializerError is unrecoverable — prevent clinit failures instead of try-catching
- Key insight: empty string defaults for pairip fields prevent NPEs but may not be enough for API auth
- Next: test v29 on device, verify no crashes, test content playback, convert patches to Morphe format

### Known Working Solutions (Require Root)
- **pairipfix** (LSPosed module): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level