# Session 2026-05-09 (v2): Clinit Injection + Caller Patching — App Launches!

## Summary

JioTV app successfully launches past pairip verification and Firebase initialization. The app displays content channels. The key breakthrough was combining clinit string injection (for pairip classes in recompilable dex files) with caller patching (for pairip classes in non-recompilable dex files).

## Patches Applied

### Pairip Bypass (from previous session)
| Class | Dex | Patch |
|-------|-----|-------|
| `SignatureCheck` | classes3 | `verifyIntegrity()` → `return-void` |
| `StartupLauncher` | classes2 | `launch()` → `return-void` |
| `VMRunner` | classes11 | `invoke()` → `const/4 v0, 0x0; return-object v0` |
| `Application` | classes5 | `attachBaseContext()` → try-catch wrapper |
| `FirebasePerfUrlConnection` | classes6 | `instrument()` → `return-object p0` |

### New Patches (this session)

| Class | Dex | Patch |
|-------|-----|-------|
| `FirebasePerfOkHttpClient` | classes | `enqueue()` → passthrough, `execute()` → passthrough, `sendNetworkMetric()` → `return-void` |
| `FirebaseConfigUtil` | classes11 | Null check for `FirebasePerformance.getInstance()` |
| `AppCompatActivity` | classes7 | `sget-object tSjy.NlLQ` → `const-string "androidx:appcompat"` |
| 33+ pairip classes | classes,2,3,5,6,7,10,11 | Clinit string injection (1,284+ fields) |
| 926 caller sites | classes,2,3,5,6,7,10,11 | `sget-object pairipField` → `const-string value` |

### Manifest Changes
- `android:name="com.pairip.application.Application"` (preserved)
- `android:extractNativeLibs="true"` 
- `android:usesCleartextTraffic="true"`
- `FirebaseInitProvider` **added back** (was previously removed)
- Split metadata removed

## Technical Approach

### Clinit String Injection
- `generate_clinit_v3.py` reads `hprof_static_fields.json` (1,472 String fields from 41 classes)
- Injects `<clinit>` blocks that pre-populate null String fields with decrypted values
- Applied to 33 classes across 8 recompilable dex files

### Caller Patching (for non-recompilable dex files)
- classes4.dex and classes8.dex exceed 65535 method references (16-bit limit)
- smali cannot reassemble them → `Unsigned short value out of range: 65539`
- Instead: find all `sget-object` references to pairip classes from classes4/8 in recompilable dex files
- Replace `sget-object vX, Lpairip/class;->field:Ljava/lang/String;` with `const-string/jumbo vX, "value"`
- 926 patches across 7 dex files

### Build Pipeline
1. `apktool d -s` → decode manifest only
2. Edit `AndroidManifest.xml` (Application class, extractNativeLibs, FirebaseInitProvider, split metadata)
3. `apktool b` → rebuild with correct manifest
4. Replace all 12 dex files via `zip -0` with patched versions
5. `zipalign` → align APK
6. `apksigner` → sign with debug key

## Known Issues

- FirebasePerformance NPE warnings in logcat (non-fatal, caught by null checks)
- `httpsnull` URL construction from null pairip strings in OkHttp callbacks (non-fatal, logged as warnings)
- classes4.dex and classes8.dex pairip classes don't have clinit patches (workaround: caller patching)
- Some pairip-encrypted strings in classes4/classes8 may still be null if not referenced from recompilable dex files

## Files Created/Modified

- `/tmp/dex-patch/patch_callers.py` — Script to patch callers of pairip class fields
- `/var/home/rabil/Projects/Others/fluffy-patches/scripts/smali-patch/generate_clinit_v3.py` — Clinit injection script (from previous session)
- All smali directories under `/tmp/dex-patch/work/smali_classes*/` with patches applied