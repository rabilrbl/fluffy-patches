# JioTV Patches

## Overview

Patches for **JioTV** (`com.jio.jioplay.tv`) v7.1.7 (404).

## Patches

### 1. Disable pairip license check (manifest)
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Resource patch
- **What**: Removes `LicenseContentProvider` from manifest, changes application class to bypass pairip initialization

### 2. Remove Play Store license check
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Bytecode patch
- **Depends on**: Disable pairip license check (manifest)
- **What**: Neutralizes 16 pairip and Play Store redirect methods:
  - `VMRunner.<clinit>` - Prevents native library loading
  - `VMRunner.setContext` - No-ops context setting
  - `StartupLauncher.launch` - Prevents VM bytecode execution
  - `SignatureCheck.verifyIntegrity` - Bypasses signature verification
  - `Application.attachBaseContext` - Redirects to real JioTVApplication
  - `LicenseContentProvider.onCreate` - Returns true without initializing LicenseClient
  - `LicenseClient.initializeLicenseCheck` - No-op
  - `LicenseClient.connectToLicensingService` - No-op
  - `LicenseClient.processResponse` - No-op
  - `LicenseClient.startPaywallActivity` - No-op
  - `LicenseClient.startErrorDialogActivity` - No-op
  - `LicenseClient.handleError` - No-op
  - `LicenseActivity.onStart` - Immediately finishes activity
  - `CommonUtils.checkIsUpdateAvailable` - No-op
  - `CommonUtils.redirectToPlayStore` - No-op
  - `CommonUtils.takeToPlayStore` - No-op

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
| `Lcom/pairip/VMRunner;` | `<clinit>` | direct |
| `Lcom/pairip/VMRunner;` | `setContext` | direct |
| `Lcom/pairip/StartupLauncher;` | `launch` | direct |
| `Lcom/pairip/SignatureCheck;` | `verifyIntegrity` | direct |
| `Lcom/pairip/application/Application;` | `attachBaseContext` | **virtual** |
| `Lcom/pairip/licensecheck/LicenseContentProvider;` | `onCreate` | **virtual** |
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
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isRunningOnEmulator` | **virtual** |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isSupportedDevice` | **virtual** |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `onCreate` | **virtual** |
| `Lcom/google/firebase/crashlytics/internal/common/CommonUtils;` | `isRooted` | direct |
| `Lcom/jio/jioplay/tv/utils/SecurityUtils;` | `isValidVersionName` | direct |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `showXposedFrameworkDetectionDialog` | direct |
| `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;` | `isSslPining` | **virtual** |
| `Lokhttp3/CertificatePinner;` | `check$okhttp` | **virtual** |
