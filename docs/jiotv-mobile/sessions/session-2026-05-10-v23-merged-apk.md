# Session 2026-05-10 (v23): Merged XAPK Build — All Split Resources Included

## Summary

Previous builds (v12–v22) used only the base APK (`com.jio.jioplay.tv.apk`) which lacks density/arch split resources. This caused ~897 `Resources$NotFoundException` crashes for ExoPlayer drawables and other split-only resources.

**v23 is built from the full XAPK merge** using APKEditor, combining all 8 split APKs into one. All resources (ExoPlayer icons, density drawables, native libs) are now included.

## Root Cause of Previous Failures

The JioTV app is distributed as an Android App Bundle with split APKs:
- `base.apk` — code + `resources.arsc` table, but **no density-specific drawables**
- `split_config.*.apk` — density drawables, native libs per arch

Previous v12–v21 builds only used `base.apk`, so `resources.arsc` had resource IDs pointing to files that didn't exist in the APK. v22 tried apktool-rebuilt resources but got package ID mismatch (`No package ID 6b found for ID 0x6b0b0013`).

## v23 Build Process

### Step 1: Merge XAPK with APKEditor
```bash
java -jar APKEditor.jar m -i jiotv-7-1-7.xapk -o jiotv-merged.apk
```
Result: 4089 files, all splits combined, `resources.arsc` contains all resource entries.

### Step 2: Decompile with apktool
```bash
apktool d jiotv-merged.apk -o apktool-work -f
```

### Step 3: Apply manifest patches
- `android:name` → `com.jio.jioplay.tv.JioTVApplication` (was `com.pairip.application.Application`)
- `android:extractNativeLibs="true"` (was `"false"`)
- `android:usesCleartextTraffic="true"` (was `"false"`)
- `FirebaseInitProvider` already present (no change needed)

### Step 4: Apply smali patches
| Class | Dex | Patch |
|-------|-----|-------|
| `JioTVApplication.attachBaseContext` | classes3 | Added `sget-object v0, pkOPEgq;->hxbBMeNJrlJB` after MultiDex.install |
| `LicenseClientV3.initializeLicenseCheck` | classes5 | `return-void` at start |
| `LicenseClientV3.processResponse` | classes5 | Set OK state + return-void at start |
| `LicenseClientV3.showPaywall` | classes5 | `return-void` at start |
| `LicenseClientV3.showErrorDialog` | classes5 | `return-void` at start |
| `APIManager.isSslPining` check | classes5 | `goto :cond_1` (always skip pinning) |
| `JioPlayer k.c` cert pin check | classes | `goto :cond_c` (always skip cert verification) |

### Step 5: Fix layout XML
- `fragment_saavn_main.xml`: `android:layout_gravity="0x0"` → `android:layout_gravity="center"`
  (apktool can't compile `0x0` as a flags attribute)

### Step 6: Rebuild with apktool
```bash
apktool b apktool-work -o jiotv-v23-rebuilt.apk
```

### Step 7: Sign and align
```bash
zipalign -f 4096 jiotv-v23-rebuilt.apk jiotv-v23-aligned.apk
apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android jiotv-v23-aligned.apk
```

## Key Difference from Previous Builds

| Build | Base | Resources | Issue |
|-------|------|-----------|-------|
| v12–v21 | base.apk only | Missing split drawables | ExoPlayer crash, channels view empty |
| v22 | base.apk + apktool full rebuild | Package ID mismatch | Stuck on splash screen |
| **v23** | **Full XAPK merge** | **All resources included** | **Should resolve both issues** |

## Risk: apktool-Rebuilt resources.arsc

Previous attempts using apktool-rebuilt `resources.arsc` caused `ClassNotFoundException` for the pairip Application class. This was because:
- v16/v18: apktool-rebuilt resources from base APK had incompatible package IDs
- v22: apktool-rebuilt resources from base APK caused `No package ID 6b` error

v23 uses the **merged APK** which already has all resources in a single `resources.arsc`. The apktool rebuild should preserve these correctly since all resource entries exist in the table. The `fragment_saavn_main.xml` fix was needed to resolve the only compilation error.

## Files

- `/tmp/dex-patch/jiotv-merged.apk` — APKEditor-merged XAPK (before patches)
- `/tmp/dex-patch/apktool-work/` — Decompiled + patched smali
- `/tmp/dex-patch/jiotv-v23-rebuilt.apk` — Rebuilt APK (before alignment)
- `/tmp/dex-patch/jiotv-v23-aligned.apk` — Final signed APK (44MB, ready to install)