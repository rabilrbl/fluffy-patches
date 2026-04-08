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
The route was first blocked by `adb root` assumptions, but **Magisk root did work once shell root was granted**.

### Checks Run
```bash
adb devices -l
adb shell dumpsys package com.jio.jioplay.tv | grep -E 'versionCode|versionName'
adb shell pidof com.jio.jioplay.tv
adb shell /debug_ramdisk/su -c id
adb push ~/Downloads/Android_APKs/frida-server-17.9.1-android-x86_64 /data/local/tmp/frida-server
adb shell chmod 755 /data/local/tmp/frida-server
adb shell '/debug_ramdisk/su 0 -c "nohup /data/local/tmp/frida-server >/data/local/tmp/frida-server.log 2>&1 &"'
adb forward tcp:27042 tcp:27042
frida-ps -H 127.0.0.1:27042
frida -H 127.0.0.1:27042 -p <main-pid> -l scripts/jiotv-371-original-frida.js
```

### Observed
- `adb root` still fails on this production Play Store image, which is expected
- Magisk is present under `/debug_ramdisk/magisk`
- `adb shell /debug_ramdisk/su -c id` succeeded after shell root was granted
- `frida-server` must be started as **root**, not as the shell user
  - working form:
    ```bash
    adb shell '/debug_ramdisk/su 0 -c "nohup /data/local/tmp/frida-server >/data/local/tmp/frida-server.log 2>&1 &"'
    ```
- Frida transport became reachable on `tcp:27042`
- Frida can attach to the **main app pid** of `com.jio.jioplay.tv`
- Important gotcha: `frida-ps -a` also shows a WebView sandbox process that looks associated with the app, but attaching there is misleading and not the target to hook

### Hook Result
The classloader-aware script successfully found and hooked:
- `com.pairip.licensecheck3.LicenseClientV3.handleError`
- `com.pairip.licensecheck3.LicenseClientV3.connectToLicensingService`
- `android.content.ContextWrapper.startActivity`

Immediately after attach, the app crashed with native anti-instrumentation behavior:
- `SIGSEGV SI_TKILL`
- crashing library: `split_config.x86_64.apk!libpairipcore.so`

### Plain-Attach Result
A clean follow-up test was run with **plain Frida attach and no script at all**.

Result:
- the main app process still crashed in `libpairipcore.so`
- so the current trigger is **attach/instrumentation presence itself**, not just the Java hooks in `jiotv-371-original-frida.js`

This sharpens the conclusion:
**pairip on the 371 split x86_64 path detects Frida attach at a native level and kills the process even before hook content matters.**

### Native Clues From `libpairipcore.so`
The x86_64 `libpairipcore.so` from `config.x86_64.apk` was extracted and inspected.

Useful findings:
- dynamic imports include:
  - `dl_iterate_phdr`
  - `dlopen`
  - `dlsym`
  - `syscall`
  - `getpid`
  - `stat`
  - `strcmp`
- string surface is intentionally sparse, but there is an explicit `syscall` import and multiple direct call sites
- a notable block around `0x3cac9` uses raw syscall numbers consistent with anti-debug flow on x86_64, including:
  - `101` (`ptrace`)
  - `61` (`wait4`)
  - `60` (`exit`)
  - `110` (`getppid`)
- another block around `0x32572` uses raw syscall numbers including:
  - `157` (`arch_prctl`)
  - `56` (`clone`)
  - followed by more wait / control syscalls
- the crash tombstone also showed an open file descriptor to:
  - `/proc/<pid>/maps`
  - which is consistent with native module / memory-map inspection during anti-instrumentation checks
- a control test on a benign rooted app (`Magisk`) showed that plain Frida attach leaves obvious map entries such as:
  - `/memfd:frida-agent-64.so (deleted)`
  - so if pairip scans `/proc/self/maps`, it has a very easy signature to match

### Low-Effort Stealth Frida Experiments
A few quick signature-reduction tests were run before attempting a full custom Frida build.

What was tested:
- alternate attach modes such as `--realm emulated`
- alternate script runtime such as `--runtime qjs`
- patched copy of the x86_64 `frida-server` binary with the obvious string `frida-agent-64.so` replaced by a same-length placeholder
- patched throwaway copy of the host Python Frida extension (`_frida.abi3.so`) with the same string replacement

Result:
- none of these low-effort changes removed the injected memfd name from `/proc/<pid>/maps`
- the process still showed:
  - `/memfd:frida-agent-64.so (deleted)`

Meaning:
- the recognizable agent name is not eliminated by shallow string patching of just the visible host/server binaries
- a proper stealth Frida route likely needs a deeper patch of the actual injected agent blob or a custom Frida build

Practical takeaway:
- this library likely has its own low-level anti-debug / anti-instrumentation path
- plain Java-layer Frida hooks are too late and too visible
- shallow Frida string patching is not enough
- the next serious bypass attempt should focus on either:
  - a deeper custom / stealth Frida build, or
  - neutralizing the native anti-debug logic in `libpairipcore.so`

## Practical Meaning
For the **current original signed split install**, runtime-hooking remains the correct direction, and this rooted AVD is usable for experiments. But plain Frida attach is not sufficient by itself because pairip reacts with a native crash once instrumentation is present.

## Resume Paths
### Path A: stealthier Frida / anti-anti-instrumentation pass
Possible next work:
1. delay hook installation further
2. reduce obvious Frida surface area
3. investigate native hook points around `libpairipcore.so`
4. trace whether the crash happens on attach alone or only after Java hooks are installed

### Path B: alternate runtime technique
Use another runtime method that avoids the current detection path, while keeping the original signed splits intact.

## Important Reminder
Do not fall back into the older 404 merged / antisplit assumptions when resuming this line of work. This route is specifically for the **371 split baseline**.
