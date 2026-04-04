# Native Library Bypass Attempts

## Overview

This document tracks all attempts to bypass `libpairipcore.so` native integrity checks, which are the fundamental blocker for dex-level APK patching.

## The Core Problem

```
Any dex modification → morphe-cli recompiles dex → native VM detects changed hashes → SIGABRT + Play Store paywall
```

The native VM (`libpairipcore.so`) performs 100+ security checks including dex file CRC32/structural hash verification. **Any dex modification triggers failure**, regardless of what Java-level patches are applied.

## Attempt 1: CRC32 Restoration (SafaSafari Method)

### Approach
After morphe-cli patches and recompiles dex files, restore the original CRC32 values in the ZIP central directory. The native VM reads CRC32 from ZIP metadata, not from actual dex content.

### Implementation
```python
python3 /tmp/crc32_patcher.py patched.apk original.apk
```

### Result: **FAILED**

All 9 dex files had their CRC32 values restored to originals, but the app still crashed with:
```
libc++abi: length_error was thrown in -fno-exceptions mode with message "vector"
libpairipcore.so → SIGABRT
```

### Why It Failed
The native VM does **structural hash verification** beyond just ZIP CRC32. It likely computes its own hash of the actual dex file content at runtime, comparing against expected values stored in the encrypted VM bytecode. The CRC32 in the ZIP central directory is irrelevant to the native VM's actual verification.

### Additional Issue
The CRC32 patcher strips the APK v2/v3 signing block (Python zipfile doesn't preserve it), requiring re-signing after restoration.

---

## Attempt 2: Stub Native Library Replacement

### Approach
Replace `libpairipcore.so` with a stub library that exports the same JNI symbols (`executeVM`) but does nothing (returns null). Since the app's Java code is NOT encrypted (only the VM bytecode is), the app should work normally without the VM.

### Implementation
```c
// Stub executeVM - returns null without doing anything
JNIEXPORT jobject JNICALL
Java_com_pairip_VMRunner_executeVM(JNIEnv *env, jclass clazz, jbyteArray vmCode, jobjectArray args) {
    return NULL;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    // Register executeVM via RegisterNatives
    JNINativeMethod methods[] = {
        {"executeVM", "([B[Ljava/lang/Object;)Ljava/lang/Object;", (void *)Java_com_pairip_VMRunner_executeVM}
    };
    (*env)->RegisterNatives(env, vmRunnerClass, methods, 1);
    return JNI_VERSION_1_6;
}
```

Compiled with NDK r27:
```bash
$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang \
    -shared -fPIC -o libpairipcore_stub.so stub_pairipcore.c -llog
```

### Result: **PARTIAL SUCCESS**

The native VM crash was eliminated — no more `libpairipcore.so` SIGABRT. However, the app crashed during Firebase initialization:

```
java.lang.RuntimeException: Unable to get provider com.google.firebase.provider.FirebaseInitProvider:
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
    at com.google.firebase.internal.DataCollectionConfigStorage.<init>
    at com.google.firebase.FirebaseApp.lambda$new$0
    at com.google.firebase.crashlytics.FirebaseCrashlytics.init
```

### Why It Failed
The VM bytecode (executed by `executeVM`) likely initializes Firebase configuration or returns data that Firebase needs. Returning `null` from the stub breaks this initialization chain. The VM does more than just integrity checks — it may also set up runtime configuration.

### Key Insight
The stub approach proves that:
1. The native VM is the ONLY thing preventing the app from launching
2. The app's Java code is fully functional and NOT encrypted
3. We need the VM to run successfully, not be disabled

---

## Attempt 3: Resource-Only Patches (No Dex Modifications)

### Approach
Only modify resources (AndroidManifest.xml, network_security_config.xml) without touching any dex files. The native VM should see unmodified dex files and pass integrity checks.

### Implementation
- Changed manifest application class from `com.pairip.application.Application` to `com.jio.jioplay.tv.JioTVApplication`
- Removed pairip components from manifest
- Fixed network security config
- Disabled ALL bytecode patches

### Result: **FAILED**

Morphe-cli **always recompiles dex files** during the patching process, even when no bytecode patches are applied. The dex file sizes changed significantly:

```
classes.dex:    11,244,936 → 8,181,148 bytes
classes2.dex:     911,028 → 8,248,064 bytes
...
```

Additionally, morphe-cli redistributed classes across dex files (original: 9 dex, patched: 12 dex). The native VM detected these changes and crashed.

### Key Finding
**Morphe-cli cannot produce an APK with unmodified dex files.** The patching process inherently recompiles all dex files, which triggers the native VM integrity checks.

---

## Attempt 4: Apktool Build

### Approach
Use apktool to decode and rebuild the APK, modifying only resources. Apktool might preserve original dex files.

### Result: **FAILED**

Apktool also recompiles dex files from smali, even when "smali has not changed." The smali-to-dex compilation produces different output (different sizes, different content) due to different smali versions or compilation settings.

---

## What We Know Works (From External Research)

### Snailsoft's Approach (Sbenny Forum)
Uses a complete pipeline:
1. APKEditor to merge split APKs
2. Decompile with APKEditor
3. Modify manifest and smali
4. Replace `libpairipcore.so` with `libpairipcorex.so` (bypassed version)
5. Rebuild and sign

The key: **replacing the native library with a bypassed version**, not a stub.

### SafaSafari's Approach
1. Change manifest application class
2. Restore CRC32 values after patching

This was reported to work for some apps but failed for JioTV (structural hash verification beyond CRC32).

### LSPosed (pairipfix)
Runtime hooks that don't modify the APK at all. This is the most reliable approach but requires root + LSPosed.

## Attempt 5: Manual APK Reconstruction (Original Dex + Modified Manifest)

### Approach
Manually construct an APK with:
- **Original dex files** (unmodified, from original APK)
- **Modified manifest** (from apktool build, with pairip components removed)
- **Modified resources.arsc** (from apktool build)
- **Original native libraries** (stored, not compressed)
- **New signature** (debug key)

### Implementation
1. Extract original APK
2. Replace AndroidManifest.xml with apktool-compiled version
3. Replace resources.arsc with apktool-compiled version
4. Replace res/ directory with apktool-compiled version
5. Keep all original dex files (verified SHA-256 match)
6. Keep all original native libraries (stored, not compressed)
7. Sign with debug key and install

### Result: **FAILED**

Still crashes with `length_error in vector` → SIGABRT in `libpairipcore.so`, even though all dex files are 100% identical to the original.

### Why It Failed
The native VM detects something beyond dex file content. Possibilities:
1. **APK signature change** — VM verifies the APK was signed with the original key
2. **resources.arsc changes** — VM checks resource table integrity
3. **Manifest changes** — VM checks that pairip components are present
4. **ZIP structure changes** — VM checks APK file structure/ordering

### Key Finding
Even with 100% identical dex files, the native VM still crashes. This means the integrity checks go far beyond dex CRC32 — they likely include:
- APK signature verification (the debug key doesn't match the original)
- Full APK hash verification
- Manifest component verification
- Resource table verification

## Attempt 7: Re-signed Original APK (Zero Content Changes)

### Approach
Take the original APK, re-sign it with a debug key (no content modifications at all), and test. This isolates whether the native VM checks the APK signature.

### Implementation
1. Used APKEditor to refactor the original APK (no content changes)
2. Re-signed with debug key
3. Installed and tested

### Result: **FAILED**

Still crashes with `length_error in vector` → SIGABRT in `libpairipcore.so`.

### Critical Finding
**The native VM checks the APK signature.** Even with ZERO content modifications, re-signing with a different key triggers the crash. This means:

1. The VM verifies the APK was signed with the original production key
2. ANY re-signing (debug key, different production key) will be detected
3. The signature check happens BEFORE dex content verification
4. This is the PRIMARY integrity check — if it fails, everything else is moot

### Implications
This fundamentally limits all non-root approaches:
- **Morphe patches**: Always re-sign → always detected
- **Apktool rebuild**: Always re-signs → always detected
- **Manual APK reconstruction**: Always re-signs → always detected
- **CRC32 restoration**: Irrelevant — signature check happens first

### Only Viable Approaches
1. **Runtime hooking** (LSPosed/Frida) — no APK modification needed
2. **System-level faking** (BetterKnownInstalled) — no APK modification needed
3. **Native library patching** — patch the .so to skip signature check, then re-sign
4. **Original APK with valid signature** — but the original APK's signature appears broken

## Attempt 8: Native Library Patching (In Progress)

### Approach
Reverse engineer `libpairipcore.so` to find and patch the signature verification function. The goal is to make the VM always pass signature checks, regardless of what key the APK is signed with.

### Strategy
1. Find the function that verifies the APK signature
2. Patch it to always return "valid"
3. Replace the patched .so in the APK
4. Re-sign and test

## Device Information

- **Device**: CPH2447 (OnePlus/OPPO)
- **Android**: 11.H.11_3110_202601281934
- **Root**: No
- **LSPosed**: No
- **APK**: JioTV v7.1.7 (404), anti-split single APK
- **morphe-cli**: v1.6.3
