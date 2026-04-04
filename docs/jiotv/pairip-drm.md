# pairip DRM Library Analysis

## Overview

pairip is a **native DRM solution** injected into the JioTV APK. It:
- Loads `libpairipcore.so` native library
- Executes encrypted VM bytecode from `assets/` directory
- Performs APK signature verification
- Manages Google Play license checking
- Shows paywall/error dialogs

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

### Why Dex-Level Patching Cannot Work
Any dex modification (which morphe-cli always does) changes CRC32/structural hashes. The native VM detects this at the native level, before any Java code runs. This is the **fundamental limitation** of APK-modification-based patching for pairip-protected apps.

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
