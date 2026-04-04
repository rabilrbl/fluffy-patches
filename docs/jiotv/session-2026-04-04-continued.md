# JioTV Patching - Session 2026-04-04 (Continued)

## Session Summary

Continued work on bypassing pairip DRM, Firebase crash, emulator/root detection, SSL pinning, and Play Store redirects in JioTV v7.1.7 (404).

## Key Findings This Session

### 1. FirebaseInitProvider.attachInfo() Patching

**Problem**: The original `disableFirebaseInitPatch` only patched `onCreate()`, but `attachInfo()` runs first and triggers the crash path.

**Attempt 1**: Patched `attachInfo(Context, ProviderInfo)` with `return-void`.
- **Result**: FAILED. The system calls `ContentProvider.attachInfo(Context, ProviderInfo, boolean)` (3-param version) which then calls the 2-param override. The patch only hit the 2-param version.

**Attempt 2**: Patched ALL methods named `attachInfo` regardless of parameter count, plus `onCreate()`.
- **Result**: FAILED. The bytecode patch still didn't work. Stack trace showed `FirebaseInitProvider.attachInfo(SourceFile:4)` still executing. Morphe redistributes `FirebaseInitProvider` across multiple dex files, and the patch only hits one copy.

**Attempt 3**: Removed `FirebaseInitProvider` from AndroidManifest.xml via resource patch.
- **Result**: PARTIAL SUCCESS. Firebase crash eliminated. App progresses past Firebase initialization.

### 2. ClassReference.<clinit> Crash

**After removing FirebaseInitProvider from manifest**, the app crashes with:

```
java.lang.ExceptionInInitializerError
    at com.jio.media.tv.ui.permission_onboarding.PermissionActivity.onCreate(SourceFile:12)
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
    at java.lang.StringBuilder.<init>(StringBuilder.java:136)
    at kotlin.jvm.internal.ClassReference.<clinit>(SourceFile:561)
```

**Root Cause**: Morphe's dex redistribution corrupts Kotlin class references. When morphe recompiles and redistributes classes across 12+ dex files, some Kotlin `ClassReference` static initializers get null class names, causing NPE during class loading.

**This is a fundamental limitation of morphe's bytecode patching** for this APK. The dex redistribution process breaks Kotlin's internal class reference mechanism.

### 3. APK Signing and Native Library Alignment

**Critical findings for APK repackaging**:
- Native libraries (`.so` files) MUST be stored (0% compression) in the APK, not deflated
- `resources.arsc` MUST be stored uncompressed for APKs targeting Android R+ (API 30+)
- APK must be zipaligned with 4-byte boundary after any modification
- APK must be re-signed with apksigner after modification
- The stub `libpairipcore.so` (6472 bytes, ARM64 ELF) works correctly when properly placed

**Correct workflow for adding stub .so to morphe-patched APK**:
```bash
# 1. Patch with morphe-cli
java -jar morphe-cli.jar patch -p patches.mpp -o patched.apk --force target.apk

# 2. Replace .so with stub (stored, not compressed)
cp patched.apk final.apk
unzip -qo patched.apk lib/arm64-v8a/libpairipcore.so
cp stub.so lib/arm64-v8a/libpairipcore.so
zip -d final.apk lib/arm64-v8a/libpairipcore.so
zip -0 final.apk lib/arm64-v8a/libpairipcore.so  # -0 = stored

# 3. Zipalign
zipalign -f -p -v 4 final.apk aligned.apk

# 4. Sign
apksigner sign --ks debug.keystore --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android \
  --out signed.apk aligned.apk

# 5. Install
adb install signed.apk
```

## Current Status

**BLOCKED**: Morphe's dex redistribution corrupts Kotlin class references, causing `ClassReference.<clinit>` crashes.

### What Works
- Stub `libpairipcore.so` prevents native VM crash
- FirebaseInitProvider can be removed from manifest (eliminates Firebase crash)
- All morphe patches compile and apply successfully
- APK can be properly aligned, signed, and installed

### What Doesn't Work
- Morphe's dex redistribution breaks Kotlin class references (ClassReference.<clinit> NPE)
- Bytecode patches on Firebase classes are unreliable due to dex redistribution
- Any approach that involves morphe recompiling dex files corrupts the APK

### Recommended Next Steps
1. **APKEditor approach**: Use APKEditor for direct APK modification without dex redistribution
2. **libpairipcorex.so**: Use Snailsoft's libpairipcorex.so replacement approach
3. **Frida/LSPosed**: Runtime hooking approach (requires root)
4. **Non-morphe bytecode patching**: Use baksmali/smali directly to modify dex files without redistribution

## Build Commands

```bash
# Build patches
export ANDROID_HOME=$HOME/Android/Sdk
GITHUB_ACTOR=anon GITHUB_TOKEN=dummy ./gradlew :patches:buildAndroid

# Output
patches/build/libs/patches-1.0.0-dev.9.mpp
```

## Files Modified This Session

- `patches/src/main/kotlin/app/template/patches/jiotv/misc/MiscPatches.kt` - Changed Firebase patch from bytecode to resource (manifest removal)
- `docs/jiotv/` - This session log

## Artifacts

- `/tmp/libpairipcore_stub.so` - Working ARM64 stub native library (6472 bytes)
- `/tmp/debug.keystore` - Debug keystore for APK signing
- `/tmp/jiotv-patched-v3.apk` - Last morphe-patched APK (has Firebase removed from manifest)
- `/tmp/jiotv-patched-v3-final.apk` - Signed version with stub .so
