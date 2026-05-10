# Session 2026-05-01: HPROF Heap Dump String Extraction

## Objective
Extract runtime String field values from JioTV's pairip-encrypted classes using a heap dump of the running (unmodified) app, so we can hardcode them into our patched APK.

## Key Findings

### 1. Heap Dump Successfully Captured
- Command: `adb shell su -c "am dumpheap com.jio.jioplay.tv /data/local/tmp/jiotv-heap.hprof"`
- File: `/tmp/pairip-dump/jiotv-heap.hprof` (53MB)
- JioTV was running unmodified with pairip VM active (all fields populated)

### 2. HPROF Pass 1 — Metadata Extraction Works
- **108,081 STRING records** extracted successfully
- **24,607 LOAD_CLASS records** extracted successfully
- **`java.lang.String`** class found at obj ID `0x706f5d58`
- **38 of 52 target classes** found in heap:
  - `com.google.android.material.transformation.ZR.qPsNnl` → `0x131ffc88`
  - `com.clevertap.android.sdk.CleverTapAPI` → `0x13207d98`
  - `androidx.room.rxjava2.Nm.jVoQuJkVbZ` → `0x13209450`
  - `com.jio.jioplay.tv.fragments.composable.model.TA.tSjy` → `0x1320df88`
  - `com.google.logging.type.Bv.qCWmO` → `0x1320f3a8`
  - `com.iab.omid.library.ril.HNCT.pkOPEgq` → `0x13221940`
  - `com.jio.jioplay.tv.JioTVApplication` → `0x132390b0`
  - `com.google.ads.mediation.admob.yUG.OfMpQvBFWns` → `0x13245960`
  - `com.jio.media.games.Ijg.QPqo` → `0x13246048`
  - `androidx.compose.ui.graphics.colorspace.UMRU.eSKGWZRpiB` → `0x13281bb8`
  - `com.google.firebase.crashlytics.internal.metadata.bjTJ.iSnnBCOyG` → `0x132ad5b0`
  - `com.google.android.gms.common.data.Gtgm.NSkdPWhhvdvp` → `0x132ad750`
  - `com.clevertap.android.sdk.ManifestInfo` → `0x132b7c50`
  - `io.reactivex.rxjava3.schedulers.FNMj.amHSzzOvxfR` → `0x132eba60`
  - `androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedSet.di.lLtqWF` → `0x132f18b8`
  - `kotlin.jvm.jdk8.bdd.TauBg` → `0x13308ce0`
  - `org.junit.UIsi.uELbXzHAqQ` → `0x1331e2b8`
  - `androidx.media.utils.sQZv.zruNGAQoDK` → `0x133267d0`
  - `com.jio.jioadstracker.JioAdsTracker` → `0x13326980`
  - `com.jio.jioplay.tv.cinemaanalytics.Utils` → `0x1332ec98`
  - `com.moat.analytics.mobile.rel.a.a.nMS.DFbi` → `0x133309f0`
  - `androidx.compose.material.icons.automirrored.filled.UkXe.cXzwoAXFFJAAAW` → `0x13352a28`
  - `com.fasterxml.jackson.databind.annotation.yY.sFNmv` → `0x146f53c8`
  - `androidx.core.view.contentcapture.xGpb.aRVZq` → `0x146f9468`
  - `com.google.android.gms.measurement.internal.ESeC.LqHXeHhnteE` → `0x146fe558`
  - `androidx.compose.material.icons.automirrored.rounded.sOdI.AwdduSDrAVMPh` → `0x146ff228`
  - `com.google.android.gms.internal.auth.fhH.AHenrzPoLpx` → `0x14707a08`
  - `com.jio.media.tokensdk.TokenController` → `0x14712a80`
  - `io.reactivex.rxjava3.internal.queue.je.QoTnrJIdjs` → `0x147158c0`
  - `com.fasterxml.jackson.databind.dr.QtgAP` → `0x14717d18`
  - `com.jio.jioads.videomodule.rgIe.tGFb` → `0x1471b0f0`
  - `org.apache.commons.net.ftp.Rso.kEpRxMC` → `0x1471ca88`
  - `androidx.localbroadcastmanager.content.bjLF.apLPWUHMfmS` → `0x1471ef50`
  - `com.google.android.gms.measurement.internal.ESeC.lFxRbOk` → `0x14720b70`
  - `androidx.compose.runtime.tooling.hqq.knUyyiHJfb` → `0x14725bf8`
  - `com.google.android.material.math.cZm.eoLd` → `0x1472d768`
  - `com.bumptech.glide.load.engine.executor.Pfy.bGAxucPBz` → `0x14730a28`
  - `org.joda.time.format.RJ.ryseAuCLZGtT` → `0x14736b18`
- **14 target classes NOT in heap** (not yet loaded at dump time or GC'd)

### 3. HPROF Pass 2 — HEAP Data Parsing FAILED
- **Root cause**: ART's HPROF format uses non-standard sub-tags:
  - `0x8D` = HEAP_DUMP_INFO (u1 heap_type + ID heap_name_string_id) — separates heap segments
  - `0xFE` = unknown (possibly ART-specific internal marker)
- Our parser only knew about standard HPROF sub-tags (0x01-0x08, 0x20-0x23), so encountering `0x8D` caused it to bail after the first occurrence, finding **0 CLASS_DUMP, 0 INSTANCE_DUMP, 0 PRIM_ARRAY_DUMP** records
- The HPROF spec from Android adds: `0x8B` = HEAP_DUMP_INFO segment markers between data chunks

### 4. HPROF Format Details (ART/Android 11)
- Identifier size: 4 bytes (u4)
- Class names use **dot notation** (e.g., `com.jio.jioplay.tv.JioTVApplication`), not slash notation
- String instances in the heap use 20-byte instance data (includes Object header fields)
- Sub-record types:
  - `0x01`-`0x08`: ROOT records (JNI_GLOBAL, JNI_LOCAL, etc.)
  - `0x20`: CLASS_DUMP
  - `0x21`: INSTANCE_DUMP
  - `0x22`: OBJ_ARRAY_DUMP
  - `0x23`: PRIM_ARRAY_DUMP
  - **`0x8B`**: ART HEAP_DUMP_INFO (non-standard)
  - **`0x8D`**: Another ART-specific sub-record type
  - **`0xFE`**: Unknown ART marker

### 5. JDWP Approach — FAILED
- `ro.debuggable=1` on AVD, but JDWP handshake returns empty response
- App manifest has `android:debuggable=false` which takes precedence
- Error: `java.io.IOException: handshake failed - connection prematurely closed`

### 6. Pairipcore-vm Python Package
- Installed via `uv` in `~/Projects/Others/pairipcore-vm/.venv/`
- `VM`, `VMContext`, `Insn`, `InsnFormat` classes all working
- `decode_opcode_v0/v1`, `decode_entry_point_v0/v1` functions available
- **Blocker**: The `gvm_strings.py` tool requires an `opcodes.json` definition file that maps opcode numbers to format IDs and metadata — we don't have this file configured
- The Rust CLI `pairip strings` works and already extracted 2,390+ strings (but without field→class mapping)

### 7. VM Bytecode String Data
- Main startup program: `fS2NPonHRPamHuOE` (152KB)
- 21 additional VM bytecode files in `/tmp/pairip-dump/vm-bytecode/assets/`
- Rust CLI extracted 2,390+ raw strings with offsets to `/tmp/pairip-dump/vm-strings/fS2NPonHRPamHuOE_addr.txt`
- Key strings found: API keys, tokens, encryption keys, class references, method descriptors
- **Missing**: mapping from strings → class.field assignments (which string goes to which field)

## Blocked Approaches (Documented)

| Approach | Status | Blocker |
|----------|--------|---------|
| LD_PRELOAD/wrap.sh | FAILED | SIGSEGV during JVM init, NULL function pointer |
| ptrace-based dump | FAILED | pairip loads/unloads in <100ms |
| Frida hooks | FAILED | Anti-Frida kills process in ~2s |
| JDWP attach | FAILED | App not debuggable, handshake fails |
| Instrumentation APK | FAILED | Android requires same signing key |
| Instrumentation (profiling) | FAILED | PERMISSION_DENIED |
| HPROF Pass 2 | FAILED | ART non-standard sub-tags break parser |

## Working / Promising Approaches

| Approach | Status | Notes |
|----------|--------|-------|
| HPROF heap dump | Pass 1 works | Need to fix ART sub-tag parsing for Pass 2 |
| app_process Java runtime | UNTRIED | Load JioTV's DEX, trigger pairip VM, reflectively dump |
| pairipcore-vm decompiler | PARTIAL | Rust CLI works for strings; Python decompiler needs opcodes.json |
| Re-sign + inject ContentProvider | UNTRIED | Sign original APK with debug key, inject dump code directly |

## Files
- `/tmp/pairip-dump/jiotv-heap.hprof` — 53MB heap dump of running JioTV
- `/tmp/pairip-dump/classes_to_dump.json` — 52 target classes with field names
- `/tmp/pairip-dump/kEpRxMC_dump.json` — 48 field values for kEpRxMC (from previous Frida dump)
- `/tmp/pairip-dump/vm-strings/fS2NPonHRPamHuOE_addr.txt` — 2,390+ raw VM strings
- `/tmp/pairip-dump/hprof_strings.json` — 108,081 HPROF STRING records
- `/tmp/pairip-dump/hprof_target_ids.json` — String class ID + 38 target class IDs
- `/tmp/pairip-dump/hprof_class_dumps_parser.py` — Broken HPROF parser (needs ART sub-tag fix)
- `/tmp/pairip-dump/extract_static_fields.py` — Second attempt at HPROF parser (same bug)
- `~/Projects/Others/pairipcore-vm/` — Python + Rust pairipcore-vm repo (venv at `.venv/`)

## Next Steps
1. **Fix HPROF parser** to handle ART's `0x8B`/`0x8D` sub-records — this is the most direct path to extracting all 38 target classes' static String field values
2. **Alternative**: Use `app_process` to start a Java runtime with JioTV's classpath, trigger pairip VM, dump strings via reflection
3. **Alternative**: Re-sign original APK with debug key, inject a `ContentProvider` or `BroadcastReceiver` that dumps strings after pairip VM populates them
4. Once string values are extracted → generate `<clinit>` blocks → build v18 → test