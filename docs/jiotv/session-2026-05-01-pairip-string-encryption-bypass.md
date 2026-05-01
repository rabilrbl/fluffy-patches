# Session 2026-05-01: Pairip R8 String Encryption Bypass

## Objective
Patch JioTV APK (com.jio.jioplay.tv v371) to bypass pairipcore DRM so a re-signed APK can run without crashing.

## Root Cause
Pairip's R8 string encryption populates **1,464 static String fields across 38 fully-encrypted classes** at runtime via a native VM. Without the VM running (which requires original APK signature), these fields remain null, causing cascade NPE crashes.

## Breakdown of All Static String Fields

| Category | Classes | Fields | Description |
|----------|---------|--------|-------------|
| Fully pairip-encrypted (no initial values, no `<clinit>`) | 38 | 1,464 | Need runtime dump to populate |
| Partially encrypted (some values, some null) | 18 | ? | Need partial dump |
| Normal (all values present) | 42 | ? | No patching needed |

### Top Fully-Encrypted Classes (by field count)

1. `com.google.android.material.math.cZm.eoLd` — 64 fields
2. `com.iab.omid.library.ril.HNCT.pkOPEgq` — 55 fields
3. `com.google.android.material.transformation.ZR.qPsNnl` — 53 fields
4. `com.fasterxml.jackson.databind.dr.QtgAP` — 52 fields
5. `androidx.compose.runtime...persistentOrderedSet.di.lLtqWF` — 52 fields
6. `com.google.android.gms.measurement.internal.ESeC.lFxRbOk` — 51 fields
7. `org.junit.UIsi.uELbXzHAqQ` — 51 fields
8. `com.bumptech.glide.load.engine.executor.Pfy.bGAxucPBz` — 51 fields
9. `androidx.media.utils.sQZv.zruNGAQoDK` — 51 fields
10. `com.google.ads.mediation.admob.yUG.OfMpQvBFWns` — 50 fields

## Build History

### v14 (Platinmods approach)
- `android:name="com.jio.jioplay.tv.JioTVApplication"` in manifest
- Removed pairip from CoreComponentFactory
- MultiDexApplication as parent class
- **Result**: Crashes from Firebase/other null string classes (not just kEpRxMC)

### v15
- Disabled `FirebaseInitProvider` (`android:enabled="false"`)
- **Result**: WORSE — "Default FirebaseApp is not initialized" error + null string crashes

### v16
- Re-enabled FirebaseInitProvider
- Patched `FirebaseInitProvider.onCreate()` to catch `ExceptionInInitializerError`
- **Result**: Still crashes — `RemoteConfigManager.<clinit>` NPE

### v17 (Best so far)
- FirebasePerfRegistrar.getComponents() returns empty list
- FirebasePerfUrlConnection all methods are identity pass-throughs
- Removed Crashlytics/Analytics registrars from ComponentDiscoveryService
- **Result**: Crashes at `AppCompatActivity.<init>` → `SavedStateRegistry.registerSavedStateProvider(key=null)` because `tSjy.NlLQ` is null (another R8-encrypted class, 45 null fields)

## Key Finding: kEpRxMC is Just the Tip of the Iceberg

We initially hard-coded kEpRxMC's 48 fields via Frida dump. But there are **37 more classes** just like it, totaling **1,464 fields**. Patching them one at a time won't scale.

## Approach: Android Instrumentation APK

Built `StringDumperInstrumentation.apk` — an Android instrumentation test APK that:
- Declares `<instrumentation android:targetPackage="com.jio.jioplay.tv">`
- Runs inside JioTV's process after pairip VM populates all strings
- Uses Java reflection to read all static String fields from target classes
- Writes JSON dump to `/data/local/tmp/string_dump.json`

### Why Instrumentation?
- Shares the same process as JioTV → same classloader and heap
- Starts after pairip VM completes → all strings populated
- Pairip can't detect it (framework feature, not Frida/jdwp)
- Separate package — no code modification to JioTV

### Source Code
`/tmp/pairip-dump/string-dumper/src/com/rabil/stringdumper/StringDumperInstrumentation.java`

### Current Target Classes (30 classes hardcoded)
```
com.jio.jioplay.tv.fragments.composable.model.TA.tSjy
org.apache.commons.net.ftp.Rso.kEpRxMC
com.google.common.net.HttpHeaders
com.jio.jioplay.tv.storage.SharedPreferenceUtils
com.jio.jioplay.tv.constants.AppConstants$Headers
com.bumptech.glide.load.engine.executor.Pfy.bGAxucPBz
io.reactivex.rxjava3.schedulers.FNMj.amHSzzOvxfR
io.reactivex.rxjava3.internal.queue.je.QoTnrJIdjs
io.reactivex.rxjava3.annotations.SchedulerSupport
com.google.firebase.crashlytics.internal.settings.SettingsController
com.google.firebase.messaging.ServiceStarter
com.google.android.gms.measurement.internal.ESeC.lFxRbOk
com.google.android.gms.location.FusedLocationProviderApi
com.google.android.gms.measurement.AppMeasurement
com.jio.jioplay.tv.views.drag.AppTourOverlayView$AppTourOverlayConstant
com.fasterxml.jackson.databind.dr.QtgAP
at8, zo7, zo6, qz4, jp, dp7, zs8, s47
plus various com.jio, com.coremedia, com.jiosaavn classes
```

**ISSUE**: Only 30 classes are hardcoded — need to expand to all 38 fully-encrypted + 18 partially-encrypted classes for complete dump.

### Build Status
- Built and signed: `/tmp/pairip-dump/string-dumper/StringDumper-aligned.apk`
- Installed on AVD: ✅
- **NOT YET RUN** — needs to be executed with `am instrument`

## Alternative: VM Bytecode Offline Extraction

Using `pairipcore-vm` (Rust CLI by MatrixEditor):
```bash
pairip strings -a /tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE
```

Extracted **2,390 strings** with offsets from the main VM program `fS2NPonHRPamHuOE` (152KB). This contains:
- Field names (e.g., `NlLQ`, `aKPJrxNCR`, `kEpRxMC`)
- Class names (e.g., `com/jio/.../tSjy`)
- String values (e.g., `kotlinx.coroutines.scheduler.default.name`)

**However**: The VM doesn't store field→value as adjacent pairs. The mapping requires understanding the VM's instruction format (sput-object sequences), which the Rust CLI doesn't provide.

The Python package in that repo has a proper decompiler but requires Python 3.12+ (system has 3.11).

## Previously Failed Approaches

| Approach | Result | Why |
|----------|--------|-----|
| LD_PRELOAD shim (v2-v4) | SIGSEGV at `rip=0x0` | Android linker loads shim before JVM init, NULL function pointer |
| PTRACE-based dumper | Library gone before attachment | pairip loads/verifies/unloads in <100ms |
| Frida spawn hooks | Process killed in ~2s | pairip's anti-Frida detection |
| JDWP debugging | Handshake fails | App not debuggable (`android:debuggable` not set) |
| Fake libpairipcore.so | Works for native layer | Doesn't help with R8-encrypted Java strings |

## What Works

- Fake `libpairipcore.so` (x86_64) — stubs JNI_OnLoad, executeVM, getVersion, nativeSetup
- `SignatureCheck.smali` patches — verifyIntegrity() returns void, verifySignatureMatches() returns true
- `StartupLauncher.smali` patches — launch() methods are no-ops
- `FirebasePerfRegistrar` neutering
- `FirebasePerfUrlConnection` identity pass-throughs
- kEpRxMC `<clinit>` hardcodes (48 fields from Frida dump)
- Manifest: `android:name="com.jio.jioplay.tv.JioTVApplication"`, `extractNativeLibs=true`

## Current Smali State
- Location: `/tmp/pairip-dump/jiotv-smali-clean/`
- 10 smali class directories
- Base + 4 density splits
- All APKs re-signed with debug key

## Key Files
- `/tmp/pairip-dump/string-dumper/StringDumper-aligned.apk` — Instrumentation APK
- `/tmp/pairip-dump/kEpRxMC_dump.json` — 48 known field values
- `/tmp/pairip-dump/vm-strings/fS2NPonHRPamHuOE_addr.txt` — 2,390 VM strings with offsets
- `/tmp/pairip-dump/jiotv-smali-clean/` — v17 smali source
- `/tmp/pairip-dump/null_classes_v2.json` — Empty (scan needs re-run with broader regex)

## Next Steps

1. **Expand instrumentation target list** to include all 38 fully-encrypted + 18 partially-encrypted classes
2. **Run instrumentation**: `adb shell am instrument com.rabil.stringdumper/com.rabil.stringdumper.StringDumperInstrumentation`
3. **Capture dump**: `adb shell cat /data/local/tmp/string_dump.json`
4. **Write script** to generate `<clinit>` methods for all 56 classes with the dumped values
5. **Build v18** with all hardcoded string classes
6. **Test** until app launches clean

### Fallback if instrumentation fails
- Build `pairipcore-vm` Python tools with Python 3.12+ (pyenv or container) to decompile VM bytecode
- Manually map VM instruction sequences to field assignments
- Try LSPosed + Shamiko on AVD to hide Frida from pairip