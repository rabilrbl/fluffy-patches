# Next Session Handoff

## Current Best Understanding

### Active target
- Work from the **371 split baseline** only
- Package: `com.jio.jioplay.tv`
- Version on tested AVD: `versionName 7.1.7`, `versionCode 371`
- Keep this separate from the older **404 merged / antisplit** research track

### What is now proven
- The **original signed split install** launches and passes pairip verification
- `SignatureCheck: Signature check ok` confirmed in logcat
- **pairipcore loads, verifies, and unloads in < 100ms** — impossible to catch in /proc/maps with polling
- **Frida spawn mode is detected and killed** — even minimal polling scripts with no Interceptor.attach
- frida-server-stealth (configured in previous sessions) is also detected
- **LD_PRELOAD/wrap.sh approach fails** — app is not debuggable, and setprop wrap causes crash
- The VM bytecode files contain security checks: anti-debug, anti-Frida, /proc/self/maps, dex integrity, signature verification

### Pairip VM bytecode analysis (new)
- 22 encrypted bytecode files in assets/ (obfuscated names)
- All share XOR key: `6a852fbd5ac3371c2c6848d6befb97a1...` (via pairipcore-vm tool)
- Security-relevant decoded strings:
  - `android/os/Debug` + `isDebuggerConnected` + `waitingForDebugger`
  - `/proc/self/maps` + `/proc/self/status`
  - `classes.dex` + `.dex` + `lastIndexOf` (dex integrity)
  - `com/pairip/VMRunner`
  - All 4 arch `libpairipcore.so` paths (self-check)
  - `sourceDir` + `base.apk`
  - `java/io/File`, `java/lang/Exception`

### Static analysis of libpairipcore.so (new)
- 475,984 bytes, ELF 64-bit x86-64, dynamically linked, **stripped**
- 3 exported functions: `JNI_OnLoad` (0x64a50, 18KB), `JNI_OnUnload` (0x69240), `ExecuteProgram` (0x6c890)
- Imports: `dl_iterate_phdr`, `dlopen`, `dlsym`, `dlclose`, `syscall` — confirms anti-tampering
- Self-decrypts at runtime — static analysis insufficient

### AVD setup
- Pixel 4 API 30 Play Store x86_64 with root (Magisk 30.6 via rootAVD)
- Start with: `~/Android/Sdk/emulator/emulator -avd Pixel_4_API30_PlayStore -no-snapshot-load -gpu angle`
- JioTV installed as original signed splits (v371)
- PADumper installed

### Frida anti-detection status
- Confirmed: **pairip kills any Frida-attached process immediately**
- This includes spawn mode and stealth frida-server
- Frida-based dump approaches are blocked

## Recommended Next Steps (ordered by viability)

1. **Ghidra deep analysis of static libpairipcore.so**
   - Load the static .so into Ghidra
   - Analyze `JNI_OnLoad` to understand the self-decryption routine
   - Identify the decryption algorithm and key material embedded in the binary
   - Apply the decryption statically to produce the runtime-equivalent binary
   - This is the most viable path since we can't catch it in memory

2. **PADumper GUI approach**
   - PADumper is installed on the device
   - It uses ptrace (not Frida), so it might avoid anti-Frida detection
   - Challenge: pairip's `clone+waitpid+ptrace` anti-debug may conflict
   - Needs manual GUI testing on the AVD

3. **Native ptrace-based dumper binary**
   - Write a C program compiled for x86_64 Android that:
     - Forks
     - Child execs JioTV start
     - Parent ptrace ATTACH immediately
     - Set breakpoint at dlopen return for pairipcore
     - Dump the library when loaded
   - Avoids Frida detection entirely

4. **QEMU/Unicorn emulation of JNI_OnLoad**
   - Extract the decryption routine from Ghidra analysis
   - Emulate JNI_OnLoad in QEMU/Unicorn to produce the decrypted binary
   - Most complex but avoids all runtime protections

5. **Continue building the pairipcore-vm disassembler**
   - Use MatrixEditor/pairipcore-vm to fully decode the VM bytecode files
   - Map which app methods are virtualized
   - This tells us whether the VM must be preserved or can be gutted

## Relevant Docs
- `docs/jiotv/pairip-hash-change-bypass-research.md` — Full research summary
- `docs/jiotv/session-2026-04-30-pairip-dump-attempts.md` — This session's dump attempts
- `docs/jiotv/next-session-handoff.md` — Previous handoff (updated to this file)
- `docs/jiotv/pairip-drm.md` — Original pairip analysis
- `docs/jiotv/external-pairip-research.md` — External research sources

## Scripts
- `/tmp/hook_pairip_v3.js` — Frida polling script (detected, doesn't work)
- `/tmp/catch_pairipcore.sh` — On-device bash polling script (too slow)
- `/tmp/pairip-dump/vm-bytecode/assets/` — Extracted VM bytecode files
- `/tmp/pairip-dump/x86_64_static/lib/x86_64/libpairipcore.so` — Static (encrypted) .so
- `pairip` CLI tool installed (from MatrixEditor/pairipcore-vm)