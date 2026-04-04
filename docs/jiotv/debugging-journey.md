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

**Planned fix**: Patch `HomeActivity.onCreate()` directly to skip the entire update block (lines 18669-18795 in smali). This is the only way to guarantee the update code never runs, regardless of dex file duplication.

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

**Status**: Awaiting user testing.

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
