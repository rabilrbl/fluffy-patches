# Pairip Bypass After APK Hash Changes — Research & Approach

## The Core Problem

Any APK modification (re-signing, smali patches, dex edits) triggers pairip's native
integrity check in `libpairipcore.so`. The VM runs in a `static {}` initializer + `JNI_OnLoad`
**BEFORE** any Java/smali code executes. Re-signing changes CRC/signature → native crash
before patches can activate.

Additionally, the VM's encrypted bytecode in `assets/` contains **critical app code**
(Kotlin metadata initialization, OnReceiver methods, etc.), so simply disabling pairip
causes a different crash — the app's own code never executes.

## Pairip's 4-Layer Defense

1. **Java layer** — `Application.<clinit>` → `StartupLauncher.launch()` → `VMRunner.invoke()`
2. **VM bytecode layer** — Encrypted assets executed by native VM; contains actual app code, not just checks
3. **Native layer** — `libpairipcore.so` self-decrypts at runtime; `executeVM()` has 100+ case switch
4. **Security checks inside switch** — signature, CRC32/dex integrity, anti-debug (clone+waitpid), anti-frida, emulator, root

## Approaches That Don't Work

| Approach | Why It Fails |
|----------|-------------|
| Mock native library (empty stub) | VM bytecode contains critical Kotlin metadata init — without it: ExceptionInInitializerError |
| Frida on original signed APK | Anti-frida kills process; child monitors /proc/self/maps |
| Smali patches alone | Native integrity check crashes before Java patches activate |
| Signature spoofing (preserve sigs) | v2/v3 signing overwrites on any modification; impractical for Morphe patches |
| Replace VM bytecode in assets/ | Bytecode encrypted and tied to specific libpairipcore.so per release |

## Approaches That Can Work

### 1. Patch the dumped `libpairipcore.so` directly (Solaree's proven method)

The Solaree/pairipcore author confirmed this works: "bypassed by rebuilding binary
and reconstructing executeVM to strip those calls."

Steps:
1. **Runtime dump** — Use PADumper on rooted device running original signed JioTV v371 to dump decrypted `libpairipcore.so` from process memory after `JNI_OnLoad`
2. **Analyze executeVM** — Locate function via RegisterNatives offset; find the switch statement
3. **NOP out security checks** — Signature verification case, CRC32/integrity case, anti-debug child spawner, Frida detection, emulator detection
4. **Preserve VM execution** — Keep all execution opcodes (CALL_JAVA, CALL_NATIVE, etc.) intact
5. **Ship patched .so** — Include in the modified APK; VM still runs bytecode and initializes app code, but all verification is removed

### 2. `libpairipcorex.so` replacement (Sbenny's toolkit)

Pre-built stripped version of the native library used in Sbenny's automated patching
suite (`patch1.py`/`patch2.sh`). Has JNI interface stubs but no security checks.

**Must verify**: Whether this library can actually execute JioTV's VM bytecode from
`assets/` or is just a generic stub. If it lacks VM execution capability, it won't
work for JioTV.

### 3. LSPosed/Xposed module (root required at runtime)

Runtime hook approach — pairipfix module exists but may be detected. A JioTV-specific
module could:
- Hook `SignatureCheck.verifyIntegrity()` → return success
- Hook `VMRunner.setContext()` → pass spoofed context
- Kill anti-debug child process after spawn
- Hook PackageManager to return expected signing certificate

### 4. MatrixEditor/pairipcore-vm for bytecode analysis

Python + Rust tools for pairip VM bytecode disassembly. Can identify which app methods
have been virtualized. Key for determining if VM can be safely removed or must be
preserved. WIP — decompiler not yet complete.

## Recommended Next Steps

### Priority 1: Runtime dump of JioTV's libpairipcore.so
- Must happen on the rooted AVD with original signed JioTV v371 running
- Use PADumper after JNI_OnLoad completes
- Gets the real binary with resolved function fixups (static analysis is useless — self-decrypts)

### Priority 2: Analyze what VM bytecode actually virtualizes
- Use MatrixEditor/pairipcore-vm tools on JioTV's assets/ bytecode files
- Map which app methods are virtualized (Kotlin metadata? OnReceiver? critical paths?)
- Determines whether VM execution must be preserved or can be stripped

### Priority 3: Patch the dumped .so
- In the dumped binary, locate executeVM function
- Identify security check cases vs VM execution cases in the switch statement
- NOP out: signature, CRC32, anti-debug, anti-frida, emulator checks
- Keep: all VM execution, bytecode decryption, method dispatch
- Test patched .so in a re-signed/modified APK

### Priority 4: Test libpairipcorex.so as shortcut
- Check if Sbenny's replacement library can run JioTV's VM bytecode
- If yes → much faster path than custom .so patching
- If no → fall back to Priority 3

## Key Resources

- **Solaree/pairipcore** — Architecture docs, executeVM analysis, anti-debug bypass
- **MatrixEditor/pairipcore-vm** — VM bytecode disassembler/decompiler (WIP)
- **SafaSafari/bypass_libpairipcore** — CRC32 patching approach (Java-only protection)
- **ahmedmani/pairipfix** — LSPosed module for runtime bypass
- **PADumper** — Process memory dumper for runtime .so dump
- **Sbenny's patch1.py/patch2.sh** — Automated patching with libpairipcorex.so
- **pairipcore.com (reversesio.com)** — Commercial bypass service (24-72hr)