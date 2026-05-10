# Session 2026-05-09: Direct Dex Patching — Pairip Bypass Works

## Summary

Successfully bypassed pairip integrity verification using direct dex patching (baksmali/smali round-trip), bypassing Morphe's bytecode patching limitation. The app launches past pairip checks but crashes during Firebase and kotlinx.coroutines initialization because pairip-encrypted String fields are still null.

## Key Achievement

**Pairip bypass is confirmed working.** The `pairipcore-stub` loaded successfully, `SignatureCheck.verifyIntegrity()` returns void, `StartupLauncher.launch()` returns void, and `VMRunner.invoke()` returns null. The try-catch wrapper in `Application.attachBaseContext()` also works correctly.

Logcat confirmed:
```
I pairipcore-stub: pairipcore stub loaded — VM execution disabled
```

## Direct Dex Patching Approach

Morphe's `classDefBy()` reports success for pairip classes but doesn't actually modify them (likely a multi-dex resolution bug). Direct dex patching bypasses this:

1. **Extract dex files** from the Morphe-patched APK using `unzip`
2. **Decompile** individual dex files with `baksmali` (smali-2.5.2)
3. **Patch smali** for target classes
4. **Recompile** with `smali` assembler
5. **Replace** dex files in the APK using `zip`
6. **Fix manifest** with `apktool d -s` (resources only, skip dex) + manual XML edits + `apktool b`
7. **Align and sign** with `zipalign` and `apksigner`

### Patches Applied (Direct Smali)

| Class | Dex File | Patch |
|-------|----------|-------|
| `SignatureCheck` | classes3.dex | `verifyIntegrity()` → `return-void` |
| `StartupLauncher` | classes2.dex | `launch()` → `return-void` |
| `VMRunner` | classes11.dex | `invoke()` → `const/4 v0, 0x0; return-object v0` |
| `Application` | classes5.dex | `attachBaseContext()` → try-catch around `setContext()` and `verifyIntegrity()` |
| `FirebasePerfUrlConnection` | classes6.dex | `instrument()` → `return-object p0` (passthrough) |

### Manifest Changes

- `android:name` → `com.pairip.application.Application` (was changed to JioTVApplication by Morphe, which breaks multi-dex)
- `android:extractNativeLibs` → `true`
- `android:usesCleartextTraffic` → `true` (already set by Morphe)
- Removed `android:requiredSplitTypes` and `android:splitTypes`
- Removed `com.android.vending.splits.required`, `com.android.vending.splits`, `com.android.vending.derived.apk.id` meta-data
- Removed `FirebaseInitProvider` (causes NPE from null pairip strings)
- Removed `FirebasePerfKtxRegistrar` and `FirebasePerfRegistrar` component metadata

## Current Blocker: Null Pairip-Encrypted Strings

Without the pairip VM running, all pairip-encrypted `String` fields remain `null`. This causes cascading NPEs:

1. **First attempt** (with FirebaseInitProvider): `ExceptionInInitializerError` from `FirebaseApp.initializeApp()` — Guava's `ObjectArrays.checkElementsNotNull` throws NPE at index 6 because a Firebase config string is null.

2. **Second attempt** (without FirebaseInitProvider, without FirebasePerf registrars): `NoClassDefFoundError: RemoteConfigManager` because FirebasePerf class loading fails without its registrar.

3. **Third attempt** (without FirebaseInitProvider, with FirebasePerf passthrough): `ExceptionInInitializerError` from `kotlinx.coroutines.scheduling.DefaultScheduler` — `System.getProperty(null)` throws NPE because the property key string is null.

**Root cause**: The pairip VM decrypts String fields at class load time via `<clinit>` blocks. Without the VM, these fields stay null.

**Fix needed**: Apply clinit string injection patches from heap dump data (`generate_clinit_v3.py` + `hprof_static_fields.json`). This pre-populates 1,472 String fields across 37 classes with their decrypted values.

## Build Pipeline (Working)

```bash
# 1. Start from Morphe-patched APK
cp jiotv-base-patched.apk /tmp/dex-patch/work/base.apk

# 2. Extract dex files
unzip base.apk "classes*.dex" -d extracted/

# 3. Decompile target dex files with baksmali
java -jar baksmali.jar d classes2.dex -o smali_classes2/
java -jar baksmali.jar d classes3.dex -o smali_classes3/
# ... for each target dex

# 4. Patch smali files (manual editing)

# 5. Recompile with smali
java -jar smali.jar a smali_classes2/ -o classes2-new.dex
# ... for each patched dex

# 6. Build final APK
# - apktool d -s to get manifest
# - Edit AndroidManifest.xml
# - apktool b to rebuild (preserves dex files with -s flag)
# - Replace patched dex files in the zip
# - Add libpairipcore.so stubs
# - zipalign + apksigner
```

## Tools

- `baksmali`/`smali` v2.5.2 (downloaded from GitHub)
- `apktool` 2.x (for manifest editing only)
- `zipalign`/`apksigner` from Android SDK build-tools 34.0.0
- NDK-built `libpairipcore.so` stub (x86_64 + arm64-v8a)

## Next Steps (Priority Order)

1. **Apply clinit string patches** from heap dump data to pre-populate null String fields
2. **Test app launch** after clinit patches
3. **Test content playback** (may need SSL pinning bypass)
4. **Integrate all patches** into the Morphe patch framework
5. **Clean up build pipeline** into a reproducible script

## Known Issues

- `apktool` full rebuild (without `-s`) breaks multi-dex class loading — always use `-s` for dex preservation
- Morphe `classDefBy()` doesn't actually modify pairip classes in secondary dex files
- The `FirebasePerfUrlConnection.instrument()` passthrough patch prevents Firebase Perf from crashing the ad SDK, but doesn't fix the root cause of null strings
- `libpairipcore.so` stub must be stored uncompressed (`-0` flag in zip) for `extractNativeLibs=true`