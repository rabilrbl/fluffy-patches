# Session 2026-05-10: v23–v29 Merged APK + Pairip String Class Fixes

## Context
v20 APK (base.apk only) worked but crashed on content playback due to missing split APK resources. v23 switches to a merged XAPK build.

## v23: Merged XAPK Build
- **Source**: `jiotv-7-1-7.xapk` (8 APKs: base + 4 density + 2 arch + config.en)
- **Merge**: `java -jar APKEditor.jar m -i jiotv-7-1-7.xapk -o jiotv-merged.apk`
- **Result**: Complete `resources.arsc` with all 897+ missing drawables
- **apktool rebuild**: `apktool d jiotv-merged.apk -o apktool-work -f -s` then `apktool b apktool-work`
- **Key insight**: Full rebuild works when starting from merged APK — base.apk-only builds broke class loading because resources.arsc was incomplete

## v23 Crash: SIGSEGV in libpairipcore.so
- Merged APK includes `libpairipcore.so` from arch splits
- App loads native lib → segfault because integrity check fails
- **Fix**: Remove `libpairipcore.so` from all arch dirs in `apktool-work/lib/`

## v24: UnsatisfiedLinkError
- `VMRunner.<clinit>` calls `System.loadLibrary("pairipcore")` even though `StartupLauncher.launch()` returns void
- `CoreComponentFactory.<clinit>` references VMRunner, triggering class init
- ExceptionInInitializerError → NoClassDefFoundError on subsequent accesses
- **Fix**: Patch `VMRunner.<clinit>` to `return-void`, `VMRunner.invoke()` to return null immediately

## v25: NullPointerException in SystemPropsKt.systemProp()
- `kEpRxMC.aKPJrxNCR` is null, passed to `System.getProperty()` as key
- `kEpRxMC` is the second pairip string class (48 fields, all null without VM init)
- **Fix**: Null guard in `SystemPropsKt.systemProp(String)` — `if-eqz p0, :cond_null; ... :cond_null; const/4 p0, 0x0; return-object p0`

## v26: NullPointerException in ImmutableList.Builder
- Firebase Analytics `zzd.<clinit>` builds `ImmutableList` with null elements from `kEpRxMC` fields
- `ImmutableList.Builder.add()` throws NPE when element is null
- **Fix attempt**: Try-catch in `FirebaseInitProvider.onCreate()` — didn't help because clinit errors are unrecoverable

## v27: ExceptionInInitializerError still crashes
- Try-catch doesn't help — when a class initializer fails, the class is permanently broken
- Subsequent accesses throw NoClassDefFoundError, not the original exception
- **Fix attempt**: Remove FirebaseInitProvider from manifest entirely

## v28: "Default FirebaseApp is not initialized"
- Removing FirebaseInitProvider means Firebase never initializes
- App code that depends on `FirebaseApp.getInstance()` crashes
- **Fix**: Restore FirebaseInitProvider, add `<clinit>` to both pairip string classes

## v29: Pairip String Class clinit Patches
- **pkOPEgq** (`com.iab.omid.library.ril.HNCT.pkOPEgq`): 48 String fields, all initialized to `""` in `<clinit>`
- **kEpRxMC** (`org.apache.commons.net.ftp.Rso.kEpRxMC`): 48 String fields, all initialized to `""` in `<clinit>`
- These empty string defaults prevent NPEs when Firebase/OkHttp accesses them before the pairip VM would normally fill them
- **NOT YET TESTED** on device (emulator killed before testing)

## Smali Changes Summary

| File | Change |
|------|--------|
| `VMRunner.smali` (classes10) | `<clinit>` → return-void, `invoke()` → return null |
| `StartupLauncher.smali` (classes3) | `launch()` → return-void at start |
| `SystemPropsKt.smali` (classes3) | Null guard in `systemProp(String)` |
| `FirebaseInitProvider.smali` (classes6) | try-catch in `onCreate()` |
| `kEpRxMC.smali` (classes2) | `<clinit>` assigning `""` to all 48 fields |
| `pkOPEgq.smali` (classes4) | Full rewrite with `<clinit>` assigning `""` to all 48 fields |
| `JioTVApplication.smali` (classes3) | Force-init pkOPEgq in `attachBaseContext()` |
| `APIManager.smali` (classes5) | SSL pinning bypass (`if-eqz` → `goto`) |
| `c.smali` (classes root) | JioPlayer cert pin bypass |
| `LicenseClientV3.smali` (classes5) | License bypass (multiple methods → return-void) |

## Status
- v29 built successfully, installed on AVD
- **NOT TESTED** — emulator killed before verification
- Next: test v29 on device, verify no NPE crashes, test content playback