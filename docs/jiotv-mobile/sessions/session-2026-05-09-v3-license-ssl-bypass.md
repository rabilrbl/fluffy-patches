# Session 2026-05-09 (v3): License Check + SSL Pinning Bypass — App Functional!

## Summary

JioTV app now launches past pairip license verification wall and is functional for main content. SSL certificate pinning bypassed for API and media endpoints. The pairip block wall only appears on certain pages within the app (not the main screen).

## New Patches Applied

### LicenseClientV3 Bypass (pairip license check)
| Class | Dex | Patch |
|-------|-----|-------|
| `LicenseClientV3` | classes5 | `initializeLicenseCheck()` → `return-void` (skip all license checks) |
| `LicenseClientV3` | classes5 | `processResponse()` → immediately set OK state + return (never show paywall) |
| `LicenseClientV3` | classes5 | `showPaywall()` → `return-void` |
| `LicenseClientV3` | classes5 | `showErrorDialog()` → `return-void` |

### SSL Certificate Pinning Bypass
| Class | Dex | Patch |
|-------|-----|-------|
| `APIManager` | classes6 | `isSslPining()` check → `goto :cond_96` (always skip pinning block) |
| `JioPlayer.k.c` | classes2 | `media.jio.com` URL check → `goto :cond_1b2` (always skip cert pin verification) |

## Technical Details

### LicenseClientV3 Flow
- `onActivityCreate(Activity)` → creates LicenseClientV3, calls `initializeLicenseCheck()`
- `initializeLicenseCheck()` checks `licenseCheckState`:
  - `CHECK_REQUIRED` (ordinal 0) → calls `connectToLicensingService()` → contacts Play Store
  - `OK` (ordinal 1) → calls `ResponseValidator.validateResponse(responsePayload, packageName)` → but responsePayload is null!
- Patch: `initializeLicenseCheck()` → `return-void` prevents both paths
- Also patched `processResponse()`, `showPaywall()`, `showErrorDialog()` as defense-in-depth

### SSL Pinning Details
- **APIManager**: OkHttp CertificatePinner for `tv.media.jio.com` with 2 SHA-256 pins, gated by `FirebaseConfig.isSslPining()`. Bypassed by jumping past the pinning block.
- **JioPlayer (k.c)**: Manual certificate pin verification for `media.jio.com` using `X509TrustManagerExtensions.checkServerTrusted()` + hardcoded SHA-256 pins. Bypassed by skipping the entire cert check block.

### Build Info
- APK: `jiotv-v12-aligned.apk`
- Modified dex: classes.dex, classes2.dex, classes5.dex, classes6.dex
- All other dex files from v11b build unchanged

## Known Issues

1. **Content playback crash**: Tapping on content to play crashes with `Resources$NotFoundException: Drawable exo_styled_controls_speed`. This is because apktool rebuild corrupts resource references for split-APK drawables (44 ExoPlayer icons are null in resources.arsc). The main screen works fine.
2. **JSON parsing errors**: `isNewPdp` (null int) and `Banners` (null JSONArray) — from API responses with null fields. Non-fatal, caught by try-catch.
3. **ClassLoaderContext mismatch**: Expected 14 dex files, found 12. Pre-existing issue from original APK — classes13/classes14 referenced but not present.
4. **`httpsnull` URLs**: Some pairip-encrypted strings in classes4/classes8 still null, potentially constructing invalid URLs for OkHttp callbacks.
5. **Resources.arsc corruption**: apktool rebuild nulls out 44 drawable resource references that point to split-APK resources. Using original resources.arsc causes ClassNotFoundException because original resources.arsc references resources from split APKs that don't exist in the merged APK.

## Build Status

- **v12** (jiotv-v12-aligned.apk): Working — app launches, shows content, license bypass works. Content playback crashes due to missing ExoPlayer drawables.
- **v13** (original resources.arsc): Crashes immediately — ClassNotFoundException for pairip Application class
- **v14** (apktool + placeholder drawables): Crashes immediately — ClassNotFoundException for pairip Application class (apktool rebuild issue)

## Root Cause: Split APK Resources

The JioTV app is distributed as a split/bundle APK. The 44 ExoPlayer drawable resources (`exo_styled_controls_*`, `exo_legacy_controls_*`, `exo_notification_*`) are in configuration split APKs. When merged into a single APK:
- Original resources.arsc has valid references (`d=0x7f08020d`) but the actual drawable files are in the splits
- apktool rebuild nulls these references (`d=0x00000000`) causing Resources$NotFoundException when loading drawables
- Adding placeholder XML drawables via apktool works but corrupts class loading (ClassNotFoundException)

Permanent fix options:
1. Install as proper split APK bundle (requires split APK files)
2. Modify `JioPlayerView` to catch `Resources$NotFoundException` and use fallback drawables
3. Find and merge all split APK resources before apktool rebuild

## Files Modified

- `/tmp/dex-patch/work/smali_classes5/com/pairip/licensecheck3/LicenseClientV3.smali` — License check bypass
- `/tmp/dex-patch/work/smali_classes6/com/jio/jioplay/tv/connection/APIManager.smali` — SSL pinning bypass
- `/tmp/dex-patch/work/smali_classes2/com/jio/jioplayer/k/c.smali` — JioPlayer cert pin bypass