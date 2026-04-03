# JioTV Patches

## Overview

Patches for **JioTV** (`com.jio.jioplay.tv`) v7.1.7 (404).

## Patches

### 1. Disable pairip license check (manifest)
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Resource patch
- **What**: Removes `LicenseContentProvider` from manifest to prevent auto-initialization of license checking

### 2. Remove Play Store license check
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Bytecode patch
- **What**: Bypasses three distinct Play Store redirect mechanisms:

#### pairip DRM bypass
- `SignatureCheck.verifyIntegrity` - Bypasses APK signature verification
- `LicenseClient.initializeLicenseCheck` - No-op (virtual method)
- `LicenseClient.connectToLicensingService` - No-op
- `LicenseClient.processResponse` - No-op
- `LicenseClient.startPaywallActivity` - No-op
- `LicenseClient.startErrorDialogActivity` - No-op
- `LicenseClient.handleError` - No-op
- `LicenseActivity.onStart` - Immediately finishes activity (virtual method)

#### Server-driven update check bypass
- `CommonUtils.checkIsUpdateAvailable` - No-op (prevents API call)
- `CommonUtils.redirectToPlayStore` - No-op
- `CommonUtils.takeToPlayStore` - No-op

#### Google Play Core in-app update bypass
- `AppUpdateHelper.checkUpdate` - No-op (virtual method)
- `AppUpdateHelper.checkUpdatefordiag` - No-op (virtual method)
- `AppUpdateHelper.a` - No-op (static method, blocks `startUpdateFlowForResult`)

**Note**: The pairip VM (`VMRunner`, `StartupLauncher`) is left intact. The app's actual code is encrypted in `assets/` and executed by the native VM — neutralizing it causes an immediate splash screen crash.

### 3. Remove emulator detection
- **File**: `emulator/RemoveEmulatorDetectionPatch.kt`
- **Type**: Bytecode patch
- **What**:
  - `isRunningOnEmulator()` → returns `false`
  - `isSupportedDevice()` → returns `true`
  - `onCreate()` → calls super + proceeds directly, skipping detection

### 4. Remove root detection
- **File**: `root/RemoveRootDetectionPatch.kt`
- **Type**: Bytecode patch
- **What**:
  - `CommonUtils.isRooted()` → returns `false`
  - `SecurityUtils.isValidVersionName()` → returns `true`
  - `CommonUtils.showXposedFrameworkDetectionDialog()` → no-op

### 5. Remove certificate pinning
- **File**: `sslpinning/RemoveCertificatePinningPatch.kt`
- **Type**: Bytecode patch
- **What**:
  - `FirebaseConfig.isSslPining()` → returns `false`
  - `CertificatePinner.check$okhttp()` → no-op (OkHttp3)
  - `CertificatePinner.check()` → no-op (legacy OkHttp, best-effort)

### 6. Enable cleartext traffic
- **File**: `misc/MiscPatches.kt`
- **Type**: Resource patch
- **What**:
  - Sets `usesCleartextTraffic="true"` in manifest
  - Rewrites `network_security_config.xml` to allow cleartext and user CAs

## Method Type Reference

| Class | Method | Type |
|-------|--------|------|
| `Lcom/pairip/SignatureCheck;` | `verifyIntegrity` | direct |
| `Lcom/pairip/licensecheck/LicenseClient;` | `initializeLicenseCheck` | **virtual** |
| `Lcom/pairip/licensecheck/LicenseClient;` | `connectToLicensingService` | direct |
| `Lcom/pairip/licensecheck/LicenseClient;` | `processResponse` | direct |
| `Lcom/pairip/licensecheck/LicenseClient;` | `startPaywallActivity` | direct |
| `Lcom/pairip/licensecheck/LicenseClient;` | `startErrorDialogActivity` | direct |
| `Lcom/pairip/licensecheck/LicenseClient;` | `handleError` | direct |
| `Lcom/pairip/licensecheck/LicenseActivity;` | `onStart` | **virtual** |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `checkIsUpdateAvailable` | direct |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `redirectToPlayStore` | direct |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `takeToPlayStore` | direct |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `checkUpdate` | **virtual** |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `checkUpdatefordiag` | **virtual** |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `a` | direct (static) |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isRunningOnEmulator` | **virtual** |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isSupportedDevice` | **virtual** |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `onCreate` | **virtual** |
| `Lcom/google/firebase/crashlytics/internal/common/CommonUtils;` | `isRooted` | direct |
| `Lcom/jio/jioplay/tv/utils/SecurityUtils;` | `isValidVersionName` | direct |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `showXposedFrameworkDetectionDialog` | direct |
| `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;` | `isSslPining` | **virtual** |
| `Lokhttp3/CertificatePinner;` | `check$okhttp` | **virtual** |
