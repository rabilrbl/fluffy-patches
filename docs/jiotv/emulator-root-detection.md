# Emulator & Root Detection

## Emulator Detection

### Location
`com.jio.media.tv.ui.permission_onboarding.PermissionActivity` (`Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;`)

### Detection Methods

#### `isRunningOnEmulator()` → **virtual method**
Checks:
- `Build.FINGERPRINT` contains "generic", "sdk", "emulator"
- `Build.MODEL` contains "Emulator", "Android SDK"
- `Build.BRAND` contains "generic"
- `Build.HARDWARE` contains "goldfish", "ranchu"
- `/system/bin/qemud` exists
- `/dev/socket/qemud` exists
- `/system/lib/libc_malloc_debug_qemu.so` exists
- `/system/bin/qemu-props` exists
- BlueStacks folder: `/storage/emulated/0/windows/BstSharedFolder` exists
- Fire TV detection

**Bypass**: `return false`

#### `isSupportedDevice()` → **virtual method**
Checks:
- `PackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)` — Android TV
- Other device compatibility checks

**Bypass**: `return true`

#### `onCreate()` → **virtual method**
The detection methods are called early in `onCreate()`. The bypass replaces the entire method body with:
```smali
invoke-super {p0}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V
invoke-virtual {p0}, Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;->proceedApplication()V
return-void
```

This skips all detection logic and proceeds directly to the app.

## Root Detection

### Location 1: Firebase Crashlytics
`com.google.firebase.crashlytics.internal.common.CommonUtils` (`Lcom/google/firebase/crashlytics/internal/common/CommonUtils;`)

#### `isRooted()` → **direct method**
Checks:
- `Build.TAGS` contains "test-keys"
- `/system/app/Superuser.apk` exists
- `/system/xbin/su` exists

**Bypass**: `return false`

### Location 2: JioTV Security Utils
`com.jio.jioplay.tv.utils.SecurityUtils` (`Lcom/jio/jioplay/tv/utils/SecurityUtils;`)

#### `isValidVersionName()` → **direct method**
Version validation check.

**Bypass**: `return true`

### Location 3: JioTV Common Utils
`com.jio.jioplay.tv.utils.CommonUtils` (`Lcom/jio/jioplay/tv/utils/CommonUtils;`)

#### `showXposedFrameworkDetectionDialog()` → **direct method**
Shows a dialog when Xposed framework is detected.

**Bypass**: `return-void`
