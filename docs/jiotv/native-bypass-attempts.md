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

## Attempt 9: APKEditor + libpairipcorex.so (Snailsoft Approach)

### Setup

Used APKEditor to merge APKM splits and decompile to smali:
```
APKM bundle splits → APKEditor merge → APKEditor decompile (to smali) → 
Manual smali patches → APKEditor rebuild → uber-apk-signer → install
```

### Patches Applied

1. **VMRunner.clinit** — load `libpairipcorex.so` first, then `libpairipcore.so`
2. **SignatureCheck.verifyIntegrity** → return-void
3. **LicenseClient.initializeLicenseCheck** → return-void
4. **LicenseClient.connectToLicensingService** → return-void

### Test on x86_64 AVD

Original APK on x86_64 AVD (no libs):
- AppLogo displayed ✅
- No native VM crash ✅
- LicenseActivity paywall → System.exit(0) ❌

This confirms x86_64 has no native VM, but Java license check still blocks.

### Test on ARM64 Physical Device

With patched APK (both libraries + smali patches):
```
nativeloader: Load libpairipcorex.so ... ok
nativeloader: Load libpairipcore.so ... ok
libc++: length_error was thrown in -fno-exceptions mode with message "vector"
Process ... exited due to signal 31
```

**Result**: FAILED — libpairipcorex.so doesn't intercept integrity checks for pairip version 404.

### Root Cause

The Snailsoft bypass library was built for an older pairip version. The v404 pairip has different integrity check implementations that the bypass doesn't hook.

## Attempt 10: Frida Hooking

### Setup
- frida-server 17.9.1 on AVD (x86_64)
- Port forwarded via ADB

### Results
- **Spawn**: SIGSEGV in libpairipcore.so - detects Frida
- **Attach to running**: App dies too fast
- **Await spawn**: Process never appears

**Result**: FAILED — Native anti-instrumentation detects Frida

## Attempt 11: unpaircore Framework

### Repository
https://github.com/Kitsuri-Studios/unpaircore

### Analysis
- Game hacking framework (Minecraft telemetry example)
- No pairip-specific logic
- Not useful for JioTV bypass

**Result**: FAILED — Wrong tool for this task

## Current Status

### Setup

Used APKEditor to merge APKM splits and decompile to smali:
```
APKM bundle splits → APKEditor merge → APKEditor decompile (to smali) → 
Manual smali patches → APKEditor rebuild → uber-apk-signer → install
```

### Patches Applied

1. **VMRunner.clinit** — load `libpairipcorex.so` first, then `libpairipcore.so`
2. **SignatureCheck.verifyIntegrity** → return-void
3. **LicenseClient.initializeLicenseCheck** → return-void
4. **LicenseClient.connectToLicensingService** → return-void

### Test on x86_64 AVD

Original APK on x86_64 AVD (no libs):
- AppLogo displayed ✅
- No native VM crash ✅
- LicenseActivity paywall → System.exit(0) ❌

This confirms x86_64 has no native VM, but Java license check still blocks.

### Test on ARM64 Physical Device

With patched APK (both libraries + smali patches):
```
nativeloader: Load libpairipcorex.so ... ok
nativeloader: Load libpairipcore.so ... ok
libc++: length_error was thrown in -fno-exceptions mode with message "vector"
Process ... exited due to signal 31
```

**Result**: FAILED — libpairipcorex.so doesn't intercept integrity checks for pairip version 404.

### Root Cause

The Snailsoft bypass library was built for an older pairip version. The v404 pairip has different integrity check implementations that the bypass doesn't hook.

## Current Status

**BLOCKED** on native library bypass. All standard approaches have failed:

1. **libpairipcorex.so** — Snailsoft library doesn't work for v404
2. **Frida** — Native library detects instrumentation and crashes
3. **unpaircore** — Wrong tool, not pairip-specific

### What Works
- APKEditor workflow successfully modifies APK without dex redistribution
- Smali patches apply correctly
- Frida hooks can be established (but app crashes before they take effect)

### What Doesn't Work
- libpairipcorex.so (Snailsoft) doesn't bypass pairip v404
- Frida-based hooking blocked by native anti-instrumentation
- unpaircore framework has no pairip logic

### Remaining Approaches
1. **Custom mock library** — Create our own libpairipcore.so that provides fake verified bytecode
2. **Reverse engineer** — Deep analysis of libpairipcore.so to understand integrity checks
3. **Find v404-specific bypass** — Look for updated Snailsoft or other source

### Attempt 12: Manual Smali Patches + String Initialization

**Approach**: Modified IklIsnnNWteL class to initialize all static strings to "placeholder" via <clinit>, removed native library loading from VMRunner, patched SignatureCheck.verifyIntegrity and StartupLauncher.launch.

**Result**: FAILED — App crashes with `ExceptionInInitializerError` in `kotlin.jvm.internal.ClassReference.<clinit>`

**Root Cause**: The `IklIsnnNWteL` class contains 49 obfuscated static String fields that are normally initialized by the native library at runtime. Even after adding a <clinit> to set them all to "placeholder", the Kotlin runtime still crashes when initializing ClassReference. This suggests the native library does more than just populate strings - it likely initializes critical data structures or patches Kotlin metadata in a way that cannot be easily reproduced.

### Attempt 13: Testing with x86_64 Native Libs

**Discovery**: The APKM bundle contains split APKs for all architectures including x86_64. Using APKEditor to merge splits produces an APK with x86_64 native libs.

**Testing**: Built APK with only x86_64 native libs + all smali patches (VMRunner, SignatureCheck, StartupLauncher, IklIsnnNWteL).

**Result**: No more SIGSEGV crash! The x86_64 native library loads correctly. BUT - same Kotlin ClassReference error persists.

**Conclusion**: This confirms the native library does MORE than populate strings. It likely patches Kotlin class metadata at runtime. Simply initializing strings to "placeholder" is insufficient - the native library performs critical initialization that cannot be easily reproduced.

### Attempt 14: Physical Device Testing

**Testing on CPH2447 (OnePlus, Android 13)**:

1. **Original APK**: Native library loads (`libpairipcore.so`) but aborts with `length_error was thrown in -fno-exceptions mode with message "vector"`. The integrity check is failing and calling abort().

2. **Patched APK (smali only)**: Native library doesn't load (System.loadLibrary removed), but app crashes with `kotlin.jvm.internal.ClassReference.<clinit>` NPE - same error as on AVD.

**Root Cause Analysis**:
- The native library (`libpairipcore.so`) exports only 3 functions: `ExecuteProgram`, `JNI_OnLoad`, `JNI_OnUnload`
- It uses `abort()` and `android_set_abort_message()` when checks fail
- It uses `dl_iterate_phdr` which could detect Frida or library injection
- Without the native library, Kotlin reflection fails because the library patches Kotlin metadata

**Frida Status**: Device blocks Frida spawning - "Failed to spawn: need Gadget to attach on jailed Android"

**Conclusion**: The pairip v404 integrity check fails on this physical device. The exact check is unknown but could be:
- SafetyNet / Play Integrity
- Device model / ROM detection
- Hardware ID verification
- Debug status detection

**Next steps would require**:
1. Find what check is failing (SafetyNet? Root? Other?)
2. Patch or bypass that specific check
3. Or create a mock native library that doesn't check but provides valid data

## Device Information

- **Device**: CPH2447 (OnePlus/OPPO)
- **Android**: 11.H.11_3110_202601281934
- **Root**: No
- **APK**: JioTV v7.1.7 (404), anti-split single APK
- **morphe-cli**: v1.6.3

## Attempt 15: APKEditor Smali Patching (Partial Success on x86_64 AVD)

### Approach

Used APKEditor to decompile merged APKM to smali, then patched:
1. `VMRunner.clinit` - Removed `System.loadLibrary("pairipcore")`
2. `VMRunner.executeVM` - Changed from native to stub returning null
3. `SignatureCheck.verifyIntegrity` - Return void immediately
4. `FirebaseInitProvider.onCreate` - Return false immediately
5. `IklIsnnNWteL` - Added `<clinit>` to initialize 49 static strings to ""

### Test Results on x86_64 AVD

**Progress**: 
- ✅ No native SIGSEGV crash
- ✅ Signature verification bypassed
- ✅ FirebaseInitProvider crash bypassed

**Still Failing**:
```
java.lang.ExceptionInInitializerError
  at com.jio.jioplay.tv.base.BaseActivity.onCreate
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
```

### Root Cause

The `ExceptionInInitializerError` confirms the native library does **deep Kotlin metadata patching** that cannot be reproduced via smali. The Kotlin runtime's `ClassReference` fails because the metadata it expects hasn't been properly initialized.

### Key Files Modified

- `/tmp/jiotv-decompiled/smali/classes2/com/pairip/VMRunner.smali`
- `/tmp/jiotv-decompiled/smali/classes2/com/pairip/SignatureCheck.smali`
- `/tmp/jiotv-decompiled/smali/classes7/com/google/firebase/provider/FirebaseInitProvider.smali`
- `/tmp/jiotv-decompiled/smali/classes2/com/jio/jioplay/tv/fingureprint/cka/IklIsnnNWteL.smali`

### Working APK

`/tmp/jiotv-patched4-aligned-debugSigned.apk` (on x86_64 AVD)

### Conclusion

For x86_64 AVD: Smali patching insufficient - needs mock native library.
For ARM64 device: Original library's integrity check fails.
