# Pairip Bypass — Session 2026-04-30 v2

## Summary

Attempted Frida-based runtime dump of R8-encrypted string values, patched `kEpRxMC` with hardcoded values, built v14 (Platinmods + hardcodes), and discovered multiple additional obfuscated string classes causing subsequent crashes.

## What Worked

### 1. Frida dump of kEpRxMC (48 string values)
- Attached Frida to original running app (PID 21551)
- Dump script: `/tmp/pairip-dump/dump_strings.js`
- Successfully extracted all 48 static String field values from `kEpRxMC` class
- Key value: `"aKPJrxNCR": "kotlinx.coroutines.scheduler.default.name"` (the one causing NPE)
- Output saved to: `/tmp/pairip-dump/kEpRxMC_dump.json`

### 2. kEpRxMC.smali patching
- Added `<clinit>` with `const-string` + `sput-object` for all 48 fields
- File: `/tmp/pairip-dump/jiotv-smali-clean/smali_classes2/org/apache/commons/net/ftp/Rso/kEpRxMC.smali`
- This **eliminated the kEpRxMC NPE crash entirely**

### 3. Platinmods manifest redirect
- Changed `android:name` from `com.pairip.application.Application` to `com.jio.jioplay.tv.JioTVApplication`
- Removed `StartupLauncher.launch()` call from `CoreComponentFactory.<clinit>`
- This skips pairip's Application class and VM startup entirely

### 4. v14 built and installed successfully
- Build pipeline: apktool → zipalign → apksigner (all working)
- App installed and launched without package-level errors

## What Didn't Work

### v14 crash: ExceptionInInitializerError in FirebaseInitProvider
- **New crash**: `FirebaseInitProvider` → `FirebaseCrashlytics` → `AnalyticsConnectorImpl.zzf` → `ExceptionInInitializerError`
- **Root cause**: `kEpRxMC` was only ONE of multiple R8-encrypted string classes
- Firebase/MLKit/Analytics classes have their own obfuscated string holders that also depend on pairip VM decryption
- The app has **thousands** of short-named obfuscated classes (aa, b0, tzk, etc.) across 10 dex files
- These classes also have null string fields without the pairip VM running

### Frida spawn mode failure
- `frida -U -f com.jio.jioplay.tv -l script.js --no-pause` fails: `--no-pause` not recognized in Frida 17.9.1
- Attach mode works briefly but pairip's anti-Frida kills the process after ~2-3 seconds
- The comprehensive dump script (`/tmp/pairip-dump/dump_all_strings.js`) was written but not yet successfully executed

## Key Finding: Multiple String Encryption Classes

The app uses R8 string encryption with **multiple** obfuscated holder classes:

| Class | Package | Status |
|-------|---------|--------|
| `kEpRxMC` | `org.apache.commons.net.ftp.Rso` | ✅ Dumped and hardcoded (48 fields) |
| `tzk` / `tzg` | Google MLKit obfuscated | ❌ Not dumped — causes ExceptionInInitializerError |
| `zzd` / `zzf` | Firebase Analytics | ❌ Not dumped — cascade crash |
| Others | ??? | ❌ Unknown |

The v14 crash trace:
```
ExceptionInInitializerError
  at com.google.firebase.analytics.connector.internal.zzd.zzf
  at com.google.firebase.crashlytics.AnalyticsDeferredProxy.subscribeToAnalyticsEvents
  at FirebaseInitProvider.onCreate
  at ActivityThread.installProvider (during handleBindApplication)
```

## Two Paths Forward

### Path A: Disable non-essential ContentProviders (quick win)
- Remove or disable `FirebaseInitProvider`, `MlKitInitProvider`, and other init providers from manifest
- These are crash reporting/analytics — not essential for JioTV playback
- If all remaining obfuscated string classes are only used by Firebase/MLKit, disabling these providers eliminates the entire crash chain

### Path B: Comprehensive Frida dump (thorough)
- Fix Frida spawn command (remove `--no-pause`, use Frida 17 syntax)
- Run `dump_all_strings.js` to enumerate ALL obfuscated string classes
- Hardcode each one's values in smali
- More complete but riskier (Frida anti-detection)

## Recommended Order
1. Try Path A first (disable Firebase/MLKit providers in manifest)
2. If app still crashes, do Path B
3. Iterate until app launches clean

## Files Modified for v14
- `jiotv-smali-clean/AndroidManifest.xml` — `android:name="com.jio.jioplay.tv.JioTVApplication"`, `extractNativeLibs=true`
- `jiotv-smali-clean/smali_classes2/org/apache/commons/net/ftp/Rso/kEpRxMC.smali` — hardcoded `<clinit>` with 48 values
- `jiotv-smali-clean/smali_classes3/androidx/core/app/CoreComponentFactory.smali` — removed `StartupLauncher.launch()` call
- `jiotv-smali-clean/smali_classes3/com/pairip/StartupLauncher.smali` — original (not no-op'd, since launch is removed from caller)
- `config.x86_64` split — contains fake `libpairipcore.so` (5.6KB stub)

## Build Pipeline
```bash
cd /tmp/pairip-dump
apktool b jiotv-smali-clean -o jiotv-patched-vN-unsigned.apk
/home/rabil/Android/build-tools/34.0.0/zipalign -f 4 jiotv-patched-vN-unsigned.apk jiotv-patched-vN-aligned.apk
/home/rabil/Android/build-tools/34.0.0/zipalign -f 4 config.x86_64-vN-unsigned.apk config.x86_64-vN-aligned.apk
/home/rabil/Android/build-tools/34.0.0/apksigner sign --ks debug.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android jiotv-patched-vN-aligned.apk
# Same for config split
adb install-multiple jiotv-patched-vN-aligned.apk config.x86_64-vN-aligned.apk [other splits...]
```

## Current v14 Status
- Installed on AVD but crashes on launch via FirebaseInitProvider → ExceptionInInitializerError
- kEpRxMC hardcodes working (no NPE from that class)
- Firebase/MLKit content providers need to be disabled or their string classes need hardcoding

## Frida Scripts
- `/tmp/pairip-dump/dump_strings.js` — kEpRxMC dumper (✅ worked)
- `/tmp/pairip-dump/dump_all_strings.js` — comprehensive dumper for all obfuscated string classes (❌ not yet run successfully)