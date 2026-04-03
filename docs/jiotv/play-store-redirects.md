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

## Other Redirect Paths (Not Patched)

| Path | Class | Trigger | Description |
|------|-------|---------|-------------|
| JioCinema | `VideoPlayerHandler.allowPlayingVideo()` | broadcasterId == 27 | Redirects to JioCinema Play Store when content requires it |
| JioGames | `GamesRedirection.redirectToAppOrPlayStore()` | JioGames not installed | Redirects to JioGames Play Store |
| Deep Links | `DeepLinkManager.takeToRelatedScreen()` | `jiovootviacom18://` deep link | Redirects to JioCinema if not installed |
| Chrome Dialog | `DialogInterfaceOnClickListenerC12286yz.onClick()` | Chrome not installed | Opens Chrome Play Store page |
