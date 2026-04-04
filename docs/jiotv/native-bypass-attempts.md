# Native Library Bypass Attempts

## Overview

This document tracks all attempts to bypass `libpairipcore.so` native integrity checks.

## The Core Problem

```
Any dex modification → morphe-cli recompiles dex → native VM detects changed hashes → SIGABRT + Play Store paywall
```

## Attempt 1: CRC32 Restoration (SafaSafari Method)

**Result**: FAILED — Native VM does structural hash verification beyond ZIP CRC32.

## Attempt 2: Stub Native Library Replacement

**Result**: PARTIAL SUCCESS — Native VM crash eliminated, but Firebase crashes because VM bytecode normally initializes Firebase config data.

## Attempt 3: Resource-Only Patches

**Result**: FAILED — Morphe-cli always recompiles dex files.

## Attempt 4: Apktool Build

**Result**: FAILED — Apktool also recompiles dex files.

## Attempt 5: Manual APK Reconstruction

**Result**: FAILED — Even with 100% identical dex files, native VM crashes (detects signature change or other modifications).

## Attempt 6: Re-signed Original APK (Zero Content Changes)

**Result**: FAILED — Native VM checks APK signature. Any re-signing triggers crash.

## Attempt 7: APKM Bundle Analysis

**Discovery**: The APKM bundle from APKMirror splits pairip:
- `base.apk` (73MB) — main APK, NO `libpairipcore.so`, valid Play Store signature
- `split_config.arm64_v8a.apk` — contains `lib/arm64-v8a/libpairipcore.so`
- Other splits for different architectures/DPI

**Implication**: The original Play Store APK has a valid signature that passes native VM checks. Pairip is in the architecture splits, not the base.

## Attempt 8: Morphe Patches + Stub Native Library

**Approach**: Combine all morphe patches (emulator, root, sslpinning, license check, cleartext traffic, FirebaseInitProvider no-op) with stub `libpairipcore.so`.

**Result**: Native VM crash eliminated. FirebaseInitProvider still crashes despite bytecode patch — likely because morphe redistributes Firebase classes across multiple dex files and the patch only hits one copy.

**FirebaseInitProvider crash**:
```
Unable to get provider com.google.firebase.provider.FirebaseInitProvider:
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()'
    at com.google.firebase.internal.DataCollectionConfigStorage.<init>
```

**Root cause**: The VM bytecode (normally executed by `executeVM`) initializes Firebase config data. Without it, `DataCollectionConfigStorage` reads null and crashes. The `FirebaseInitProvider.onCreate()` patch returns `false` but `attachInfo()` still calls into the crashing code path.

## Current Status

**BLOCKED** on Firebase initialization. The app's Java code is fully functional (not encrypted), but Firebase requires config data that the VM bytecode normally provides.

### What Works
- Stub native library eliminates native VM crash
- Morphe patches correctly modify dex files
- All Java-level bypasses are verified in binary

### What Doesn't Work
- Firebase crashes without VM config data
- Can't reliably patch FirebaseInitProvider (exists in multiple dex files after morphe redistribution)
- Can't remove FirebaseInitProvider from manifest (morphe recompiles manifest, merging in Firebase providers)

### Remaining Approaches
1. **Patch FirebaseInitProvider.attachInfo()** — intercept before it calls onCreate
2. **Remove FirebaseInitProvider from manifest** — via morphe resource patch (may not work due to manifest merging)
3. **Use APKEditor + libpairipcorex.so** — Snailsoft's approach
4. **Runtime hooking** — Frida/LSPosed
5. **Patch all Firebase registrar classes** — No-op CrashlyticsRegistrar, etc.

## Device Information

- **Device**: CPH2447 (OnePlus/OPPO)
- **Android**: 11.H.11_3110_202601281934
- **Root**: No
- **APK**: JioTV v7.1.7 (404), anti-split single APK
- **morphe-cli**: v1.6.3
