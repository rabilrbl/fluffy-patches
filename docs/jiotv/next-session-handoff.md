# Next Session Handoff — JioTV Pairip Bypass

## Quick Status
**v17 APK** crashes at `tSjy.NlLQ = null` in SavedStateRegistry — needs all 52 pairip-encrypted String fields populated. We captured a heap dump of the running original app but can't parse the CLASS_DUMP records yet.

## What's Working
- ✅ Heap dump captured (`/tmp/pairip-dump/jiotv-heap.hprof`, 53MB)
- ✅ HPROF Pass 1: 108K strings, 24K classes, 38/52 target classes identified
- ✅ kEpRxMC: 48 field values already dumped via Frida (`/tmp/pairip-dump/kEpRxMC_dump.json`)
- ✅ pairipcore-vm Rust CLI: 2,390+ raw VM strings extracted (no field mapping)
- ✅ pairipcore-vm Python package installed (`~/Projects/Others/pairipcore-vm/.venv/`)

## Critical Blocker
**HPROF Pass 2 parser fails** — ART uses non-standard sub-tags (`0x8B`, `0x8D`, `0xFE`) that our parser doesn't handle, causing complete misalignment. Zero CLASS_DUMP/INSTANCE_DUMP records found.

## How to Fix the HPROF Parser
The sub-tag format for `0x8B` (HEAP_DUMP_INFO) is:
- Tag: `0x8B` (1 byte)
- Heap type: `u1` (0=DEFAULT, 1=APP, 2=IMAGE, 3=ZYgote)
- Heap name string ID: `ID` (4 bytes for id_size=4)

The sub-tag `0x8D` is unknown but likely follows a similar format. The parser needs to skip these properly to reach the actual CLASS_DUMP (0x20) and INSTANCE_DUMP (0x21) records.

## Alternative Approaches (in order of feasibility)
1. **app_process Java runtime** — Start a bare DalvikVM with JioTV's classpath, let pairip VM populate fields, then reflectively dump. No signature issues since it's a standalone process.
2. **Re-sign + inject ContentProvider** — Modify original APK: add `android:debuggable=true`, inject a ContentProvider that dumps strings, re-sign with debug key. Pairip will fail signature check but we can patch SignatureCheck.smali first.
3. **pairipcore-vm Python decompiler** — Needs `opcodes.json` file. The `gvm_strings.py` tool requires it. Need to generate or find this file to use the decompiler for field→value mapping.

## Key Paths
- Smali source (v17): `/tmp/pairip-dump/jiotv-smali-clean/`
- Heap dump: `/tmp/pairip-dump/jiotv-heap.hprof`
- Target classes: `/tmp/pairip-dump/classes_to_dump.json`
- kEpRxMC values: `/tmp/pairip-dump/kEpRxMC_dump.json`
- VM bytecode: `/tmp/pairip-dump/vm-bytecode/assets/`
- VM strings: `/tmp/pairip-dump/vm-strings/`
- Build tools: `/home/rabil/Android/Sdk/build-tools/34.0.0/`
- Debug keystore: `~/.android/debug.keystore`
- pairipcore-vm: `~/Projects/Others/pairipcore-vm/`
- AVD: Pixel 4 API 30 Play Store, x86_64, rooted (Magisk 30.6), `ro.debuggable=1`

## AVD Notes
- JioTV v371 (all 6 splits) installed and running on AVD
- SELinux: permissive
- frida-server: killed/not running
- `wrap.com.jio.jioplay.tv` property: cleared
- JDWP: blocked by app's `android:debuggable=false`

## Build v18 Once Strings Are Extracted
1. For each of 52 target classes, generate `<clinit>` with `const-string`/`sput-object` pairs
2. Replace null-string smali fields with hardcoded values
3. Rebuild APK: `apktool b`, `zipalign`, `apksigner` with debug key
4. Install all 6 splits via `adb install-multiple`
5. Test on AVD with `-gpu angle`