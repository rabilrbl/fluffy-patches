# Debugging Journey

## Iteration 1: Wrong Smali Names

**Error**: `Collection contains no element matching the predicate`

**Cause**: Used JADX deobfuscated names (`Lcom/jio/jioplay/tv/p037tv/utils/CommonUtils;`) instead of real smali names (`Lcom/jio/jioplay/tv/utils/CommonUtils;`). JADX strips obfuscation tokens (`p037`, `p062`, `p063`) from package names.

**Fix**: Verified all class names via `jadx_get_smali_of_class` before writing patches.

## Iteration 2: Wrong Method Types (Direct vs Virtual)

**Error**: Same predicate error on different methods after fixing class names.

**Cause**: Lifecycle overrides (`onCreate`, `onStart`, `attachBaseContext`) and public instance methods (`initializeLicenseCheck`, `isSslPining`) are virtual methods, not direct methods.

**Fix**: Checked smali section markers (`# direct methods` vs `# virtual methods`) for each method.

## Iteration 3: Splash Screen Crash

**Error**: App crashes immediately on splash screen after patching.

**Cause**: Neutralized `VMRunner.<clinit>`, `setContext`, and `StartupLauncher.launch`. The app's actual Java code is encrypted in `assets/` and executed by the pairip native VM. Without the VM, the app has no code to run.

**Fix**: Only patch signature check and license-related methods. Let the VM execute normally.

### Startup Flow (Discovered)
```
com.pairip.application.Application.attachBaseContext()
  → VMRunner.setContext(context)
  → SignatureCheck.verifyIntegrity(context)
  → super.attachBaseContext() → MultiDex.install()
→ StartupLauncher.launch()
  → VMRunner.invoke("mVBwD2didVTgj5k7", null)
    → VmDecryptor.decrypt() → executeVM() → libpairipcore.so
→ JioTVApplication.onCreate() ← actual app code
```

## Iteration 4: Play Store Redirect Returns (Update Dialog)

**Error**: "Get this app from Play" dialog appears after splash on real device.

**Cause**: Google Play Core in-app update (`AppUpdateHelper.checkUpdate()`) was not patched. This is called from `HomeActivity.onCreate()` and `HomeActivity.onResume()`.

**Fix**: Added patches for `AppUpdateHelper.checkUpdate()`, `checkUpdatefordiag()`, and static `a()` method.

## Iteration 5: Play Store Redirect Still Appears

**Error**: "Get this app from Play" dialog still appears despite patching `AppUpdateHelper`.

**Root Cause Analysis**:
1. `AppUpdateHelper` class exists in **multiple dex files** (3, 4, 9) in the patched APK but only in classes8.dex in the original
2. Morphe-cli redistributes classes across dex files during patching
3. The patch only targets ONE copy of the class
4. `HomeActivity` may reference a different, unpatched copy

**Additional Discovery**: Even with `checkIsUpdateAvailable()` no-oped, `CommonUtils.getCheckAppUpadteData()` could return pre-cached data from a previous session, triggering the mandatory update dialog in `HomeActivity.onCreate()`.

**Fix**:
1. Patch `CommonUtils.getCheckAppUpadteData()` to always return `null` — this prevents ALL update dialog branches in `HomeActivity.onCreate()` since the first check `if (data != null)` will be false
2. Also patch `AppUpdateHelper.resumeUpdate()` to prevent `HomeActivity.onResume()` from resuming pending updates

### Three Distinct Play Store Redirect Mechanisms (Discovered)

| Mechanism | Entry Point | UI | Bypass |
|-----------|-------------|-----|--------|
| pairip paywall | `LicenseContentProvider.onCreate()` | "Get this app from Play" with sad face | Remove provider + neutralize LicenseClient |
| Google Play Core | `HomeActivity.onCreate()/onResume()` | Standard Play Store update dialog | Patch AppUpdateHelper methods |
| Server-driven update | `PermissionActivity.onCreate()` → API | Custom JioDialog with Update/Exit | `getCheckAppUpadteData()` returns null |

## Iteration 6: Play Store Sideloading Dialog Persists

**Error**: "Get this app from Play" dialog still appears with "Get App" button that opens Play Store.

**Dialog Description**: Standard Google Play dialog with official logo, "Get App" blue button. Dismissing it returns to Play Home. Re-opening app shows same dialog. App doesn't work.

**Root Cause**: The morphe-patcher **deduplicates classes by type** — only ONE copy is stored and patched. `AppUpdateHelper` exists in multiple dex files (3, 4, 9) after morphe-cli redistributes classes. The patch only hits one copy. `HomeActivity` in another dex file calls the unpatched copy.

**Additional finding**: `AppUpdateManagerFactory.create()` is in the Play Core library itself, not in JioTV's dex files. Our patch can't find it because it's in a separate library dex file.

**Attempted fixes that didn't work**:
- `AppUpdateHelper.checkUpdate()` → no-op (only patches one dex copy)
- `AppUpdateHelper.<init>` → no-op (only patches one dex copy)
- `AppUpdateManagerFactory.create()` → return null (class not in JioTV dex files)
- `CommonUtils.getCheckAppUpadteData()` → return null (should work but dialog still appears)

**Next approach**: Patch `HomeActivity.onCreate()` directly to skip the entire update block (lines 18669-18795 in smali). This is the only way to guarantee the update code never runs, regardless of dex file duplication.

### HomeActivity.onCreate() Update Block (Lines 18669-18795)

```smali
# Line 18669: getCheckAppUpadteData()
invoke-static {}, Lcom/jio/jioplay/tv/utils/CommonUtils;->getCheckAppUpadteData()Lcom/jio/jioplay/tv/data/network/response/CheckAppUpadteData;
move-result-object v0
if-eqz v0, :cond_555
# ... mandatory dialog (JioDialog) ...
# ... non-mandatory: AppUpdateHelper.checkUpdate() ...
:cond_555
sget-boolean v0, Lcom/jio/jioplay/tv/data/AppDataManager;->inu:Z
if-eqz v0, :cond_561
# ... AppUpdateHelper.checkUpdate() ...
:cond_561
:goto_561  # ← continuation point
```

## Iteration 7: Injecting Update Data Clear at HomeActivity.onCreate Start

**Approach**: Since `CommonUtils` exists in 8 dex files and `AppUpdateHelper` in 3, patching individual method copies won't work. Instead, inject code at the **start** of `HomeActivity.onCreate()` to call `setCheckAppUpadteData(null)`, clearing the cached data before the update check runs. Also no-op `HomeActivity.onResume()` to prevent `resumeUpdate()` from running.

**Implementation**:
- `HomeActivity.onCreate()` — Prepend: `const/4 v0, 0x0` + `invoke-static {v0}, CommonUtils.setCheckAppUpadteData(null)`
- `HomeActivity.onResume()` — Replace with: `super.onResume()` + `return-void`

**Rationale**: `HomeActivity` is defined in only ONE dex file, so patching it directly is guaranteed to work. Clearing the cached data at the start of `onCreate()` ensures the update check sees `null` regardless of which `CommonUtils` copy is called.

**Status**: Dialog still appears — see iteration 8.

## Iteration 8: AppDataManager.inu Bypass + Constructor Patch Fix

**Error**: Play Store "Get this app" dialog still appears despite clearing cached update data.

**Root Cause**: `HomeActivity.onCreate()` has **two independent paths** to `AppUpdateHelper.checkUpdate()`:

```smali
# Path A: server-driven update data (lines 18668-18778)
invoke-static {}, CommonUtils->getCheckAppUpadteData()
move-result-object v0
if-eqz v0, :cond_555          # ← iteration 7 handles this (data cleared to null)
  # ... mandatory dialog or checkUpdate() ...

# Path B: inu flag (lines 18781-18791)
:cond_555
sget-boolean v0, AppDataManager->inu:Z
if-eqz v0, :cond_561          # ← NOT handled by iteration 7!
  new AppUpdateHelper(this).checkUpdate()   # ← TRIGGERS PLAY STORE DIALOG

:cond_561  # continuation
```

Iteration 7's `setCheckAppUpadteData(null)` clears data → Path A skips to `:cond_555`. But then Path B checks `AppDataManager.inu` (in-app update flag). When `inu == true`, it creates `new AppUpdateHelper` and calls `checkUpdate()` regardless.

Since `AppUpdateHelper` exists in multiple dex files and only one copy is patched, the runtime-loaded copy runs **unpatched** `checkUpdate()` → `getAppUpdateInfo()` → `startUpdateFlowForResult()` → Play Store dialog.

**Additional issue**: The `AppUpdateHelper.<init>` → `return-void` patch was causing a Dalvik verifier violation (returns without calling `super()`/`Object.<init>()`). This likely made the patched dex copy of `AppUpdateHelper` fail verification, forcing the classloader to use an unpatched copy — effectively disabling ALL `AppUpdateHelper` method patches.

**Fix**:
1. Add `sput-boolean v0, AppDataManager->inu:Z` (v0=0) to the `HomeActivity.onCreate()` injection, clearing the `inu` flag before the check
2. Remove the broken `AppUpdateHelper.<init>` → `return-void` patch

**HomeActivity.onCreate() injection now**:
```smali
const/4 v0, 0x0
invoke-static {v0}, CommonUtils->setCheckAppUpadteData(null)V   # clear update data
const/4 v0, 0x0
sput-boolean v0, AppDataManager->inu:Z                           # clear inu flag
```

**Status**: Dialog still appears — see iteration 9.

## Iteration 9: Nuclear Play Core Library Block

**Error**: Play Store dialog STILL appears despite all app-level patches being verified correct in the dex.

**Verification performed**: Extracted and inspected every patched dex file:
- `HomeActivity.onCreate()` injection (setCheckAppUpadteData(null) + inu=false) — **verified at offset 0001-0005** ✓
- `HomeActivity.onResume()` (super.onResume + return-void) — **verified at offset 0000-0003** ✓
- `AppUpdateHelper.checkUpdate()` (return-void) — **verified at offset 0000** in classes9.dex ✓
- `AppUpdateHelper` exists in only ONE dex (classes9.dex) ✓
- `HomeActivity` exists in only ONE dex (classes9.dex) ✓
- All pairip patches (SignatureCheck, LicenseClient, LicenseActivity) — **all verified** ✓
- `LicenseContentProvider` removed from manifest ✓

**Conclusion**: Every known code path is patched and verified in the binary. The dialog must be triggered by a mechanism we haven't traced — possibly an unidentified code path, a Play Core library auto-trigger, or a system-level detection.

**Nuclear approach**: Block the update dialog at the **Play Core library level** itself. `zzg` is the internal class that implements `AppUpdateManager`. It exists in a single dex (`classes2.dex`). Patching it blocks ALL update flows regardless of what app code triggers them.

**Implementation**:
1. `zzg.startUpdateFlowForResult()` (all 5 overloads) → `return false` — prevents the Play Store intent from being launched
2. `zzg.startUpdateFlow()` → `return null` — prevents the `PlayCoreDialogWrapperActivity` path
3. Remove `PlayCoreDialogWrapperActivity` from AndroidManifest — blocks the wrapper activity

**Status**: Dialog still appears — see iteration 10.

## Iteration 10: pairipfix-Style Approach + Native VM Discovery

**Error**: Play Store "Get this app from Play" dialog STILL appears despite nuclear Play Core library block.

**Logcat Analysis**: Captured full startup log via ADB. Key findings:

```
# Native VM crash ~40-60ms after startup
04-04 XX:XX:XX.XXX E/libc++abi: terminating due to length_error in vector
04-04 XX:XX:XX.XXX F/libc    : Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE)
```

The native VM (`libpairipcore.so`) **directly launches Play Store** via JNI:
```
# VM creates an Intent and calls startActivity() through JNI
Intent { act=android.intent.action.VIEW dat=http://play.google.com/store/license/paywall?id=com.jio.jioplay.tv }
```

This bypasses ALL Java-level patches because the native code calls `Context.startActivity()` directly through JNI, not through any Java method we can patch.

**pairipfix analysis**: Studied https://github.com/ahmedmani/pairipfix — an LSPosed/Xposed module that:
1. Hooks `LicenseClient.processResponse()` to force `responseCode = 0` (LICENSED)
2. Hooks `ResponseValidator.validateResponse()` → DO_NOTHING
3. Does **NOT** modify the APK — hooks at runtime, so native VM integrity checks pass
4. Does **NOT** handle the native VM crash — recommends BetterKnownInstalled for that

**BetterKnownInstalled analysis**: Studied https://github.com/Pixel-Props/BetterKnownInstalled — a Magisk module that:
1. Modifies `/data/system/packages.xml` at boot
2. Sets `installer=com.android.vending`, `installInitiator=com.android.vending`, `installerUid`, `packageSource=2`
3. Makes sideloaded apps appear as Play Store installs to the system
4. Requires root (Magisk/KernelSU)

**Attempted pairipfix-style bytecode approach**:
1. Changed `SignatureCheck.verifyIntegrity()` from `return-void` to setting expected signature fields to our APK's cert hash (`MpWsyp43Cdc9I5z/G6d8/6/a7cistsJdgxKXDrrT8z4=`)
2. Changed `LicenseClient.processResponse()` from `return-void` to `const/4 p1, 0x0` (force LICENSED)
3. Added `LicenseResponseHelper.validateResponse()` → `return-void` (no-op JWS validation)

**Result**: Still crashes. The native VM performs **dex file integrity verification** at the native level (CRC32/structural hash). Any dex modification triggers `length_error in vector` → SIGABRT, regardless of what Java methods are patched.

### Root Cause: Native-Level Integrity Checks

The pairip native VM (`libpairipcore.so`) performs 100+ security checks including:
- Dex file CRC32/structural hash verification
- APK signature validation (redundant with Java-level `SignatureCheck`)
- Anti-debugging checks
- Environment detection

When ANY dex file is modified (which morphe-cli always does), the native VM detects it and:
1. Launches Play Store paywall URL directly via JNI `startActivity()`
2. Crashes with `SIGABRT` (`length_error in vector` — C++ `std::vector` bounds error in `-fno-exceptions` mode → `std::terminate()`)

### Fundamental Limitation

**Dex-level patching (APK modification) CANNOT bypass pairip's native VM integrity checks.**

Known working solutions all require root or runtime hooking:
- **pairipfix** (LSPosed): Runtime hooks, no APK modification
- **BetterKnownInstalled** (Magisk): Fakes Play Store installer at system level
- **Reverse-engineering `libpairipcore.so`**: Possible but extremely complex (obfuscated native code)

### Package ID Change Research

Investigated whether changing the package ID or app name could help. Answer: **No**.
- The native VM would detect the package name change
- The app's encrypted code in `assets/` references the original package name
- pairip's paywall URL includes the package ID: `play.google.com/store/license/paywall?id=com.jio.jioplay.tv`

### Correct Startup Flow (Revised)

The startup flow is earlier than previously documented:

```
MultiDexApplication.<clinit>()              ← STATIC INITIALIZER (before attachBaseContext!)
  └── StartupLauncher.launch()
        └── VMRunner.invoke("mVBwD2didVTgj5k7", null)
              └── executeVM() → libpairipcore.so
                    ├── Integrity checks (dex CRC, signatures, etc.)
                    ├── If FAIL → startActivity(paywall URL) via JNI + SIGABRT
                    └── If PASS → decrypt and execute app bytecode

com.pairip.application.Application.attachBaseContext()
  ├── VMRunner.setContext(context)
  ├── SignatureCheck.verifyIntegrity(context)     ← Java-level check (redundant)
  └── super.attachBaseContext() → MultiDex.install()

com.jio.jioplay.tv.JioTVApplication.onCreate()   ← actual app code
```

The native VM runs in `<clinit>()` — **before** any instance methods like `attachBaseContext()`. This means the native integrity check happens before we can intercept anything at the Java level.

### InitContextProvider

`com.pairip.InitContextProvider` creates a `Context` via reflection on `ActivityThread` and sets up an `Instrumentation` instance. This allows the native VM to call `startActivity()` even before `Application.onCreate()` runs, which is how it launches the Play Store paywall URL.

### SignatureCheck Field Values

| Field | Original Value |
|-------|---------------|
| `expectedSignature` | `VkwE0TgslZMpxvR+ldSXr9FRIQ5NlCaBT+tvpXr3rTA=` |
| `ALLOWLISTED_SIG` | `Vn3kj4pUblROi2S+QfRRL9nhsaO2uoHQg6+dpEtxdTE=` |
| Our APK cert hash | `MpWsyp43Cdc9I5z/G6d8/6/a7cistsJdgxKXDrrT8z4=` |

## Testing Workflow

Testing uses ADB for direct install and logcat capture:

```bash
# Build
GITHUB_ACTOR=$(gh api user --jq '.login') GITHUB_TOKEN=$(gh auth token) ANDROID_HOME=~/Android/Sdk ./gradlew :patches:buildAndroid

# Patch
java -jar /tmp/morphe-cli-1.6.3-all.jar patch \
  -p patches/build/libs/patches-1.0.0-dev.8.mpp \
  -o /tmp/JioTV_patched.apk \
  "/home/rabil/Documents/JioTV_v7.1.7(404)_antisplit.apk"

# Install
adb install -r /tmp/JioTV_patched.apk

# Launch
adb shell monkey -p com.jio.jioplay.tv -c android.intent.category.LAUNCHER 1

# Logcat (filter by PID)
adb logcat -v time | grep <PID>
```

**Note**: The launcher activity is `com.jio.jioplay.tv.AppLogo` (activity-alias targeting `PermissionActivity`). Direct `am start` with `SplashActivity` fails — use `monkey` command instead.

## Key Lessons

1. **Never trust JADX deobfuscated names** — Always verify via `jadx_get_smali_of_class`
2. **Lifecycle overrides are virtual** — `onCreate`, `onStart`, `attachBaseContext`, etc.
3. **Static methods are direct** — `public static` → `.directMethods`
4. **Public instance methods are virtual** — Even if they look like utility methods
5. **Test incrementally** — Binary search to isolate failures
6. **Use morphe-cli for testing** — The official CLI is the definitive test environment
7. **Clean build before testing** — Stale `.mpp` files cause confusing errors
8. **Never neutralize the pairip VM** — The app's code is encrypted in `assets/`
9. **Multiple Play Store redirect paths exist** — Must patch all independently
10. **R8 obfuscation creates single-letter method names** — `a()`, `b()`, `c()` may be critical
11. **Multiple dex file copies** — Morphe-cli may redistribute classes; patch at the call site level when possible
12. **Static cached data** — Even if you prevent the API call, cached data in static fields can still trigger dialogs
13. **Never no-op constructors with return-void** — Dalvik requires `<init>` to call `super.<init>()` before returning; skipping it causes VerifyError, which can invalidate the entire class in that dex
14. **Trace ALL branches at the call site** — Clearing one condition variable isn't enough if an independent flag (`AppDataManager.inu`) provides an alternative path to the same target method
15. **When app-level patches fail, patch the library** — If patching app code doesn't stop a dialog, go lower: patch the library class that actually shows it (`zzg` for Play Core). Library classes typically exist in a single dex, making patches reliable
16. **Always verify patches in the binary** — Use `dexdump -d` on the patched APK to confirm injected instructions are present at the expected offsets
17. **Native VM integrity checks defeat dex-level patching** — pairip's `libpairipcore.so` verifies dex file CRC32/structural hashes at the native level. Any dex modification triggers SIGABRT, regardless of Java-level patches
18. **Native code can call startActivity via JNI** — The native VM uses `InitContextProvider` to get a Context, then launches Play Store paywall directly through JNI, bypassing all Java hooks
19. **Runtime hooking (LSPosed) != APK modification (Morphe)** — pairipfix works because it hooks at runtime without modifying the APK. Morphe patches modify dex files, which native integrity checks detect
20. **Static initializers run before attachBaseContext** — `MultiDexApplication.<clinit>()` calls `StartupLauncher.launch()` → native VM runs before any instance method can intercept it
21. **Changing package ID won't bypass pairip** — Native VM detects it, encrypted app code references original package, paywall URL embeds package ID

## Iteration 11: CRC32 Restoration (SafaSafari Method)

**Approach**: After morphe-cli patches dex files, restore original CRC32 values in the ZIP central directory using SafaSafari's `crc32_patcher.py`. The native VM reads CRC32 from ZIP metadata, so it should see original hashes.

**Implementation**:
1. Build patched APK with morphe-cli
2. Run `crc32_patcher.py patched.apk original.apk` — restores all 9 dex CRC32 values to originals
3. Re-sign APK (CRC32 patcher strips v2/v3 signatures)
4. Install and test

**Result**: Still crashes with `length_error in vector` → SIGABRT in `libpairipcore.so`.

**Why it failed**: The native VM does **structural hash verification** beyond ZIP CRC32. It computes its own hash of actual dex content at runtime, comparing against expected values stored in encrypted VM bytecode. The ZIP CRC32 is irrelevant to the actual verification.

**Key finding**: All 9 dex CRC32 values were successfully restored, confirming the patcher works. But the native VM performs deeper verification.

## Iteration 12: Stub Native Library Replacement

**Approach**: Replace `libpairipcore.so` with a stub library that exports the same JNI symbols (`executeVM`) but returns null. The app's Java code is NOT encrypted — only the VM bytecode is.

**Implementation**:
1. Compiled stub `.so` with NDK r27 (aarch64-linux-android21-clang)
2. Stub exports `JNI_OnLoad`, `JNI_OnUnload`, and `Java_com_pairip_VMRunner_executeVM`
3. Replaced native library in patched APK
4. Re-signed and installed

**Result**: **Native VM crash eliminated** — no more `libpairipcore.so` SIGABRT! But new crash in Firebase initialization:
```
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
    at com.google.firebase.internal.DataCollectionConfigStorage.<init>
    at com.google.firebase.FirebaseApp.lambda$new$0
    at com.google.firebase.crashlytics.FirebaseCrashlytics.init
```

**Why it failed**: The VM bytecode (executed by `executeVM`) likely initializes Firebase configuration or returns data that Firebase needs. Returning `null` breaks this initialization chain.

**Key insight**: The stub approach proves that:
1. The native VM is the ONLY thing preventing the app from launching
2. The app's Java code is fully functional and NOT encrypted
3. We need the VM to run successfully, not be disabled

## Iteration 13: Resource-Only Patches (No Dex Modifications)

**Approach**: Only modify resources (AndroidManifest.xml, network_security_config.xml) without touching any dex files. Disabled ALL bytecode patches. The native VM should see unmodified dex files and pass.

**Implementation**:
- Changed manifest application class to `com.jio.jioplay.tv.JioTVApplication`
- Removed pairip components from manifest
- Fixed network security config (added required `<domain>` child element)
- Disabled all bytecode patches (emulator, root, sslpinning, license check)

**Result**: Morphe-cli **always recompiles dex files** during patching, even with zero bytecode patches. Dex sizes changed dramatically (e.g., classes.dex: 11.2MB → 8.2MB), and classes were redistributed (9 → 12 dex files). Native VM still crashes.

**Key finding**: **Morphe-cli cannot produce an APK with unmodified dex files.** The patching process inherently recompiles all dex files.

## Iteration 14: Apktool Build

**Approach**: Use apktool to decode and rebuild the APK, modifying only resources. Apktool might preserve original dex files.

**Result**: Apktool also recompiles dex files from smali, even when "smali has not changed." Different smali versions produce different dex output.

## Iteration 15: Manual Binary XML Modification (In Progress)

**Approach**: Modify the APK's binary AndroidManifest.xml directly using a binary XML editor or manual byte manipulation, then repack with original dex files (no recompilation).

**Status**: Exploring binary XML parsing and modification tools.

## External Research Summary

Key findings from external sources (see `external-pairip-research.md`):

- **Solaree/pairipcore**: Native library has 100+ security checks, self-decrypts at runtime, uses runtime function fixup
- **SafaSafari/bypass_libpairipcore**: CRC32 restoration approach (failed for JioTV)
- **Snailsoft/Sbenny**: Uses `libpairipcorex.so` replacement library + APKEditor pipeline
- **ahmedmani/pairipfix**: LSPosed runtime hooks (requires root)
- **BetterKnownInstalled**: Magisk module faking Play Store installer (requires root)
- **Kitsuri-Studios/unpaircore**: Fake libpairipcore.so written in C for research
- **qyzhaojinxi/bypass_pairipcore**: Frida-based runtime hooking

## Updated Key Lessons

22. **CRC32 restoration is insufficient** — Native VM does structural hash verification beyond ZIP CRC32
23. **Stub native library eliminates VM crash** — But breaks Firebase initialization that depends on VM results
24. **Morphe-cli always recompiles dex** — Even with zero bytecode patches, dex files are modified
25. **Apktool also recompiles dex** — Smali-to-dex compilation produces different output
26. **The app's Java code is NOT encrypted** — Only the VM bytecode in assets/ is encrypted
27. **We need the VM to succeed, not be disabled** — The VM likely initializes Firebase config
28. **External tools use native library replacement** — `libpairipcorex.so` is a bypassed version, not a stub
