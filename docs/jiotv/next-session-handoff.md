# Next Session Handoff

## Current Best Understanding

### Active target
- Work from the **371 split baseline** only
- Package: `com.jio.jioplay.tv`
- Version on tested AVD: `versionName 7.1.7`, `versionCode 371`
- Keep this separate from the older **404 merged / antisplit** research track

### What is already proven
- The **original signed split install** launches on the x86_64 Play Store AVD
- Re-signing the split set is enough to trigger the native pairip crash
- Therefore the Morphe APK-modification route is blocked by signature-sensitive native checks

### Root / runtime state
- `Pixel_4_API30_PlayStore` has Magisk root available via `/debug_ramdisk/su`
- `adb root` still fails, which is expected on this production build
- `frida-server` can be started successfully as root with:
  ```bash
  adb shell '/debug_ramdisk/su 0 -c "nohup /data/local/tmp/frida-server >/data/local/tmp/frida-server.log 2>&1 &"'
  ```
- Frida transport on `tcp:27042` works

### Anti-Frida result
- Plain Frida attach to the **main JioTV process** is enough to kill the app
- No Java hook is required to reproduce the crash
- Crash signature:
  - `SIGSEGV SI_TKILL`
  - `split_config.x86_64.apk!libpairipcore.so`

### Strong native clues
- `libpairipcore.so` imports and uses low-level process-control APIs including `syscall`, `dl_iterate_phdr`, `dlopen`, `dlsym`, `stat`, `strcmp`, `getpid`
- Disassembly shows raw syscall patterns consistent with anti-debug flow, including `ptrace`, `wait4`, `exit`, `getppid`, `clone`, `arch_prctl`
- Tombstone showed `/proc/<pid>/maps` open in the crashing process
- Control test showed Frida leaves obvious `/memfd:frida-agent-64.so (deleted)` entries in process maps
- Low-effort stealth attempts did not remove that signature

## Recommended Resume Order
1. Assume **plain Frida attach is detectable** and stop spending time on shallow flag tweaks
2. Choose one of these paths:
   - **deeper custom / stealth Frida build** targeting the actual injected agent blob
   - **native bypass** against pairip’s maps / anti-debug logic in `libpairipcore.so`
3. Keep all notes under `docs/jiotv/` and continue pushing to `dev`

## Relevant Docs
- `docs/jiotv/targets.md`
- `docs/jiotv/session-2026-04-08-latest-uptodown-xapk.md`
- `docs/jiotv/session-2026-04-08-frida-route.md`

## Relevant Scripts
- `scripts/jiotv-371-original-frida-route.sh`
- `scripts/jiotv-371-original-frida.js`
