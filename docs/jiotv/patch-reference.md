# Patch Reference

## Smali Name Mapping Table

| JADX Name | Real Smali Name |
|-------------|----------------|
| `com.jio.jioplay.p037tv.utils.CommonUtils` | `Lcom/jio/jioplay/tv/utils/CommonUtils;` |
| `com.jio.jioplay.p037tv.utils.SecurityUtils` | `Lcom/jio/jioplay/tv/utils/SecurityUtils;` |
| `com.jio.jioplay.p037tv.utils.AppUpdateHelper` | `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` |
| `com.jio.jioplay.p037tv.data.firebase.FirebaseConfig` | `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;` |
| `com.jio.media.p062tv.p063ui.permission_onboarding.PermissionActivity` | `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` |
| `com.pairip.VMRunner` | `Lcom/pairip/VMRunner;` |
| `com.pairip.StartupLauncher` | `Lcom/pairip/StartupLauncher;` |
| `com.pairip.SignatureCheck` | `Lcom/pairip/SignatureCheck;` |
| `com.pairip.application.Application` | `Lcom/pairip/application/Application;` |
| `com.pairip.licensecheck.LicenseContentProvider` | `Lcom/pairip/licensecheck/LicenseContentProvider;` |
| `com.pairip.licensecheck.LicenseClient` | `Lcom/pairip/licensecheck/LicenseClient;` |
| `com.pairip.licensecheck.LicenseActivity` | `Lcom/pairip/licensecheck/LicenseActivity;` |

## Method Type Reference

| Class | Method | Type | Patch Action |
|-------|--------|------|-------------|
| `Lcom/pairip/SignatureCheck;` | `verifyIntegrity` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `initializeLicenseCheck` | **virtual** | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `connectToLicensingService` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `processResponse` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `startPaywallActivity` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `startErrorDialogActivity` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseClient;` | `handleError` | direct | `return-void` |
| `Lcom/pairip/licensecheck/LicenseActivity;` | `onStart` | **virtual** | `super.onStart(); finish(); return-void` |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `getCheckAppUpadteData` | direct | `return null` |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `checkIsUpdateAvailable` | direct | `return-void` |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `redirectToPlayStore` | direct | `return-void` |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `takeToPlayStore` | direct | `return-void` |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `checkUpdate` | **virtual** | `return-void` |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `checkUpdatefordiag` | **virtual** | `return-void` |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `resumeUpdate` | **virtual** | `return-void` |
| `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | `a` | direct (static) | `return-void` |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isRunningOnEmulator` | **virtual** | `return false` |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `isSupportedDevice` | **virtual** | `return true` |
| `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` | `onCreate` | **virtual** | `super.onCreate(); proceedApplication(); return-void` |
| `Lcom/google/firebase/crashlytics/internal/common/CommonUtils;` | `isRooted` | direct | `return false` |
| `Lcom/jio/jioplay/tv/utils/SecurityUtils;` | `isValidVersionName` | direct | `return true` |
| `Lcom/jio/jioplay/tv/utils/CommonUtils;` | `showXposedFrameworkDetectionDialog` | direct | `return-void` |
| `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;` | `isSslPining` | **virtual** | `return false` |
| `Lokhttp3/CertificatePinner;` | `check$okhttp` | **virtual** | `return-void` |

## Patch Details

### 1. Disable pairip license check (manifest)
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Resource patch
- **Action**: Removes `LicenseContentProvider` from AndroidManifest.xml

### 2. Remove Play Store license check
- **File**: `playstore/RemovePlayStoreLicenseCheckPatch.kt`
- **Type**: Bytecode patch
- **Targets**: 15 methods across 4 classes (pairip DRM, server-driven updates, Google Play Core)

### 3. Remove emulator detection
- **File**: `emulator/RemoveEmulatorDetectionPatch.kt`
- **Type**: Bytecode patch
- **Targets**: 3 methods in `PermissionActivity`

### 4. Remove root detection
- **File**: `root/RemoveRootDetectionPatch.kt`
- **Type**: Bytecode patch
- **Targets**: 3 methods across 3 classes

### 5. Remove certificate pinning
- **File**: `sslpinning/RemoveCertificatePinningPatch.kt`
- **Type**: Bytecode patch
- **Targets**: 2-3 methods (FirebaseConfig toggle + OkHttp3 CertificatePinner)

### 6. Enable cleartext traffic
- **File**: `misc/MiscPatches.kt`
- **Type**: Resource patch
- **Action**: Sets `usesCleartextTraffic=true` in manifest, rewrites `network_security_config.xml`
