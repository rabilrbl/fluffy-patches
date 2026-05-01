# pairip DRM Library Analysis

## Overview

pairip is a **native DRM solution** injected into the JioTV APK. It:
- Loads `libpairipcore.so` native library
- Executes encrypted VM bytecode from `assets/` directory
- Performs APK signature verification
- Manages Google Play license checking
- Shows paywall/error dialogs

## Runtime Behavior (confirmed 2026-04-30)

On the **original signed split install** (v371):
- `SignatureCheck: Signature check ok` — passes and logs to logcat
- `LicenseClientV3: Connecting to the licensing service...` — connects to Play licensing
- **libpairipcore.so loads, executes, and unloads in < 100ms** — too fast to catch in /proc/maps
- After verification, the library is dlclose'd and unmapped from process memory
- This means: runtime memory dumping of the decrypted .so is extremely difficult

### Anti-Frida Detection (confirmed)
- **Frida spawn mode (`frida -f`) is detected and kills the process immediately**
- Even minimal polling scripts (no Interceptor.attach) are detected
- frida-server-stealth (from previous sessions) is also detected
- The VM bytecode contains explicit checks: `android/os/Debug` → `isDebuggerConnected`,
  `/proc/self/maps`, `/proc/self/status`, `waitingForDebugger`

## Key Classes

| Class | Smali Name | Purpose |
|-------|------------|---------|
| `VMRunner` | `Lcom/pairip/VMRunner;` | Native library loader and VM executor |
| `StartupLauncher` | `Lcom/pairip/StartupLauncher;` | Entry point that triggers VM execution |
| `SignatureCheck` | `Lcom/pairip/SignatureCheck;` | APK signature verification (SHA-256) |
| `Application` | `Lcom/pairip/application/Application;` | Extends JioTVApplication, runs checks in `attachBaseContext` |
| `LicenseContentProvider` | `Lcom/pairip/licensecheck/LicenseContentProvider;` | Auto-initializes license checking at app startup |
| `LicenseClient` | `Lcom/pairip/licensecheck/LicenseClient;` | Manages license checking, paywall, error dialogs |
| `LicenseActivity` | `Lcom/pairip/licensecheck/LicenseActivity;` | Shows paywall/error dialog UI |
| `InitContextProvider` | `Lcom/pairip/InitContextProvider;` | Fallback context creator via reflection |

## Startup Flow

The native VM runs in a **static initializer**, before `attachBaseContext()`:

```
MultiDexApplication.<clinit>()                      ← STATIC INITIALIZER (earliest possible)
  └── StartupLauncher.launch()
        └── VMRunner.invoke("mVBwD2didVTgj5k7", null)
              ├── VmDecryptor.decrypt()             ← decrypts bytecode from assets/
              └── executeVM(bytecode, args)         ← native call to libpairipcore.so
                    ├── Dex integrity verification (CRC32/structural hashes)
                    ├── APK signature verification (redundant with Java-level)
                    ├── Anti-debugging checks
                    ├── On FAIL → startActivity(paywall URL) via JNI + SIGABRT
                    └── On PASS → execute encrypted app bytecode

Android Framework
  └── com.pairip.application.Application.attachBaseContext()
        ├── VMRunner.setContext(context)           ← sets context for VMRunner
        ├── SignatureCheck.verifyIntegrity(context) ← Java-level signature check (redundant)
        └── super.attachBaseContext(context)
              └── JioTVApplication.attachBaseContext()
                    └── MultiDex.install(this)

  └── com.jio.jioplay.tv.JioTVApplication.onCreate() ← actual app initialization
```

**Critical**: Because the VM runs in `<clinit>()`, it executes before ANY instance method. This means native integrity checks happen before we can intercept anything at the Java level.

## License Check Flow

```
LicenseContentProvider.onCreate()
  └── new LicenseClient(context).initializeLicenseCheck()
        ├── Local installer check
        ├── connectToLicensingService() → binds to Google Play Licensing service
        ├── processResponse(responseCode)
        │     ├── responseCode == 0 (LICENSED) → allow app
        │     ├── responseCode == 1 (NOT_LICENSED) → handleError() → show error dialog
        │     └── responseCode == 2 (RETRY/OTHER) → startPaywallActivity()
        └── LicenseActivity
              ├── ActivityType.PAYWALL → shows "Get this app from Play" dialog
              ├── ActivityType.ERROR_DIALOG → shows error dialog
              └── closeApp() → finishAndRemoveTask() + System.exit(0)
```

## Splash Screen Crash (Critical)

**The app's actual Java code is encrypted** inside the `assets/` directory. The pairip native library (`libpairipcore.so`) contains a custom VM that decrypts and executes this bytecode at runtime.

**Crash cause**: Neutralizing `VMRunner.<clinit>`, `setContext`, or `StartupLauncher.launch` prevents the VM from ever running. The app has no code to execute → immediate crash on splash screen.

**Correct approach**: Only patch:
- `SignatureCheck.verifyIntegrity` — bypass APK signature verification
- `LicenseClient` methods — disable license checking, paywall, error dialogs
- `LicenseActivity.onStart` — auto-finish any paywall that somehow appears

**Do NOT patch**:
- `VMRunner.<clinit>` — loads `libpairipcore.so`
- `VMRunner.setContext` — sets context for VM
- `StartupLauncher.launch` — triggers VM execution

## Native VM Integrity Checks

The native library `libpairipcore.so` performs 100+ security checks at the native level:

### Checks Performed
- **Dex file CRC32/structural hash verification** — detects ANY modification to dex files
- **APK signature validation** — redundant with Java-level `SignatureCheck`
- **Anti-debugging checks** — detects debuggers, frida, etc.
- **Environment detection** — checks for emulators, rooted devices, etc.

### Failure Behavior
When integrity checks fail:
1. `InitContextProvider` creates a `Context` via reflection on `ActivityThread`
2. Sets up an `Instrumentation` instance (allows `startActivity()` before `Application.onCreate()`)
3. Calls `Context.startActivity()` with `Intent(ACTION_VIEW, "http://play.google.com/store/license/paywall?id=com.jio.jioplay.tv")` — directly via JNI
4. Crashes with `SIGABRT`: `length_error in vector` (C++ `std::vector` bounds error in `-fno-exceptions` mode → `std::terminate()`)

**Log excerpt** (pairip v404):
```
nativeloader: Load libpairipcorex.so ... ok
nativeloader: Load libpairipcore.so ... ok
libc++: length_error was thrown in -fno-exceptions mode with message "vector"
Process ... exited due to signal 31
```

### Why Dex-Level Patching Alone Cannot Work
Any dex modification (which morphe-cli always does) changes CRC32/structural hashes. The native VM detects this at the native level, before any Java code runs. This is the **fundamental limitation** of APK-modification-based patching for pairip-protected apps.

## Heap-Dump-Based Bypass (Current Approach)

Since runtime hooking (Frida/Xposed) is detected by pairip, and the native library loads/unloads in <100ms (cannot be dumped), the working approach is:

### 1. Replace `libpairipcore.so` with a stub
- Build a minimal native library (`libpairipcore-stub.so`) that exports:
  - `JNI_OnLoad` → returns `JNI_VERSION_1_6`
  - `Java_com_pairip_VMRunner_executeVM` → returns `null` (jobject)
  - `com_pairipcore_ExecuteProgram` → returns `null`
  - `JNI_OnUnload` → returns void
- This prevents the native VM from running ANY integrity checks
- The stub is ~5KB vs the original 475KB encrypted binary

### 2. Hardcode pairip-populated fields via `<clinit>` blocks
- pairip's VM populates **1,752 static String fields** across **52 obfuscated classes** at runtime
- Without the VM running, these fields remain `null`, causing NPEs
- Solution: heap-dump the app on a rooted device, extract field values from HPROF, inject them as `<clinit>` blocks in smali
- **38/52 classes** found in heap dump → **1,472/1,752 fields** resolved
- **14/52 classes** not in heap (not yet loaded) → **30 fields unresolved** + **164 primitive fields** set to default values
- 10 type-mismatch fields (non-String) are skipped

### 3. Build & Sign
- Replace `libpairipcore.so` in x86_64 split with stub
- Use `apktool` to rebuild base APK with patched smali
- Sign all splits with debug keystore
- Install via `adb install-multiple`

### Key Technical Challenges Resolved

| Issue | Solution |
|-------|----------|
| CleverTapAPI `NoSuchFieldError: field i type String` | v2/v3 generator reads actual smali field types, skips non-String fields |
| Literal `\t`/`\r`/`\n` in const-string breaking smali | v3 uses binary (bytes) file I/O, no Python string interpretation |
| `\/` invalid smali escape in regex strings | v3 escapes backslash → `\\` (double backslash in file) |
| `re.sub()` corrupting binary replacement data | v3 uses manual byte-level find-replace with slicing |
| `65536 Unsigned short overflow` in DEX (method/string indices >65K) | Use `const-string/jumbo` (32-bit string index) instead of `const-string` |
| classes5.dex has exactly 65,536 methods (overflow on recompile) | Move 4 patched classes from classes5 to classes10 (room for growth) |
| `No implementation found for VMRunner.executeVM` | Added `Java_com_pairip_VMRunner_executeVM` JNI export to stub |

### Firebase NPE Fixes (v18i/v18j)

|| Crash | Cause | Fix |
||-------|-------|-----|
|| `FirebaseCrashlytics.getInstance()` NPE in `v2.onComplete` | `getInstance()` **throws** NPE (not returns null) when Crashlytics component isn't initialized | Wrapped in try-catch Exception in `v2.smali` |
|| `FirebasePerformance.setPerformanceCollectionEnabled()` NPE in `FirebaseConfigUtil.a` | `FirebasePerformance.getInstance()` returns null in emulator | Wrapped `FirebaseConfigUtil.a()` call in try-catch in `v2.onComplete` |
|| `APIManager.getNormalHttpClient` VerifyError (v18e) | Smali null-check branch caused register type inference failure | Reverted — FirebasePerformance block already has try-catch in original code |

Note: `FirebaseCrashlytics.getInstance()` throws an NPE internally ("FirebaseCrashlytics component is not present") — a null check on the return value is insufficient. Only try-catch Exception works.

### Status: ✅ WORKING (v18j)

JioTV v18j launches and runs on AVD (Pixel 4 API 30, rooted). No FATAL EXCEPTION crashes. App displays AppLogo activity successfully. Only benign network errors (analytics DNS resolution failures — expected on emulator).

## SignatureCheck Field Values

| Field | Value |
|-------|-------|
| `expectedSignature` | `VkwE0TgslZMpxvR+ldSXr9FRIQ5NlCaBT+tvpXr3rTA=` |
| `expectedLegacyUpgradedSignature` | (same as expectedSignature) |
| `expectedTestSignature` | (same as expectedSignature) |
| `ALLOWLISTED_SIG` | `Vn3kj4pUblROi2S+QfRRL9nhsaO2uoHQg6+dpEtxdTE=` |
| Our APK cert hash | `MpWsyp43Cdc9I5z/G6d8/6/a7cistsJdgxKXDrrT8z4=` |

## Bypass Strategy

### Manifest Level
- Remove `LicenseContentProvider` from manifest (prevents auto-initialization)
- Remove `PlayCoreDialogWrapperActivity` from manifest (blocks Play Core wrapper)
- **Do NOT** change the application class — `com.pairip.application.Application` is needed for VM initialization

### Bytecode Level (Java-Layer Patches)

These patches handle the Java-level license check flow. They are necessary but **insufficient** — the native VM integrity check must also be addressed.

- `SignatureCheck.verifyIntegrity` → set expected signatures to our cert hash (direct method)
- `LicenseClient.initializeLicenseCheck` → `return-void` (virtual method)
- `LicenseClient.connectToLicensingService` → `return-void` (direct method)
- `LicenseClient.processResponse` → `const/4 p1, 0x0` (force LICENSED, pairipfix-style) (direct method)
- `LicenseResponseHelper.validateResponse` → `return-void` (no-op JWS validation) (direct method)
- `LicenseClient.startPaywallActivity` → `return-void` (direct method)
- `LicenseClient.startErrorDialogActivity` → `return-void` (direct method)
- `LicenseClient.handleError` → `return-void` (direct method)
- `LicenseActivity.onStart` → `super.onStart(); finish(); return-void` (virtual method)

### Known Working Solutions (Require Root or Runtime Hooking)

#### pairipfix (LSPosed Module)
- Source: https://github.com/ahmedmani/pairipfix
- **Approach**: Runtime hooks via LSPosed/Xposed framework — does NOT modify the APK
- Hooks `LicenseClient.processResponse()` to force `responseCode = 0` (LICENSED)
- Hooks `ResponseValidator.validateResponse()` → DO_NOTHING
- Does NOT handle native VM crash — recommends BetterKnownInstalled
- Works because native VM integrity checks pass (APK is unmodified)

#### BetterKnownInstalled (Magisk Module)
- Source: https://github.com/Pixel-Props/BetterKnownInstalled
- **Approach**: Modifies `/data/system/packages.xml` at boot to fake Play Store installer
- Sets: `installer=com.android.vending`, `installInitiator=com.android.vending`, `installerUid`, `packageSource=2`
- Makes sideloaded apps appear as Play Store installs to the system
- Requires root (Magisk/KernelSU)

#### Reverse-Engineering libpairipcore.so
- Possible but extremely complex — native code is obfuscated
- Would need to find and patch the integrity check routines in the .so file
- Not attempted

### Package ID Change (Not Viable)
Changing the package ID or app name **will not work**:
- The native VM would detect the package name change
- The app's encrypted code in `assets/` references the original package name
- pairip's paywall URL embeds the package ID: `play.google.com/store/license/paywall?id=com.jio.jioplay.tv`
