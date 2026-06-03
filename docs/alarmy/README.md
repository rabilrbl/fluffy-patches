# Alarmy Patches

## App Info

- **Package:** `droom.sleepIfUCan`
- **Name:** Alarmy
- **Version:** 26.23.0
- **File Type:** XAPK

## Architecture

Alarmy uses Google Pairip DRM to verify APK signature and Google Play license. The app also uses a local `PremiumState` data class (`pi/b`) to track subscription status. Both mechanisms must be bypassed for the app to work when patched and sideloaded.

### Pairip DRM

| Smali Class | Purpose |
|-------------|---------|
| `com.pairip.application.Application` | Application subclass that extends `AlarmyApp`; its `attachBaseContext()` calls `VMRunner.setContext()` and `SignatureCheck.verifyIntegrity()` |
| `com.pairip/SignatureCheck` | Verifies APK signature against hardcoded expected signatures; throws `SignatureTamperedException` if mismatch |
| `com.pairip/licensecheck/LicenseContentProvider` | ContentProvider that runs before app launch; its `onCreate()` instantiates `LicenseClient` and calls `initializeLicenseCheck()` |
| `com.pairip/licensecheck/LicenseClient` | Core license check logic; binds to Google Play licensing service and processes the response |
| `com.pairip/licensecheck/LicenseActivity` | Activity shown on license failure; shows "Get this app from Play" paywall or error dialog |
| `com.pairip/licensecheck/LicenseResponseHelper` | Validates JWS (JSON Web Signature) response from Google Play licensing service |

### Premium State

| Smali | Kotlin Metadata | Purpose |
|-------|-----------------|---------|
| `pi/b` | `PremiumState` | Core premium state data class |
| Patch | Target | What it does |
|-------|--------|--------------|
| Bypass pairip license verification | `AndroidManifest.xml` | Replaces Application class with `AlarmyApp`, removes `LicenseContentProvider` and `LicenseActivity`, sets `extractNativeLibs=true` |
| Disable pairip VM load | `VMRunner.<clinit>()` | Returns immediately, preventing `libpairipcore.so` from loading — this native library's `JNI_OnLoad` runs background integrity checks that detect APK modifications |
| Disable pairip content provider | `LicenseContentProvider.onCreate()` | Returns immediately without creating a `LicenseClient` |
| Disable pairip signature check | `SignatureCheck.verifyIntegrity()` | Returns immediately; prevents APK signature mismatch detection |
| Disable pairip license check | `LicenseClient.initializeLicenseCheck()` | Returns immediately; prevents Google Play license verification |
| Disable pairip paywall | `LicenseClient.startPaywallActivity()` | Returns immediately; prevents the "Get this app from Play" redirect |
| Disable pairip error dialog | `LicenseClient.startErrorDialogActivity()` and `handleError()` | Returns immediately; prevents error dialogs |
| Disable pairip license activity | `LicenseActivity.onStart()` | Finishes the activity immediately if somehow launched |
| Disable pairip response validation | `LicenseResponseHelper.validateResponse()` | Returns immediately; treats any license response as valid |
| Smali Name | Kotlin Name | Logic |
|------------|-------------|-------|
| `r()` | `isPremium()` | Returns true if any premium type is active (lifetime, playpass, google, manual, delightroom) |
| `s()` | `isRemoveAdPremium()` | Returns true if premium type is REMOVE_AD_SUBSCRIPTION and not expired |
| `o()` | `isLifetimePremium()` | Returns true if premium type is LIFETIME |
| `q()` | `isPlayPassPremium()` | Returns true if premium type is PLAYPASS |
| `n()` | `isGooglePremium()` | Returns true if premium type is GOOGLE_SUBSCRIPTION and not expired |
| `p()` | `isManualPremium()` | Returns true if premium type is MANUAL and not expired |
| `k()` | `isDelightroomSubscriptionPremium()` | Returns true if premium type is DELIGHTROOM_SUBSCRIPTION and not expired |

## Patches

### Pairip Bypass (apply all)

These patches target the Google Pairip DRM layer. All must be applied for the app to launch without the "Get this app from Play" paywall.

| Patch | Target | What it does |
|-------|--------|--------------|
| Bypass pairip license verification | `AndroidManifest.xml` | Replaces Application class with `AlarmyApp`, removes `LicenseContentProvider` and `LicenseActivity`, sets `extractNativeLibs=true` |
| Disable pairip content provider | `LicenseContentProvider.onCreate()` | Returns immediately without creating a `LicenseClient` |
| Disable pairip signature check | `SignatureCheck.verifyIntegrity()` | Returns immediately; prevents APK signature mismatch detection |
| Disable pairip license check | `LicenseClient.initializeLicenseCheck()` | Returns immediately; prevents Google Play license verification |
| Disable pairip paywall | `LicenseClient.startPaywallActivity()` | Returns immediately; prevents the "Get this app from Play" redirect |
| Disable pairip error dialog | `LicenseClient.startErrorDialogActivity()` and `handleError()` | Returns immediately; prevents error dialogs |
| Disable pairip license activity | `LicenseActivity.onStart()` | Finishes the activity immediately if somehow launched |
| Disable pairip response validation | `LicenseResponseHelper.validateResponse()` | Returns immediately; treats any license response as valid |

### Premium Patches

| Patch | Target | What it does |
|-------|--------|--------------|
| Unlock Pro subscription | `PremiumState.isPremium()` | Forces the method to always return `true`, unlocking all premium features |
| Remove ads | `PremiumState.isRemoveAdPremium()` | Forces the method to always return `true`, disabling ads |

## Notes

- The app uses Google Play Billing (`com.android.vending.BILLING` permission) for in-app purchases.
- Subscription state is cached locally in `PremiumStatePreferences` (`zg/h`).
- Patching the local state class is sufficient to unlock UI and features; server-side validation only affects receipt validation for new purchases.
- Pairip's `LicenseClient` performs both a local installer check (`performLocalInstallerCheck()`) and a remote Google Play license check. The bypass patches neutralize both paths.
- The native library `libpairipcore.so` (in config APKs) is loaded by `VMRunner` but only executes VM bytecode when explicitly invoked via `VMRunner.invoke()`. It is not triggered during normal app operation after the patches are applied.
