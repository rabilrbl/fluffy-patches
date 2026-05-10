# Session 2026-04-30: Pairip Bypass Research — Runtime Dump Attempts

## Goal
Dump the decrypted `libpairipcore.so` from a running JioTV process to analyze and patch its integrity checks.

## Setup
- AVD: Pixel 4 API 30 Play Store (x86_64), Android 11
- Root: Magisk 30.6 via rootAVD
- JioTV: v7.1.7 (versionCode 371), original signed split install
- Tools installed: PADumper v1.1.0, frida-server-stealth 17.9.1

## Key Findings

### 1. Pairipcore loads and unloads extremely fast
On the original signed APK, `SignatureCheck: Signature check ok` passes, then the library
is immediately unmapped. The entire load→verify→unload cycle takes < 100ms. No amount of
/proc/pid/mem polling (on-device, 50ms intervals, 200 attempts) could catch it in maps.

### 2. Frida is detected even in spawn mode
- `frida -f com.jio.jioplay.tv` with spawn mode: process terminates immediately
- The polling script v3 (no Interceptor.attach, just setTimeout polling) also failed
- Process killed before any module enumeration could find pairipcore
- This confirms pairip's anti-Frida detection runs in the native static initializer BEFORE
  Frida's instrumentation can take effect
- frida-server-stealth (from previous sessions) also detected

### 3. Pairip VM bytecode analysis (pairipcore-vm tool)
- 22 encrypted bytecode files in assets/ (obfuscated names like `3i7oiYrqjs8dQOyr`)
- All share the same XOR key: `6a852fbd5ac3371c2c6848d6befb97a1...`
- One file (`ACOBk6WueMRYb6n5`) could not find its XOR key
- Security-relevant strings found in decoded bytecode:
  - `android/os/Debug` + `isDebuggerConnected` + `waitingForDebugger`
  - `/proc/self/maps` + `/proc/self/status`
  - `classes.dex` + `.dex` + `lastIndexOf` (dex integrity checks)
  - `com/pairip/VMRunner`
  - `lib/arm64-v8a/libpairipcore.so` + all 4 arch variants (self-verification)
  - `sourceDir` + `base.apk` (APK path integrity)
  - `java/io/File` (file existence checks)
  - `java/lang/Exception` (error handling)

### 4. Static analysis of libpairipcore.so
- File: ELF 64-bit LSB shared object, x86-64, dynamically linked, **stripped**
- Size: 475,984 bytes
- 3 exported functions:
  - `JNI_OnLoad` at offset 0x64a50 (18,401 bytes)
  - `JNI_OnUnload` at offset 0x69240 (1,147 bytes)
  - `ExecuteProgram` at offset 0x6c890 (1,947 bytes)
- Imports confirm anti-tampering: `dl_iterate_phdr`, `dlopen`, `dlsym`, `dlclose`, `syscall`
- Self-decrypts at runtime (runtime function fixup)
- **Static analysis alone is insufficient** — must dump at runtime after self-decryption

### 5. dlclose-blocker approach (LD_PRELOAD / Frida Interceptor)
- Attempted: Frida Interceptor.attach on dlclose to prevent library unloading
- Result: TypeError crashes from Frida when Interceptor.attach gets null function pointer
- Also: Process terminated before hooks could fully activate (anti-Frida)
- LD_PRELOAD with wrap.sh: app not debuggable, setprop wrap.com.jio.jioplay.tv
  caused crash

## Remaining Approaches to Try

### A. PADumper (GUI app, uses ptrace not Frida)
- Installed on device but requires manual GUI interaction
- May avoid Frida detection since it uses ptrace
- However: pairip's anti-debug (clone+waitpid+ptrace) may conflict with PADumper's ptrace

### B. PTRACE_STOP approach
- Write a native binary that ptrace ATTACH to the process, sends SIGSTOP immediately
- Then scan /proc/PID/maps and dump pairipcore before it can unload
- Challenge: timing — must attach between dlopen and dlclose

### C. Hook dlclose via a modified libpairipcore.so replacement
- Since we can't catch the library in memory, we could modify the APK's libpairipcore.so
  to remove the dlclose call AND the signature checks
- This is essentially the "patch the .so" approach but requires runtime dump first
- Circular dependency: need the dump to patch, need to patch to dump

### D. Memory dump at process crash (brute force)
- Deliberately trigger a crash AFTER pairipcore loads but BEFORE it can unload
- Extract the library from the tombstone/core dump
- Could work if we can cause a crash at the right moment

### E. QEMU/Unicorn emulation of JNI_OnLoad
- Dump the library using a custom Android runtime that executes JNI_OnLoad in an emulator
- Would get the decrypted version without the security checks killing the process
- Complex but theoretically sound

### F. GHIDRA analysis of static .so + runtime fixup simulation  
- Analyze the JNI_OnLoad function to understand the self-decryption routine
- Identify the decryption key/algorithm in the binary
- Apply decryption statically to produce the runtime-equivalent binary
- Most viable approach since we can't catch it in memory

## Files Created
- `/tmp/pairip-dump/x86_64_static/lib/x86_64/libpairipcore.so` — static (encrypted) binary
- `/tmp/pairip-dump/vm-bytecode/assets/` — all 22 VM bytecode files extracted
- `/tmp/hook_pairip_v3.js` — Frida polling script (detected and killed)
- Research doc: `docs/jiotv-mobile/research/pairip-hash-change-bypass-research.md`