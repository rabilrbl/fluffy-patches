# Session 2026-04-08: Latest Uptodown XAPK on x86_64 AVD

## Goal
Re-test JioTV Mobile using the **latest Uptodown XAPK** on the existing x86_64 emulator instead of assuming older antisplit findings still apply.

## Environment
- AVD: `Pixel_4_API30_PlayStore`
- Emulator ABI: `x86_64`
- Emulator command requires:
  ```bash
  ANDROID_SDK_ROOT=$HOME/Android ANDROID_HOME=$HOME/Android $HOME/Android/Sdk/emulator/emulator -avd Pixel_4_API30_PlayStore -no-snapshot-load -no-snapshot-save
  ```
- Latest Uptodown version visible on page at test time: `7.1.7` (`Nov 16, 2025`)
- Local test artifact used: `tmp/jiotv-7-1-7.xapk`

## Critical Result
The **untouched latest Uptodown split APK set launches on the x86_64 AVD without the old native startup crash**.

Installed with:
```bash
adb install-multiple -r com.jio.jioplay.tv.apk config.x86_64.apk config.xxhdpi.apk
adb shell monkey -p com.jio.jioplay.tv -c android.intent.category.LAUNCHER 1
```

Observed:
- Process stays alive (`pidof com.jio.jioplay.tv` returned a PID)
- Logcat shows `Displayed com.jio.jioplay.tv/.HomeActivity`
- No immediate `SIGSEGV` in `libpairipcore.so`
- Foreground later moves to Google Play / Play sign-in UI, but this is **not** the old native crash

## Control Test: Re-signing Alone
A critical control test was run with the **same untouched APKs**, but all three installed splits were re-signed with a shared debug key before install.

Result:
- App immediately crashes on launch
- Logcat shows native crash in:
  - `split_config.x86_64.apk!lib/x86_64/libpairipcore.so`
- This reproduces the classic pairip native failure path

## Patch Test
Then the Morphe-patched base APK was installed with matching re-signed split APKs.

Result:
- Same native crash behavior as the re-sign-only control
- Therefore the immediate blocker is **not proven to be the patch logic itself**
- The stronger conclusion is: **pairip on this build rejects non-original signatures on the x86_64 path**

## Updated Understanding
Older notes focused on "patched APK crashes on emulator" and tended to attribute that to dex/resource modification. The new tests show a more precise and more important distinction:

1. **Original latest Uptodown split APKs**: launch successfully on x86_64 emulator
2. **Re-signed untouched split APKs**: crash in `libpairipcore.so`
3. **Patched + re-signed split APKs**: also crash in `libpairipcore.so`

This means the latest blocker for fluffy-patches-based installation is at least **signature preservation / signature-sensitive native checks**, not just patch content.

## Practical Implication for fluffy-patches
`fluffy-patches` currently produces a modified APK that must be re-signed for installation. On the latest Uptodown JioTV split package, that re-signing step is enough to trigger pairip native failure on x86_64.

So, for this app version, the current Morphe patch workflow does **not** provide a working installable emulator build unless one of these changes:
- preserve the original signing identity (not realistically possible for third-party patching)
- bypass the native signature check in `libpairipcore.so`
- avoid APK modification entirely and use runtime hooking / root-based methods

## Suggested Next Steps
1. Treat the latest Uptodown XAPK as the new baseline, not the old antisplit APK
2. Document that **re-sign-only** is enough to trigger the native crash
3. If pursuing a non-root solution, focus on native signature-check bypass rather than Java-layer patches first
4. If pursuing a practical working solution, test runtime-hooking / installer-spoofing approaches instead of Morphe APK modification
