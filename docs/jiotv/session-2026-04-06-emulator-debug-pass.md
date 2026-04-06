# Session 2026-04-06: Emulator/AVD Debug Pass + Current Hard Blocker

## Goal of this pass

Validate the most promising non-root path with local tooling:

1. inventory the repo state and uncommitted work
2. launch the best available AVD(s)
3. install/run original or patched JioTV APKs if possible
4. capture the current blocker with fresh evidence

## Repo state at start

Branch status:

```bash
cd /home/rabil/Projects/Others/fluffy-patches
git status --short --branch
```

Output:

```text
## dev...origin/dev
?? docs/jiotv/avd-compatibility.md
?? docs/jiotv/session-2026-04-06-analysis.md
?? patches/src/main/kotlin/app/template/patches/jiotv/playstore/RemoveNativePairipPatch.kt
?? scripts/
?? tmp/
```

Notable uncommitted items:
- `docs/jiotv/avd-compatibility.md`
- `docs/jiotv/session-2026-04-06-analysis.md`
- `patches/src/main/kotlin/app/template/patches/jiotv/playstore/RemoveNativePairipPatch.kt`
- `scripts/patch_jiotv_manual.py`
- `tmp/playstore.apk`
- `tmp/playstore_extract/system.raw.img`

## SDK / tool inventory

Command:

```bash
command -v adb
command -v avdmanager
command -v sdkmanager
command -v emulator
find $HOME/Android/Sdk -maxdepth 3 \( -name adb -o -name emulator -o -name avdmanager -o -name sdkmanager \) | sort
```

Result:
- `adb`: `/usr/bin/adb`
- `avdmanager`: `/home/rabil/Android/cmdline-tools/latest/bin/avdmanager`
- `sdkmanager`: `/home/rabil/Android/cmdline-tools/latest/bin/sdkmanager`
- `emulator`: not on `PATH`, but present at `/home/rabil/Android/Sdk/emulator/emulator`
- SDK root in practice: `/home/rabil/Android/Sdk`

Installed system images relevant to this work:
- `system-images;android-34;google_apis;x86_64`
- `system-images;android-34;google_apis;arm64-v8a`
- `system-images;android-34;google_apis_playstore;x86_64`
- `system-images;android-30;google_apis;x86_64`

## Available source APKs / inputs

Located under `~/Documents/Android_APKs`:

```text
/home/rabil/Documents/Android_APKs/com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_3f3f662dc17e44460f9b30fc5079b476_apkmirror.com.apkm
/home/rabil/Documents/Android_APKs/com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_3f3f662dc17e44460f9b30fc5079b476_apkmirror.com_merged.apk
/home/rabil/Documents/Android_APKs/JioTV_v7.1.7(404)_antisplit-aligned-debugSigned.apk
/home/rabil/Documents/Android_APKs/JioTV_v7.1.7(404)_antisplit.apk
```

Quick inventory:

```bash
unzip -l ~/Documents/Android_APKs/JioTV_v7.1.7\(404\)_antisplit.apk | rg 'lib/.*/libpairipcore|assets/mVBwD2didVTgj5k7'
unzip -l ~/Documents/Android_APKs/com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_3f3f662dc17e44460f9b30fc5079b476_apkmirror.com_merged.apk | rg 'lib/.*/libpairipcore|assets/mVBwD2didVTgj5k7'
```

Findings:
- antisplit APK contains only `lib/arm64-v8a/libpairipcore.so`
- merged APK contains `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` pairip libraries
- both contain the encrypted VM asset `assets/mVBwD2didVTgj5k7`

## AVD inventory and launch attempts

### 1) Existing AVD list

```bash
$HOME/Android/Sdk/emulator/emulator -list-avds
avdmanager list avd
```

Result:
- `PlayStoreAVD`
- `PlayStoreARM64`

But `avdmanager` reports:

```text
The following Android Virtual Devices could not be loaded:
    Name: PlayStoreARM64
    Path: /home/rabil/.android/avd/PlayStoreARM64.avd
   Error: Missing system image for Google APIs arm64-v8a PlayStoreARM64.
```

### 2) Why PlayStoreARM64 is unusable on this host

Direct launch attempt:

```bash
$HOME/Android/Sdk/emulator/emulator -avd PlayStoreARM64 -no-window -no-audio -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect
```

Result:

```text
FATAL | Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host. System image must match the host architecture.
```

So even though the ARM64 system image is installed, this host/emulator combination cannot boot an ARM64 AVD.

### 3) PlayStoreAVD x86_64 launch attempts

GUI launch:

```bash
$HOME/Android/Sdk/emulator/emulator -avd PlayStoreAVD -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect -accel on
```

Headless launch:

```bash
$HOME/Android/Sdk/emulator/emulator -avd PlayStoreAVD -no-window -no-audio -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect
```

In both cases the emulator itself crashed before ADB became available.

Observed tail from the headless run:

```text
Started GRPC server at 127.0.0.1:8554, security: Local, auth: +token
Advertising in: /run/user/1000/avd/running/pid_128853.ini
Setting display: 0 configuration to: 1080x2400, dpi: 420x420
setDisplayActiveConfig 0
USER_INFO | Emulator is performing a full startup. This may take upto two minutes, or more.

Process exited with signal SIGSEGV.
```

ADB never saw a device:

```bash
adb devices -l
```

Output:

```text
List of devices attached
```

### 4) Clean sanity-check AVD

Created a fresh test AVD from installed API 30 x86_64 image:

```bash
printf 'no\n' | avdmanager create avd -n TmpApi30X86 -k 'system-images;android-30;google_apis;x86_64' -d pixel_6
```

Attempted launch failed due malformed/relative system-image path in generated config:

```text
WARNING | /var/home/rabil/Android/Sdk/Sdk/system-images/android-30/google_apis/x86_64/ is not a valid directory.
FATAL   | Cannot find AVD system path. Please define ANDROID_SDK_ROOT
```

This indicates the local SDK/AVD setup also has path inconsistency (`Android` vs `Android/Sdk`) on this machine.

## Non-root bypass artifact inventory

Located a candidate native replacement library:

```text
/home/rabil/Documents/Dev_Projects/removepairip/libpairipcorex.so
```

Inspection:

```bash
file ~/Documents/Dev_Projects/removepairip/libpairipcorex.so
sha256sum ~/Documents/Dev_Projects/removepairip/libpairipcorex.so
readelf -h ~/Documents/Dev_Projects/removepairip/libpairipcorex.so | sed -n '1,20p'
readelf -sW ~/Documents/Dev_Projects/removepairip/libpairipcorex.so | rg 'JNI_OnLoad|Java_com_pairip_VMRunner_executeVM'
```

Findings:
- architecture: `ARM aarch64`
- SHA-256: `22a7954092001e7c87f0cacb7e2efb1772adbf598ecf73190e88d76edf6a7d2a`
- exports `JNI_OnLoad`
- does **not** expose `Java_com_pairip_VMRunner_executeVM` as a plain dynamic symbol, so it likely uses `RegisterNatives` or an internal dispatch path

This is the strongest locally available candidate for the Snailsoft-style APKEditor path, but it cannot be verified on the current host because there is no working ARM64 runtime target.

## Codebase correction made during this pass

Updated `patches/src/main/kotlin/app/template/patches/jiotv/playstore/RemovePlayStoreLicenseCheckPatch.kt` to stop neutralizing `StartupLauncher.launch()`.

Reason:
- docs and prior experiments already showed that bypassing `StartupLauncher.launch()` is the wrong direction
- the pairip VM is invoked from `MultiDexApplication.<clinit>()`
- removing that launch path causes earlier init/splash crashes instead of a usable app

## High-confidence blocker (current)

The immediate blocker is now **environmental first, app second**:

1. **ARM64 AVD path is unavailable on this x86_64 host**
   - emulator refuses to run ARM64 AVDs here
2. **Existing x86_64 AVD crashes before ADB attaches**
   - cannot even perform baseline app installs/logcat collection on the current local emulator state
3. Even if x86_64 AVD were stable, prior docs already show pairip v404 is not a trustworthy target there because the native library crashes under x86_64/emulation anyway

So the best current blocker statement is:

> The best remaining non-root experiment is `APKEditor + libpairipcorex.so` on a real ARM64 runtime target, but this machine currently has no working ARM64 Android target and its x86_64 emulator is host-crashing before ADB is available.

## Best next experiment

Run the `libpairipcorex.so` path on **real ARM64 hardware** or on a host that can boot an ARM64 Android target.

Recommended order:

1. Fix/replace the local emulator environment only if needed for other tasks
   - set a consistent `ANDROID_SDK_ROOT`
   - regenerate AVDs with valid `image.sysdir.1`
   - but do **not** expect ARM64 AVD boot on this x86_64 host with current QEMU2 limitation
2. Use a physical ARM64 device (preferred) or a compatible ARM64 host
3. Build/test this exact pipeline:
   - input: merged APKM-derived APK or antisplit APK
   - APKEditor decompile
   - replace `libpairipcore.so` with `libpairipcorex.so`
   - keep manifest/provider adjustments only where necessary
   - avoid `StartupLauncher.launch()` no-op
   - sign/install
   - capture logcat focused on `pairip`, `AndroidRuntime`, `libc`, and app PID

## Minimal command reference captured this pass

```bash
# list AVDs
$HOME/Android/Sdk/emulator/emulator -list-avds
avdmanager list avd

# x86_64 AVD launch attempt
$HOME/Android/Sdk/emulator/emulator -avd PlayStoreAVD -no-window -no-audio -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect

# ARM64 AVD launch attempt
$HOME/Android/Sdk/emulator/emulator -avd PlayStoreARM64 -no-window -no-audio -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect

# APK inventory
find $HOME/Documents/Android_APKs -maxdepth 2 -type f \( -iname '*.apk' -o -iname '*.apkm' \) | sort

# inspect pairip libs
unzip -l "$HOME/Documents/Android_APKs/JioTV_v7.1.7(404)_antisplit.apk" | rg 'lib/.*/libpairipcore|assets/mVBwD2didVTgj5k7'
unzip -l "$HOME/Documents/Android_APKs/com.jio.jioplay.tv_7.1.7-404_4arch_7dpi_3f3f662dc17e44460f9b30fc5079b476_apkmirror.com_merged.apk" | rg 'lib/.*/libpairipcore|assets/mVBwD2didVTgj5k7'

# inspect libpairipcorex
file "$HOME/Documents/Dev_Projects/removepairip/libpairipcorex.so"
readelf -sW "$HOME/Documents/Dev_Projects/removepairip/libpairipcorex.so" | rg 'JNI_OnLoad|Java_com_pairip_VMRunner_executeVM'
```
