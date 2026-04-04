# APKM Bundle Analysis

## Critical Discovery

The APKM bundle from APKMirror contains the **original Play Store APK** split into multiple files.

## APKM Bundle Structure

```
com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_apkmirror.com.apkm
├── base.apk (73MB)                    # Main APK - HAS pairip in manifest, valid Play Store signature
├── split_config.arm64_v8a.apk (578KB) # HAS libpairipcore.so (527KB)
├── split_config.armeabi_v7a.apk       # HAS libpairipcore.so (armeabi-v7a)
├── split_config.x86.apk               # HAS libpairipcore.so (x86)
├── split_config.x86_64.apk            # HAS libpairipcore.so (x86_64)
├── split_config.xxhdpi.apk            # DPI resources
└── ... (other DPI splits)
```

## Key Findings

### 1. base.apk Has Pairip in Manifest
- **Has** pairip manifest entries (`com.pairip.application.Application`, `LicenseActivity`, `LicenseContentProvider`)
- **No** `libpairipcore.so` in base.apk itself
- **Valid** Play Store signature (META-INF/BNDLTOOL.SF/RSA)
- Application class: `com.pairip.application.Application` (pairip's wrapper)
- All dex files match the antisplit APK exactly

### 2. Pairip Native Library is in Architecture Splits
The native library `libpairipcore.so` is only in the architecture-specific split APKs:
- `split_config.arm64_v8a.apk` → `lib/arm64-v8a/libpairipcore.so`
- `split_config.armeabi_v7a.apk` → `lib/armeabi-v7a/libpairipcore.so`
- `split_config.x86.apk` → `lib/x86/libpairipcore.so`
- `split_config.x86_64.apk` → `lib/x86_64/libpairipcore.so`

### 3. Antisplit APK Has Pairip Injected into Base
The antisplit APK was created by merging all splits:
- `lib/arm64-v8a/libpairipcore.so` moved from split into base
- Manifest unchanged (already had pairip references from base.apk)
- All dex files identical to APKM base.apk

### 4. Original APKM Works (With Pairip)
When installed via `adb install-multiple`, the original APKM bundle works — the app launches, shows `LicenseActivity`, and runs without crashing. This proves:
- The Play Store signature is valid
- Pairip's native VM passes integrity checks on the original APK
- The app functions normally with pairip intact

## Implications for Patching

### The Split APK Problem
The base.apk has `isSplitRequired=true` and can't install standalone. This means:
1. We can't use base.apk alone as a clean starting point
2. Any merged APK requires re-signing, which breaks the native VM signature check
3. The antisplit APK is our only standalone option, but it requires re-signing

### What This Means
The native VM checks the APK signature. Since the APKM base.apk has a valid Play Store signature and the antisplit APK was re-signed, the native VM crashes on the antisplit version regardless of content.

**The only way to get a working patched APK without root is to either:**
1. Patch the native library itself to skip signature verification
2. Use the original APKM splits with a stub native library in the split
3. Find a way to preserve the original Play Store signature while modifying content (impossible by design)

## Testing Results

### Original APKM (install-multiple)
- ✅ App launches
- ✅ No crashes
- ✅ `LicenseActivity` shows
- ⚠️ App exits to Play Store after a few seconds (expected behavior)

### Antisplit APK (re-signed)
- ❌ Native VM crashes with SIGABRT (signature mismatch)

### Antisplit APK + Stub Native Library
- ✅ No native VM crash
- ❌ Firebase crashes without VM config data
