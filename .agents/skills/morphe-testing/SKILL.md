---
name: morphe-testing
description: Build the patch bundle (.mpp), apply it to a target APK with morphe-cli, install and verify on a device/emulator via ADB. Use before committing patch changes, when a patched APK crashes or misbehaves, or when claiming support for an app version.
license: MIT
compatibility: Requires JDK 21, Android SDK (ANDROID_HOME), adb, and morphe-cli
metadata:
  audience: developers
  workflow: testing
---

There are no automated tests — the build/apply/verify loop below IS the test suite, and
AGENTS.md requires it before committing any patch change. The cycle:
write patch → build → apply → install → verify → document.

## 1. Build the patch package

```bash
ANDROID_HOME="$HOME/Android/Sdk" \
GITHUB_ACTOR="$(gh api user --jq '.login')" \
GITHUB_TOKEN="$(gh auth token)" \
./gradlew :patches:buildAndroid

ls patches/build/libs/*.mpp
```

`GITHUB_ACTOR`/`GITHUB_TOKEN` are needed for Morphe's private Maven registry. Build
failures here are usually invalid Dalvik in an injected instruction string.

## 2. Apply to the target APK with morphe-cli

```bash
java -jar morphe-cli*-all.jar patch \
    --patches patches/build/libs/patches-<version>.mpp \
    -f <target-app>.apk \
    --out patched.apk
```

- Run `java -jar morphe-cli*.jar patch --help` for the current flag set.
- The target APK version must match an `AppTarget` version in `Constants.kt`, or the
  patches won't be offered.
- For split distributions (XAPK/APKM), apply to the **antisplit-merged APK** — the same
  artifact you'll install.
- Morphe Manager on an Android device is the GUI alternative.

## 3. Install and verify via ADB

```bash
adb install -r patched.apk

# Clear stale state before testing gates — cached prefs mask results
adb shell pm clear <package.name>

# Launch
adb shell monkey -p <package.name> 1

# Watch logs for the app (patcher errors, crashes)
adb logcat --pid=$(adb shell pidof -s <package.name>)
```

Verify each patch's intended behavior on the device:

- Premium gate → gated UI/modules unlock after a fresh start
- Ads → no ad initialization at startup or in relevant screens
- Dialog/flow bypass (e.g. coin redemption) → the flow completes without the skipped step
- App doesn't crash on launch, navigation, and background/foreground transitions

## When it fails

- **Crash on launch** — read logcat first; look for patcher exceptions,
  `ClassNotFoundException`, verification errors. Don't guess fixes; find the root cause.
- **Patch changes nothing at runtime** — classic prepend-only injection; see
  morphe-patching gotchas (clear the body before forcing a return).
- **Target class/method missing** — re-analyze the APK (`android-apk-analysis` skill) and
  update `docs/<appname>/`; the app version may differ from what the patch expects.
- **Patcher doesn't offer the patches** — APK version doesn't match `AppTarget` versions
  in `Constants.kt`, or the `.mpp` is stale relative to your source edits.

## Gotchas

- **Client-side gates only.** Patches here change local checks; server-side entitlement
  validation failing is expected and out of scope (see BlockerX docs). Verify what the
  patch actually controls.
- **No device available? Don't skip silently.** State it in `docs/<appname>/README.md`
  and the PR (precedent: BlockerX "No device was available during repository verification").
- **Test after every patch, not in batches** — when adding several patches, verify each
  change on the device before moving to the next; otherwise failures are unattributable.
- **Document the outcome** — success or failure, record it in `docs/<appname>/` so the
  next version bump starts from reality.
