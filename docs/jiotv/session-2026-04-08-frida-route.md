# Session 2026-04-08: Frida / Runtime-Hook Route Against Original Split Install

## Goal
Start a runtime-hook path against the **working original signed split install** instead of the re-signed APK route.

## Baseline Confirmed
- Device: `emulator-5554`
- Build: Play Store AVD, Android 11, `x86_64`
- Installed package: `com.jio.jioplay.tv`
- Installed version: `versionCode 371`, `versionName 7.1.7`
- Split set on device:
  - `base.apk`
  - `split_config.x86_64.apk`
  - `split_config.xxhdpi.apk`
- The app process was alive during checks (`pidof com.jio.jioplay.tv` returned a PID)

## What Was Added To The Repo
- `scripts/jiotv-371-original-frida.js`
  - Frida hooks for `com.pairip.licensecheck3.LicenseClientV3.handleError`
  - tracing hook for `connectToLicensingService`
  - generic `ContextWrapper.startActivity` tracing to catch Play redirects
- `scripts/jiotv-371-original-frida-route.sh`
  - validates the installed package and version
  - forwards `tcp:27042`
  - checks Frida reachability
  - launches the app if needed
  - attaches Frida to the live process when possible

## Result On This Specific AVD
The route is **prepared but blocked** on the current emulator build.

### Checks Run
```bash
adb devices -l
adb shell dumpsys package com.jio.jioplay.tv | grep -E 'versionCode|versionName'
adb shell pidof com.jio.jioplay.tv
adb root
adb forward tcp:27042 tcp:27042
frida-ps -H 127.0.0.1:27042
```

### Observed
- `adb root` failed with:
  - `adbd cannot run as root in production builds`
- `frida-ps -H 127.0.0.1:27042` could not reach a Frida server
- Therefore classic Frida attach is not available on this Play Store AVD as-is

## Practical Meaning
For the **current original signed split install**, runtime-hooking is still the right direction, but this exact emulator image is the wrong vehicle for standard Frida-server-based work.

## Resume Paths
### Path A: rooted target
Use a rooted AVD or rooted physical device, then:
1. start `frida-server`
2. forward `tcp:27042`
3. run `scripts/jiotv-371-original-frida-route.sh`

### Path B: alternate runtime technique
If staying on this Play Store AVD, use a runtime method that does not depend on root-hosted Frida server.
Potential directions:
- rebuild on a rootable emulator image
- use a rooted physical device
- evaluate other non-re-sign runtime instrumentation options separately from the Morphe patch flow

## Important Reminder
Do not fall back into the older 404 merged / antisplit assumptions when resuming this line of work. This route is specifically for the **371 split baseline**.
