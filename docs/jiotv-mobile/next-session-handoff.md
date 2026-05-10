# Next Session Handoff — JioTV Mobile Pairip Bypass

## Quick Status
**v29 APK built (NOT TESTED)**. Merged XAPK build with all split resources, pairip VM fully neutralized, string class fields defaulting to empty strings. Firebase NPE cascade addressed via `<clinit>` patches on pkOPEgq and kEpRxMC.

## What's Working
- ✅ Merged XAPK build (v23+) — all split resources included, no missing drawables
- ✅ libpairipcore.so removed, VMRunner.<clinit> returns void, StartupLauncher.launch() returns void
- ✅ LicenseClientV3 fully bypassed (initializeLicenseCheck, processResponse, showPaywall, showErrorDialog → return-void)
- ✅ SSL pinning bypassed (APIManager + JioPlayer cert pin skip)
- ✅ FirebaseInitProvider restored with exception handler
- ✅ pkOPEgq + kEpRxMC pairip string classes have `<clinit>` assigning empty strings to all 96 fields
- ✅ SystemPropsKt.systemProp() null guard prevents NPE from null pairip keys
- ✅ Build pipeline: APKEditor merge → apktool decode/rebuild → zipalign → apksigner

## Key Files
| Item | Path |
|------|------|
| Merged APK source | `~/Downloads/jiotv-7-1-7.xapk` |
| apktool work dir | `/tmp/dex-patch/apktool-work/` |
| Build tools | `/home/rabil/Android/build-tools/34.0.0/` (zipalign, apksigner) |
| Debug keystore | `/tmp/dex-patch/debug.keystore` (alias: `androiddebugkey`) |
| AVD | Pixel 4 API 30, x86_64, Android 11, rooted (Magisk 30.6) |

## Next Steps (Priority Order)

### 1. Test v29 on Device
- Install v29 APK on AVD or real device
- Verify no NPE crashes from pairip string classes
- Test content playback (channels, VOD)

### 2. Content Playback Testing
- App needs real network to test channel playback
- Watch for: SSL pinning issues, auth failures, token refresh
- Empty string defaults for pairip fields may cause API auth issues — watch for 401/403

### 3. Empty String vs Real Values
- pkOPEgq and kEpRxMC fields default to `""` instead of real pairip-decrypted values
- Some fields may need actual values for API calls to work (auth tokens, URLs)
- If content playback fails with auth errors, need to extract real values from heap dump

### 4. Emulator/Root Detection
- Current build may trigger emulator detection on some paths
- See [Emulator & Root Detection](research/emulator-root-detection.md)

### 5. Convert to Morphe Patches
- Manual smali patches need converting to Morphe bytecodePatch format
- Several patches already written: BypassLicenseCheckPatch, ForceInitPairipStringsPatch, RemoveCertificatePinningPatch, BypassPairipVMPatch

## Known Pitfalls
- **NEVER use `re.sub()` for binary smali patching** — control chars get corrupted. Use byte-level slicing.
- **ALWAYS use `const-string/jumbo`** — all dex files have >65K string indices
- **Don't recompile classes5.dex** — it's exactly at 65,536 methods; adding ANY code overflows
- **ExceptionInInitializerError is unrecoverable** — class is permanently broken after clinit failure, try-catch doesn't help
- **apktool caches dex** — clear build directory if patches don't appear in rebuilt APK
- **Split APK resources must be merged before patching** — base.apk alone is incomplete

## Session History (Most Recent First)
- [v23–v29 Merged APK + Pairip Strings](sessions/session-2026-05-10-v23-v29-merged-pairip-strings.md) — Merged XAPK, VMRunner bypass, string class clinit patches
- [v23 Merged APK](sessions/session-2026-05-10-v23-merged-apk.md) — Merged XAPK build with all split resources
- [v4 Working App](sessions/session-2026-05-09-v4-working-app.md) — Working v18j with clinit v3 + Firebase NPE fixes
- [v3 License + SSL Bypass](sessions/session-2026-05-09-v3-license-ssl-bypass.md) — License check + SSL pinning bypass
- [v2 Clinit Caller Patching](sessions/session-2026-05-09-v2-clinit-caller-patching.md) — Clinit caller patching approach
- [Direct DEX Patching](sessions/session-2026-05-09-direct-dex-patching.md) — Direct DEX patching approach