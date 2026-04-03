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

```
Android Framework
  └── com.pairip.application.Application.attachBaseContext()
        ├── VMRunner.setContext(context)           ← sets context for VMRunner
        ├── SignatureCheck.verifyIntegrity(context) ← APK signature check (throws on tamper)
        └── super.attachBaseContext(context)
              └── JioTVApplication.attachBaseContext()
                    └── MultiDex.install(this)
  └── StartupLauncher.launch()
        └── VMRunner.invoke("mVBwD2didVTgj5k7", null)
              ├── VmDecryptor.decrypt()             ← decrypts bytecode from assets/
              └── executeVM(bytecode, args)         ← native call to libpairipcore.so
  └── com.jio.jioplay.tv.JioTVApplication.onCreate() ← actual app initialization
```

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

## Bypass Strategy

### Manifest Level
- Remove `LicenseContentProvider` from manifest (prevents auto-initialization)
- **Do NOT** change the application class — `com.pairip.application.Application` is needed for VM initialization

### Bytecode Level
- `SignatureCheck.verifyIntegrity` → `return-void` (direct method)
- `LicenseClient.initializeLicenseCheck` → `return-void` (virtual method)
- `LicenseClient.connectToLicensingService` → `return-void` (direct method)
- `LicenseClient.processResponse` → `return-void` (direct method)
- `LicenseClient.startPaywallActivity` → `return-void` (direct method)
- `LicenseClient.startErrorDialogActivity` → `return-void` (direct method)
- `LicenseClient.handleError` → `return-void` (direct method)
- `LicenseActivity.onStart` → `super.onStart(); finish(); return-void` (virtual method)
