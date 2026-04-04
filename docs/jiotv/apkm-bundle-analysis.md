# APKM Bundle Analysis

## Critical Discovery

The APKM bundle from APKMirror contains the **original Play Store APK** split into multiple files. The key finding: **pairip is NOT in the base.apk** — it's injected into the architecture split APKs.

## APKM Bundle Structure

```
com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_apkmirror.com.apkm
├── base.apk (73MB)                    # Main APK - NO pairip, valid Play Store signature
├── split_config.arm64_v8a.apk (578KB) # HAS libpairipcore.so (527KB)
├── split_config.armeabi_v7a.apk       # HAS libpairipcore.so (armeabi-v7a)
├── split_config.x86.apk               # HAS libpairipcore.so (x86)
├── split_config.x86_64.apk            # HAS libpairipcore.so (x86_64)
├── split_config.xxhdpi.apk            # DPI resources
└── ... (other DPI splits)
```

## Key Findings

### 1. base.apk is Clean
- **No** `libpairipcore.so`
- **No** pairip manifest entries
- **Valid** Play Store signature (META-INF/BNDLTOOL.SF/RSA)
- Application class: `com.jio.jioplay.tv.JioTVApplication` (not pairip's wrapper)
- All dex files match the antisplit APK exactly

### 2. Pairip is in Architecture Splits
The native library `libpairipcore.so` is only in the architecture-specific split APKs:
- `split_config.arm64_v8a.apk` → `lib/arm64-v8a/libpairipcore.so`
- `split_config.armeabi_v7a.apk` → `lib/armeabi-v7a/libpairipcore.so`
- `split_config.x86.apk` → `lib/x86/libpairipcore.so`
- `split_config.x86_64.apk` → `lib/x86_64/libpairipcore.so`

### 3. Antisplit APK Has Pairip Injected
The antisplit APK (`JioTV_v7.1.7(404)_antisplit.apk`) was created by merging all splits and has pairip injected into the base:
- `lib/arm64-v8a/libpairipcore.so` added to base.apk
- Manifest modified to use `com.pairip.application.Application`
- Pairip components added to manifest

### 4. Original APKM Works (With Pairip)
When installed via `adb install-multiple`, the original APKM bundle works — the app launches, shows `LicenseActivity`, and runs without crashing. This proves:
- The Play Store signature is valid
- Pairip's native VM passes integrity checks on the original APK
- The app functions normally with pairip intact

## Implications for Patching

### The Right Approach
Since pairip is in a **separate split APK**, we can:
1. Use the clean `base.apk` as our source (no pairip, valid signature)
2. Create a **fake split APK** with a stub `libpairipcore.so`
3. Install both together using `adb install-multiple`

This avoids:
- Modifying the base.apk (preserves valid signature)
- Triggering native VM integrity checks (stub replaces pairip)
- Dex recompilation (base.apk dex files stay untouched)

### Alternative: Single APK Approach
If we need a single APK (for morphe-cli compatibility):
1. Extract base.apk (clean, no pairip)
2. Add stub `libpairipcore.so` to `lib/arm64-v8a/`
3. Merge into single APK
4. Sign with debug key
5. The native VM will never trigger because the manifest doesn't reference pairip

## Testing Results

### Original APKM (install-multiple)
- ✅ App launches
- ✅ No crashes
- ✅ `LicenseActivity` shows
- ⚠️ App exits to Play Store after a few seconds (expected behavior)

### Next Steps
1. Create stub split APK with fake `libpairipcore.so`
2. Install with base.apk via `adb install-multiple`
3. Test if app works without pairip's native VM
4. If successful, create morphe patches that target the clean base.apk
