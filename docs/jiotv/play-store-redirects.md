# Play Store Redirect Paths

There are **three distinct mechanisms** that can redirect to Play Store in JioTV.

## 1. pairip License Paywall

**UI**: "Get this app from Play" dialog with sad face emoji

### Entry Point
`LicenseContentProvider.onCreate()` → `new LicenseClient(context).initializeLicenseCheck()`

### Flow
```
LicenseContentProvider.onCreate()
  → LicenseClient.initializeLicenseCheck()
    → Local installer check
    → connectToLicensingService() → binds to Google Play Licensing
    → processResponse(responseCode)
      ├── responseCode == 2 → startPaywallActivity()
      └── failure → handleError() → startErrorDialogActivity()
        → LicenseActivity (shows "Get this app from Play")
```

### Bypass
- Manifest: Remove `LicenseContentProvider`
- Bytecode: Neutralize all `LicenseClient` methods + `LicenseActivity.onStart`

## 2. Google Play Core In-App Update

**UI**: Standard Google Play update dialog ("Get this app from Play")

### Entry Points
- `HomeActivity.onCreate()` → `AppUpdateHelper.checkUpdate()` (when `CheckAppUpadteData` exists but not mandatory, or `AppDataManager.inu == true`)
- `HomeActivity.onResume()` → `AppUpdateHelper.resumeUpdate()`

### Flow
```
HomeActivity.onCreate()
  └── AppUpdateHelper.checkUpdate()
        └── AppUpdateManager.getAppUpdateInfo()
              └── OnSuccessListener → AppUpdateHelper.a()
                    └── startUpdateFlowForResult() → Play Store dialog

HomeActivity.onResume()
  └── AppUpdateHelper.resumeUpdate()
        └── AppUpdateManager.getAppUpdateInfo()
              └── OnSuccessListener → shows "Install" snackbar if update downloaded
```

### Key Classes
| Class | Smali Name | Purpose |
|-------|------------|---------|
| `AppUpdateHelper` | `Lcom/jio/jioplay/tv/utils/AppUpdateHelper;` | Wrapper around Play Core API |
| `AppUpdateManager` | `Lcom/google/android/play/core/appupdate/AppUpdateManager;` | Google Play Core interface |

### AppUpdateHelper Methods
| Java Name | Smali Name | Type | Purpose |
|-----------|------------|------|---------|
| `checkUpdate()` | `checkUpdate` | virtual | Starts Play Core update check |
| `checkUpdatefordiag()` | `checkUpdatefordiag` | virtual | Diagnostic variant |
| `resumeUpdate()` | `resumeUpdate` | virtual | Resumes pending update |
| `a(AppUpdateHelper, AppUpdateInfo)` | `a` | direct (static) | Calls `startUpdateFlowForResult()` |
| `b(AppUpdateHelper)` | `b` | direct (static) | Complete update callback |
| `c()` | `c` | virtual | Shows "JioTV has downloaded an update" snackbar |

### Bypass
- `AppUpdateHelper.checkUpdate` → `return-void`
- `AppUpdateHelper.checkUpdatefordiag` → `return-void`
- `AppUpdateHelper.resumeUpdate` → `return-void`
- `AppUpdateHelper.a` → `return-void`

## 3. Server-Driven Update Check

**UI**: Custom `JioDialog` with "Update" and "Exit" buttons

### Entry Point
`PermissionActivity.onCreate()` → `CommonUtils.checkIsUpdateAvailable()`

### Flow
```
PermissionActivity.onCreate()
  └── CommonUtils.checkIsUpdateAvailable()
        └── APIManager.checkVersionUpdate()
              └── C0062Az.onResponse()
                    └── CommonUtils.setCheckAppUpadteData(response)

HomeActivity.onCreate()
  └── CommonUtils.getCheckAppUpadteData()
        ├── data != null && mandatory == true → JioDialog (non-cancelable)
        │     └── "Update" button → CommonUtils.takeToPlayStore() → finishAndClear()
        │     └── "Exit" button → exit app
        └── data != null && mandatory == false → AppUpdateHelper.checkUpdate()
        └── data == null && AppDataManager.inu == true → AppUpdateHelper.checkUpdate()
```

### Data Model
`CheckAppUpadteData` fields:
- `version` (Integer)
- `url` (String) — Play Store URL
- `description` (String)
- `heading` (String)
- `mandatory` (Boolean)

### Bypass
- `CommonUtils.getCheckAppUpadteData` → `return null` (prevents ALL branches in HomeActivity)
- `CommonUtils.checkIsUpdateAvailable` → `return-void` (prevents API call)
- `CommonUtils.redirectToPlayStore` → `return-void`
- `CommonUtils.takeToPlayStore` → `return-void`

## 4. Play Core Library (Nuclear Block)

**UI**: Any dialog shown by the Google Play Core update library

### Internal Class
`com.google.android.play.core.appupdate.zzg` — the actual `AppUpdateManager` implementation. Exists in a single dex (`classes2.dex`), making patches reliable.

### Flow
All update dialogs ultimately go through `zzg`:
```
zzg.startUpdateFlowForResult() → startIntentSenderForResult() → Play Store dialog
zzg.startUpdateFlow() → PlayCoreDialogWrapperActivity → Play Store dialog
```

### Bypass
- `zzg.startUpdateFlowForResult()` (all overloads) → `return false`
- `zzg.startUpdateFlow()` → `return null`
- Remove `PlayCoreDialogWrapperActivity` from AndroidManifest

## 5. Native VM Direct Paywall (JNI)

**UI**: Play Store's own "Get this app from Play" page (not an in-app dialog — the actual Play Store app)

**Screenshot**: `docs/jiotv/Play-store-license-check-failed.jpg`

### Trigger
The pairip native VM (`libpairipcore.so`) performs dex integrity checks during static initialization. When verification fails, it directly launches Play Store via JNI.

### Flow
```
MultiDexApplication.<clinit>()
  └── StartupLauncher.launch()
        └── VMRunner.invoke() → executeVM() (native)
              └── libpairipcore.so integrity check
                    ├── Verify dex CRC32/structural hashes
                    ├── On FAIL:
                    │     ├── InitContextProvider → get Context via ActivityThread reflection
                    │     ├── Context.startActivity(Intent(ACTION_VIEW, "http://play.google.com/store/license/paywall?id=com.jio.jioplay.tv"))
                    │     └── SIGABRT (length_error in vector → std::terminate)
                    └── On PASS: decrypt and execute app bytecode
```

### Key Details
- Uses `InitContextProvider` to create a Context via reflection on `ActivityThread` **before** `Application.onCreate()` runs
- Calls `startActivity()` directly through JNI, bypassing ALL Java-level patches
- The paywall URL is `http://play.google.com/store/license/paywall?id=com.jio.jioplay.tv`
- Crash is C++ `std::vector` length error in `-fno-exceptions` mode → `std::terminate()` → SIGABRT
- Happens ~40-60ms after app startup

### Bypass
**Cannot be bypassed with dex-level patching.** Any dex modification changes CRC32/structural hashes, which the native VM detects.

Known working approaches (all require root or runtime hooking):
- **pairipfix** (LSPosed module): Hooks at runtime without modifying the APK
- **BetterKnownInstalled** (Magisk module): Fakes Play Store installer at system level
- **Reverse-engineering `libpairipcore.so`**: Possible but extremely complex

## Other Redirect Paths (Not Patched)

| Path | Class | Trigger | Description |
|------|-------|---------|-------------|
| JioCinema | `VideoPlayerHandler.allowPlayingVideo()` | broadcasterId == 27 | Redirects to JioCinema Play Store when content requires it |
| JioGames | `GamesRedirection.redirectToAppOrPlayStore()` | JioGames not installed | Redirects to JioGames Play Store |
| Deep Links | `DeepLinkManager.takeToRelatedScreen()` | `jiovootviacom18://` deep link | Redirects to JioCinema if not installed |
| Chrome Dialog | `DialogInterfaceOnClickListenerC12286yz.onClick()` | Chrome not installed | Opens Chrome Play Store page |
