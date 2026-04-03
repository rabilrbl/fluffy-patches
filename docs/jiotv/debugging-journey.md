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
