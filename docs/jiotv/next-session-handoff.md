# Next Session Handoff — JioTV Pairip Bypass

## Quick Status
**v18j APK launches and runs** on AVD. Pairip signature/integrity checks fully bypassed via heap-dump + clinit injection + libpairipcore stub. All Firebase NPE crashes fixed.

## What's Working
- ✅ App launches, shows AppLogo activity, no FATAL crashes
- ✅ Pairip VM check bypassed (stub `libpairipcore.so` with null JNI_OnLoad / ExecuteProgram / VMRunner)
- ✅ 1,472/1,502 String fields hardcoded via `<clinit>` patches (38/52 target classes resolved from heap)
- ✅ FirebaseCrashlytics NPE fixed (try-catch in `v2.onComplete`)
- ✅ FirebasePerformance NPE fixed (try-catch around `FirebaseConfigUtil.a()`)
- ✅ Smali patching pipeline (`generate_clinit_v3.py`) working (bytes-based, no regex, `const-string/jumbo`)
- ✅ Build pipeline: `apktool b` → `zipalign` → `apksigner` → `adb install-multiple`

## Key Files
| Item | Path |
|------|------|
| Smali source (patched) | `/tmp/pairip-dump/jiotv-smali-clean/` |
| Fresh smali backup | `/tmp/pairip-dump/jiotv-smali-fresh/` |
| Latest APK (v18j) | `/tmp/pairip-dump/jiotv-patched-v18j-aligned.apk` |
| clinit generator (v3) | `scripts/smali-patch/generate_clinit_v3.py` |
| Heap dump data | `/tmp/pairip-dump/hprof_static_fields.json` (1,472 resolved String fields) |
| Debug keystore | `/tmp/pairip-dump/debug.keystore` (alias: `androiddebugkey`) |
| libpairipcore stub | `/tmp/pairip-dump/libpairipcore-stub.so` |
| JioTV v371 splits (original) | `tmp/jiotv-splits/` |
| Build tools | `/home/rabil/Android/build-tools/34.0.0/` (zipalign, apksigner) |
| NDK | `/home/rabil/Android/ndk/27.0.12077973/` |
| Python venv (pairipcore-vm) | `~/Projects/Others/pairipcore-vm/.venv` |

## AVD State
- Pixel 4 API 30, x86_64, Android 11, rooted (Magisk 30.6)
- SELinux: permissive (`setenforce 0`)
- Launch: `emulator @Pixel_4_API_30 -gpu angle`
- frida-server: NOT running (killed for LD_PRELOAD approach, not needed now)
- `wrap.com.jio.jioplay.tv` property: CLEARED (do not re-set)

## Next Steps (Priority Order)

### 1. Content Playback Testing
- App launches but needs real network to test channel playback
- May need to handle additional crashes when hitting actual API calls
- Watch for: SSL pinning issues (see `ssl-pinning.md`), auth failures, token refresh

### 2. Resolve 14 Missing Classes / 30 String Fields
- 14 pairip-encrypted classes weren't in the heap dump (never instantiated during dump window)
- Options:
  - Do a second heap dump after navigating through more of the app
  - Try `app_process` Java runtime to force-instantiate these classes
  - Use pairipcore-vm decompiler (needs `opcodes.json`) to extract values
- Missing classes likely related to less-common code paths (settings, premium features, etc.)

### 3. Emulator/Root Detection Bypass
- Current build may trigger emulator detection on some paths
- See `emulator-root-detection.md` for known detection vectors
- May need smali patches for `Build.FINGERPRINT`, `Build.MODEL`, `Build.BRAND` checks

### 4. SSL Pinning Bypass
- Network calls likely use certificate pinning
- See `ssl-pinning.md` for analysis
- May need OkHttp `CertificatePinner` bypass and Firebase-controlled pinning disable

### 5. Real Device Testing
- Test on ARM64 device to verify the x86_64-specific stub works on ARM
- May need ARM64 version of `libpairipcore-stub.so`

### 6. Clean Up & Harden
- Rebuild `classes10.dex` from smali instead of using cached original
- Verify all 10 dex files compile cleanly
- Consider making `generate_clinit_v3.py` a reusable skill

## Known Pitfalls
- **NEVER use `re.sub()` for binary smali patching** — control chars like `\r`, `\t` get corrupted. Use byte-level slicing.
- **ALWAYS use `const-string/jumbo`** — all dex files have >65K string indices
- **Don't recompile classes5.dex** — it's exactly at 65,536 methods; adding ANY code overflows
- **`FirebaseCrashlytics.getInstance()` THROWS, not returns null** — always use try-catch, never if-eqz
- **apktool caches dex** — if you patch smali but the build doesn't reflect it, clear the build directory or delete the cached dex

## Architecture Quick Ref
- Pairip verification flow: `JNI_OnLoad` → `ExecuteProgram` (runs VM bytecode) → `SignatureCheck.verifySignature()` → `dlclose` (library unloads in <100ms)
- Our bypass: stub `libpairipcore.so` (all exports → return null/true) + hardcoded String field values in `<clinit>` blocks + try-catch for Firebase NPEs
- The VM bytecode (22 `.bin` files in `assets/`) decrypts field values at runtime — we replaced this with static field assignment

## Session History (Most Recent First)
- `session-2026-05-01-clinit-v3-crashlytics.md` — v3 script, build fixes, Firebase NPE fixes → **WORKING**
- `session-2026-05-01b-hprof-heap-string-extraction.md` — HPROF parser fix, 1,472 strings extracted
- `session-2026-05-01-pairip-string-encryption-bypass.md` — clinit injection strategy
- `session-2026-04-30-pairip-dump-attempts.md` — Failed runtime dump approaches
- `session-2026-04-30-v2-smali-patching.md` — Early smali patching attempts