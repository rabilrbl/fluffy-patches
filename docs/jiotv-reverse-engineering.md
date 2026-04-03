# JioTV Reverse Engineering Documentation

## Target Application

- **Package**: `com.jio.jioplay.tv`
- **Version**: 7.1.7 (404)
- **APK**: `JioTV_v7.1.7(404)_antisplit.apk` (anti-split, single APK)
- **Source**: User-provided at `/var/home/rabil/Documents/JioTV_v7.1.7(404)_antisplit.apk`

## Patch Goals

1. **Remove Play Store license check** - Bypass pairip DRM and Google Play Core in-app updates
2. **Remove emulator detection** - Bypass device restriction checks
3. **Remove root detection** - Bypass root/Xposed detection
4. **Remove SSL certificate pinning** - Allow MITM proxy interception
5. **Enable cleartext traffic** - Allow HTTP traffic and user CAs

## Reverse Engineering Methodology

### Tools Used

| Tool | Purpose |
|------|---------|
| **JADX-GUI** | Primary decompiler with MCP plugin for programmatic access |
| **morphe-cli** | Official CLI for testing patches against the APK |
| **unzip/strings** | Quick APK content inspection |
| **Python3** | Smali analysis scripts |

### JADX Setup

The JADX MCP plugin must be connected to query the decompiled APK programmatically:

```
jadx_fetch_current_class    # Get currently selected class
jadx_get_class_source       # Get full Java source of a class
jadx_get_smali_of_class     # Get smali representation (CRITICAL)
jadx_search_classes_by_keyword  # Search by keyword with scope filters
jadx_get_methods_of_class   # List all method names in a class
jadx_get_xrefs_to_method    # Find callers of a method
jadx_get_xrefs_to_class     # Find references to a class
```

## Critical Discovery: JADX Deobfuscation vs Real Smali Names

### The Problem

JADX deobfuscates package names. The decompiled Java shows:
- `com.jio.jioplay.p037tv.utils.CommonUtils`
- `com.jio.media.p062tv.p063ui.permission_onboarding.PermissionActivity`
- `com.jio.jioplay.p037tv.data.firebase.FirebaseConfig`

But the **actual smali names** in the APK are:
- `Lcom/jio/jioplay/tv/utils/CommonUtils;`
- `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;`
- `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;`

The obfuscation tokens (`p037`, `p062`, `p063`) are simply **removed** in the real smali.

### How to Find Real Smali Names

Always use `jadx_get_smali_of_class` and check the `.class` declaration line:

```
jadx_get_smali_of_class("com.jio.jioplay.p037tv.utils.CommonUtils")
```

Returns:
```
.class public final Lcom/jio/jioplay/tv/utils/CommonUtils;
```

**Rule**: Always verify class names via smali before writing patches. Never trust JADX's deobfuscated package names.

## Critical Discovery: Direct vs Virtual Methods

### The Problem

`.directMethods` and `.virtualMethods` in the Morphe patcher API correspond to smali's `# direct methods` and `# virtual methods` sections.

**Direct methods include**:
- Constructors (`<init>`, `<clinit>`)
- `private` methods
- `static` methods
- `final` methods (sometimes)

**Virtual methods include**:
- `public`/`protected` non-final instance methods
- **Override methods** (`onCreate`, `onStart`, `attachBaseContext`, etc.)

### Common Mistakes

| Method | Assumed | Actual | Reason |
|--------|---------|--------|--------|
| `onCreate` (Activity/ContentProvider) | direct | **virtual** | Lifecycle override |
| `onStart` (Activity) | direct | **virtual** | Lifecycle override |
| `attachBaseContext` (Application) | direct | **virtual** | Lifecycle override |
| `initializeLicenseCheck` (LicenseClient) | direct | **virtual** | Public instance method |
| `isSslPining` (FirebaseConfig) | direct | **virtual** | Public instance method |
| `check$okhttp` (CertificatePinner) | direct | **virtual** | Public instance method |
| `isRunningOnEmulator` (PermissionActivity) | direct | **virtual** | Public instance method |
| `isSupportedDevice` (PermissionActivity) | direct | **virtual** | Public instance method |
| `checkUpdate` (AppUpdateHelper) | direct | **virtual** | Public instance method |
| `checkUpdatefordiag` (AppUpdateHelper) | direct | **virtual** | Public instance method |
| `checkIsUpdateAvailable` (CommonUtils) | direct | direct | `static` method ✓ |
| `redirectToPlayStore` (CommonUtils) | direct | direct | `static` method ✓ |
| `takeToPlayStore` (CommonUtils) | direct | direct | `static` method ✓ |
| `a` (AppUpdateHelper) | direct | direct | `static` method ✓ |

### How to Verify Method Types

Use `jadx_get_smali_of_class` and look for section markers:

```smali
# direct methods
.method public static checkIsUpdateAvailable()V  # <-- DIRECT
    ...
.end method

# virtual methods
.method public onStart()V  # <-- VIRTUAL
    ...
.end method
```

Or use a Python script to scan the smali output:

```python
import json
with open('smali_output.json') as f:
    data = json.load(f)
content = data['response']
lines = content.split('\n')

in_direct = False
in_virtual = False
for i, line in enumerate(lines):
    if '# direct methods' in line:
        in_direct, in_virtual = True, False
    elif '# virtual methods' in line:
        in_virtual, in_direct = True, False
    elif '.method' in line and 'targetMethodName' in line:
        print(f"{'DIRECT' if in_direct else 'VIRTUAL'}")
```

## Patch Architecture

### Smali Name Mapping Table

| JADX Name | Real Smali Name |
|-------------|----------------|
| `com.jio.jioplay.p037tv.utils.CommonUtils` | `Lcom/jio/jioplay/tv/utils/CommonUtils;` |
| `com.jio.jioplay.p037tv.utils.SecurityUtils` | `Lcom/jio/jioplay/tv/utils/SecurityUtils;` |
| `com.jio.jioplay.p037tv.utils.AppUpdateHelper` | `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` |
| `com.jio.jioplay.p037tv.data.firebase.FirebaseConfig` | `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;` |
| `com.jio.media.p062tv.p063ui.permission_onboarding.PermissionActivity` | `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;` |
| `com.pairip.VMRunner` | `Lcom/pairip/VMRunner;` |
| `com.pairip.StartupLauncher` | `Lcom/pairip/StartupLauncher;` |
| `com.pairip.SignatureCheck` | `Lcom/pairip/SignatureCheck;` |
| `com.pairip.application.Application` | `Lcom/pairip/application/Application;` |
| `com.pairip.licensecheck.LicenseContentProvider` | `Lcom/pairip/licensecheck/LicenseContentProvider;` |
| `com.pairip.licensecheck.LicenseClient` | `Lcom/pairip/licensecheck/LicenseClient;` |
| `com.pairip.licensecheck.LicenseActivity` | `Lcom/pairip/licensecheck/LicenseActivity;` |

### pairip DRM Library

The pairip library is a **native DRM solution** that:
- Loads `libpairipcore.so` native library in `VMRunner.<clinit>`
- Executes encrypted VM bytecode from `assets/` directory
- Performs signature verification (`SignatureCheck.verifyIntegrity`)
- Manages license checking via `LicenseClient`
- Shows paywall/error dialogs via `LicenseActivity`

**Bypass strategy**:
1. **Manifest**: Remove `LicenseContentProvider` only (do NOT change application class — the pairip Application is needed for VM initialization)
2. **Bytecode**: Bypass signature check and license-related methods. **Do NOT neutralize** `VMRunner.<clinit>`, `setContext`, or `StartupLauncher.launch` — the app's actual code is encrypted in `assets/` and executed by the native VM.

### pairip VM Execution and the Splash Screen Crash

**Critical**: The app's actual Java code is **encrypted** inside the `assets/` directory. The pairip native library (`libpairipcore.so`) contains a custom VM that decrypts and executes this bytecode at runtime.

**Startup flow**:
```
Android Framework
  └── com.pairip.application.Application.attachBaseContext()
        ├── VMRunner.setContext(context)
        ├── SignatureCheck.verifyIntegrity(context)    ← signature check
        └── super.attachBaseContext() → MultiDex.install()
  └── StartupLauncher.launch()
        └── VMRunner.invoke("mVBwD2didVTgj5k7", null)
              ├── VmDecryptor.decrypt()                ← decrypts bytecode from assets/
              └── executeVM(bytecode, args)            ← native call to libpairipcore.so
  └── com.jio.jioplay.tv.JioTVApplication.onCreate()   ← actual app initialization
```

**Crash cause**: Neutralizing `VMRunner.<clinit>`, `setContext`, or `StartupLauncher.launch` prevents the VM from ever running. The app has no code to execute → immediate crash on splash screen.

**Correct approach**: Only patch:
- `SignatureCheck.verifyIntegrity` — bypass APK signature verification
- `LicenseClient` methods — disable license checking, paywall, error dialogs
- `LicenseActivity.onStart` — auto-finish any paywall that somehow appears
- Play Store redirect helpers
- Google Play Core in-app update methods

### Play Store Redirect Paths (Multiple Sources)

There are **three distinct mechanisms** that can redirect to Play Store:

#### 1. pairip License Paywall
- **Entry**: `LicenseContentProvider.onCreate()` → `LicenseClient.initializeLicenseCheck()`
- **Flow**: License check fails → `handleError()` → `startErrorDialogActivity()` → `LicenseActivity`
- **UI**: "Get this app from Play" dialog with sad face emoji
- **Bypass**: Remove provider from manifest + neutralize `LicenseClient` methods + `LicenseActivity.onStart`

#### 2. Google Play Core In-App Update
- **Entry**: `HomeActivity.onCreate()` / `HomeActivity.onResume()` → `AppUpdateHelper.checkUpdate()`
- **Flow**: `checkUpdate()` → `AppUpdateManager.getAppUpdateInfo()` → callback → `AppUpdateHelper.a()` → `startUpdateFlowForResult()`
- **UI**: Standard Google Play update dialog ("Get this app from Play")
- **Bypass**: Neutralize `AppUpdateHelper.checkUpdate()`, `checkUpdatefordiag()`, and static `a()` method

#### 3. Server-Driven Update Check
- **Entry**: `PermissionActivity.onCreate()` → `CommonUtils.checkIsUpdateAvailable()`
- **Flow**: API call → `C0062Az.onResponse()` → `CommonUtils.setCheckAppUpadteData()` → `HomeActivity.onCreate()` reads data → shows `JioDialog` with update button
- **Data model**: `CheckAppUpadteData` (version, url, description, heading, mandatory)
- **UI**: Custom `JioDialog` with "Update" button → `CommonUtils.takeToPlayStore()` → `finishAndClear()`
- **Bypass**: Neutralize `checkIsUpdateAvailable()`, `redirectToPlayStore()`, `takeToPlayStore()`

#### 4. Other Redirect Paths (not patched, but documented)
- **JioCinema redirect**: `VideoPlayerHandler.allowPlayingVideo()` — redirects to JioCinema Play Store when broadcasterId == 27
- **JioGames redirect**: `GamesRedirection.redirectToAppOrPlayStore()` — redirects to JioGames Play Store
- **Deep links**: `DeepLinkManager.takeToRelatedScreen()` — redirects to JioCinema if not installed
- **Chrome dialog**: `DialogInterfaceOnClickListenerC12286yz.onClick()` — opens Chrome Play Store page

### AppUpdateHelper Method Obfuscation

The `AppUpdateHelper` class uses R8 obfuscation for its key methods:

| Java Name | Smali Name | Type | Purpose |
|-----------|------------|------|---------|
| `checkUpdate()` | `checkUpdate` | virtual | Starts Play Core update check |
| `checkUpdatefordiag()` | `checkUpdatefordiag` | virtual | Diagnostic variant of update check |
| `a(AppUpdateHelper, AppUpdateInfo)` | `a` | direct (static) | Actually calls `startUpdateFlowForResult()` |
| `b(AppUpdateHelper)` | `b` | direct (static) | Complete update callback |
| `c()` | `c` | virtual | Shows "JioTV has downloaded an update" snackbar |

The static `a()` method is the critical one — it's what actually triggers the Play Store update flow. All three must be neutralized.

## Testing Workflow

### Build Patches
```bash
ANDROID_HOME=$HOME/Android/Sdk \
GITHUB_ACTOR=rabilrbl \
GITHUB_TOKEN=<token> \
./gradlew :patches:buildAndroid
```

### Test with morphe-cli
```bash
rm -rf /tmp/morphe-tmp
java -jar morphe-cli-1.6.3-all.jar patch \
  --continue-on-error \
  -p patches/build/libs/patches-1.0.0-dev.1.mpp \
  --force \
  -t /tmp/morphe-tmp \
  -o /tmp/morphe-tmp/output.apk \
  JioTV_v7.1.7\(404\)_antisplit.apk
```

### Binary Search Debugging

When a patch fails with "Collection contains no element matching the predicate":
1. Split the patch in half
2. Test first half → if fails, split again; if passes, test second half
3. Repeat until you isolate the failing method
4. Verify the method's type (direct/virtual) via smali

## Known Issues & Caveats

### 1. Synthetic Lambda Methods

Kotlin generates synthetic methods with names like `lambda$methodName$0`. When searching by method name, `.first { it.name == "methodName" }` may match a synthetic lambda instead of the real method. Always check the smali to confirm.

Example in `LicenseClient`:
- `lambda$initializeLicenseCheck$0()` - synthetic lambda (direct)
- `lambda$initializeLicenseCheck$1(Z)` - synthetic lambda (direct)
- `initializeLicenseCheck()` - real public method (virtual)

### 2. Multiple Overloaded Methods

Some methods have multiple overloads. `.first { it.name == "methodName" }` returns the first match, which may not be the one you want. Check parameter signatures in smali.

### 3. Resource Patching

Resource patches use `document("AndroidManifest.xml")` and `document("res/xml/...")`. The `document()` function returns a closable `org.w3c.dom.Document`. Always use `.use { }` block.

### 4. Network Security Config

The original `network_security_config.xml` has domain-specific configs. To enable full MITM:
- Set `cleartextTrafficPermitted="true"` on `<base-config>`
- Add user CA certificates to `<trust-anchors>`
- The manifest `usesCleartextTraffic` attribute must also be set

### 5. Emulator Detection Bypass

`PermissionActivity.onCreate()` calls detection methods early. The bypass injects `super.onCreate()` + `proceedApplication()` at the start, skipping all detection logic that would normally run before `proceedApplication()` is called.

### 6. R8 Obfuscated Method Names

Some classes use R8 single-letter method names (`a`, `b`, `c`). These are static bridge methods that call the actual logic. When patching, use the obfuscated name as it appears in smali, not the deobfuscated Java name.

Example: `AppUpdateHelper.a(AppUpdateHelper, AppUpdateInfo)` is the method that calls `startUpdateFlowForResult()`.

## Iterative Debugging Journey

### Iteration 1: Wrong smali names
- **Error**: "Collection contains no element matching the predicate"
- **Cause**: Used JADX deobfuscated names (`p037tv`) instead of real smali names (`tv`)
- **Fix**: Verified all class names via `jadx_get_smali_of_class`

### Iteration 2: Wrong method types (direct vs virtual)
- **Error**: Same predicate error on different methods
- **Cause**: Lifecycle overrides (`onCreate`, `onStart`, `attachBaseContext`) and public instance methods (`initializeLicenseCheck`, `isSslPining`) are virtual, not direct
- **Fix**: Checked smali section markers (`# direct methods` vs `# virtual methods`) for each method

### Iteration 3: Splash screen crash
- **Error**: App crashes immediately on splash screen
- **Cause**: Neutralized `VMRunner.<clinit>`, `setContext`, and `StartupLauncher.launch` — the app's code is encrypted in `assets/` and needs the VM to run
- **Fix**: Only patch signature check and license methods. Let the VM execute normally.

### Iteration 4: Play Store redirect returns
- **Error**: "Get this app from Play" dialog appears after splash
- **Cause**: Google Play Core in-app update (`AppUpdateHelper.checkUpdate()`) was not patched
- **Fix**: Added patches for `AppUpdateHelper.checkUpdate()`, `checkUpdatefordiag()`, and static `a()` method

## Lessons Learned

1. **Never trust JADX deobfuscated names** - Always verify via `jadx_get_smali_of_class`
2. **Lifecycle overrides are virtual** - `onCreate`, `onStart`, `attachBaseContext`, etc.
3. **Static methods are direct** - `public static` → `.directMethods`
4. **Public instance methods are virtual** - Even if they look like utility methods
5. **Test incrementally** - Binary search to isolate failures
6. **Use morphe-cli for testing** - The official CLI is the definitive test environment
7. **Clean build before testing** - Stale `.mpp` files cause confusing errors
8. **Never neutralize the pairip VM** - The app's code is encrypted in `assets/` and executed by the native VM. Neutralizing `VMRunner` or `StartupLauncher` causes an immediate splash screen crash. Only patch signature check and license-related methods.
9. **Multiple Play Store redirect paths exist** - pairip paywall, Google Play Core in-app updates, server-driven update checks, and app-specific redirects (JioCinema, JioGames). All must be patched independently.
10. **R8 obfuscation creates single-letter method names** - `a()`, `b()`, `c()` may be the critical methods. Check smali to understand what each does.
