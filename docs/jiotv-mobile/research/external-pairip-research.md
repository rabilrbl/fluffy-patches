# External PairIP Research Findings

## Overview

This document consolidates findings from external pairip research sources, including GitHub projects, forum posts, and blog articles.

## Key Sources

### 1. Solaree/pairipcore (GitHub)
- **URL**: https://github.com/Solaree/pairipcore
- **Stars**: 389+
- **Purpose**: Public research of Google's Android apps protection

#### Technical Findings
- Native library uses **runtime function fixup** — offsets change at load time, preventing static analysis
- **100+ security checks** in a switch-dispatch VM inside `executeVM()`
- Anti-debugging via `prctl`/`clone`/`waitpid`/`ptrace` with child process for `/proc/self/maps` checks
- Hundreds of system property checks via `__system_property_read_callback`
- Full frida detection (not just port scanning) — uses messaging/packet sending to detect frida-server
- Library **self-decrypts at runtime** — original offsets are meaningless
- Uses `dlopen`/`dlsym`/`dlclose` for dynamic import of bionic libc functions
- `syscall` and `SVC 0`-based custom function calls to evade analysis
- Basic anti-debugger (`prctl`, `clone`, `waitpid`, `ptrace`)
- `/proc/self/maps`, `/proc/self/status` checks
- System property functions, `access`, `opendir`, `readdir`, `closedir` for critical directory checks
- Full frida-server check (not only default port, like Promon Shield)

#### VM Dispatch Structure
The `executeVM` function contains a massive switch statement with 130+ cases, each calling a different security check function:
- Case 49: `antidebugger()` — prctl/clone/waitpid/ptrace anti-debugging
- Case 63: `fstatfs_check()` — filesystem check
- Case 74: `getdents64_check()` — directory enumeration check
- Case 79: `fstat_check()` — file stat check
- Case 80: `dlopen_libc()` — dynamic library loading
- Case 93: `read_check_1()` — file read check
- Case 103: `dlsym_check_1()` — symbol resolution check
- Case 128: `clock_gettime_check()` — timing check
- And 100+ more obfuscated checks

#### Anti-Debugging Bypass
The anti-debugger uses a fork pattern:
```
prctl(PR_GET_DUMPABLE, ...)
prctl(PR_SET_DUMPABLE, 1)
prctl(PR_SET_PTRACER, -1)
clone(0, 0, 0, 0)
waitpid(child_pid, ...)
prctl(PR_SET_PTRACER, 0)
prctl(PR_SET_DUMPABLE, 0)
```
The child process performs `/proc/self/maps` and `/proc/self/status` checks. Can be bypassed by sending `SIGKILL` to the child after `waitpid`.

#### Bypass Approach (from Solaree)
1. Find pairip reference in AndroidManifest.xml (`com.pairip.application.Application`)
2. Locate the smali file for the application class
3. Replace `com.pairip.application.Application` in manifest with the **real** app class path (found in the pairip Application smali's `super` reference)
4. Build and patch the APK
5. Run `crc32_patcher.py` to restore original dex CRC32 values

---

### 2. SafaSafari/bypass_libpairipcore (GitHub)
- **URL**: https://github.com/SafaSafari/bypass_libpairipcore
- **Stars**: 58+
- **Purpose**: Bypass libpairipcore tamper and signature protection

#### CRC32 Patcher Approach
The key insight: pairip's native VM reads dex file CRC32 values from the **ZIP central directory**. If we modify dex files but restore the original CRC32 values in the ZIP metadata, the native VM sees the original hashes and passes.

```python
def crc_patch(crc: int):
    def inner_crc(*args):
        return crc
    return inner_crc

# For each file in patched APK, restore original CRC from original APK
for file in patched.filelist:
    try:
        crc = [x for x in orig.filelist if x.filename == file.filename][0].CRC
        setattr(zipfile, "crc32", crc_patch(crc))
    except:
        pass
    patched_patched.writestr(file, data)
    setattr(zipfile, "crc32", orig_crc_binascii)
```

#### Steps
1. Merge split APKs using APKEditor
2. Decompile APK
3. Find `com.pairip.application.Application` in AndroidManifest.xml
4. Find the real app class path from the pairip Application smali
5. Replace `com.pairip.application.Application` with the real class in manifest
6. Build APK
7. Run `crc32_patcher.py patched.apk original.apk`
8. Sign and install

---

### 3. Snailsoft/Sbenny Forum Tutorial
- **URL**: https://forum.sbenny.com/thread/bypassing-pairip-2025-and-variants-using-pairipcore-so.191135/
- **Date**: Nov 2025

#### Key Findings
- Uses `libpairipcorex.so` — a **replacement native library** that bypasses integrity checks
- **Two-tier checking**: primary (online) + secondary (offline/firewall/airplane mode)
- The secondary check is in `connectToLicensingService()` — can be neutralized with regex replacement:
  ```
  Search: \.method (?:private|public|protected)?\s+connectToLicensingService\(\)V[\s\S]*?\.end\s+method
  Replace: .method private connectToLicensingService()V\n\t.registers 1\n\n\treturn-void\n.end method
  ```
- Works on split APK formats (xapk/apkx/apk+)
- Uses APKEditor-1.4.5.jar for merging
- **Device compatibility issues exist** — works on some devices, not others
- Some variants use **Google加固** (Google Jiagu) — a Chinese modified pairip with even more protection
- Some apps also include **Jiagu 360 security** with anti-hook protection

#### Files Used
- `APKEditor-1.4.5.jar` — merging and decompiling
- `libpairipcorex.so` — replacement native library
- `pairip-protection-remover-main.bin` — binary patch resource
- `patch1.py` — automatic Python patching
- `patch2.sh` — automatic BASH patching
- `patch3.py` — manual Python patching (for older pairip versions)

---

### 4. ahmedmani/pairipfix (GitHub, LSPosed Module)
- **URL**: https://github.com/ahmedmani/pairipfix
- **Stars**: 424+
- **Purpose**: LSPosed module to bypass signature checks for sideloaded apps

#### Approach
- **Runtime hooks** via LSPosed/Xposed — does NOT modify the APK
- Hooks `LicenseClient.processResponse()` → force `responseCode = 0` (LICENSED)
- Hooks `ResponseValidator.validateResponse()` → DO_NOTHING
- Works because native VM integrity checks pass (APK is unmodified)
- Does NOT handle native VM crash — recommends BetterKnownInstalled for that

---

### 5. Pixel-Props/BetterKnownInstalled (GitHub, Magisk Module)
- **URL**: https://github.com/Pixel-Props/BetterKnownInstalled
- **Purpose**: Fakes Play Store installer at system level

#### Approach
- Modifies `/data/system/packages.xml` at boot
- Sets: `installer=com.android.vending`, `installInitiator=com.android.vending`, `installerUid`, `packageSource=2`
- Makes sideloaded apps appear as Play Store installs to the system
- Requires root (Magisk/KernelSU)

---

### 6. Kitsuri-Studios/unpaircore (GitHub)
- **URL**: https://github.com/Kitsuri-Studios/unpaircore
- **Stars**: 13+
- **Purpose**: Fake libpairipcore.so for research purposes
- **Language**: C (93.2%), C++ (6.2%)
- **License**: BSD-3-Clause
- Depends on GamePwnage (Kitsuri-Studios/gamepwnage)

---

### 7. qyzhaojinxi/bypass_pairipcore (GitHub)
- **URL**: https://github.com/qyzhaojinxi/bypass_pairipcore
- **Stars**: 26+
- **Purpose**: Bypass pairip, fix APK after modification
- **Language**: JavaScript (Frida script)
- Supports latest pairip (as of Jan 2026)
- Temporarily does NOT support online Unity games
- Paid service: ~$100 per APK

---

### 8. Medium Article - Bypassing PairIP Integrity Checks
- **URL**: https://petruknisme.medium.com/bypassing-pairip-integrity-checks-21d7bdd4a052
- **Date**: Dec 2025

#### Key Findings
- **Solution 1**: LSPosed + pairipfix module
- **Solution 2**: Lucky Patcher → Remove License Verification → Auto Mode (dex + inversed)
- **Solution 3**: Manual modification — remove pairip components from AndroidManifest.xml
  - Remove `<activity>` with `com.pairip.licensecheck.LicenseActivity`
  - Remove `<provider>` with `com.pairip.licensecheck.LicenseContentProvider`
  - Remove `<uses-permission>` with `com.android.vending.CHECK_LICENSE`
- For apps with `libpairipcore.so`, manifest tweaks alone won't work — need native library modification
- RASP (Runtime Application Self-Protection) can detect hooking frameworks

---

### 9. DroidWin Article
- **URL**: https://droidwin.com/fix-get-this-app-from-play-store-bypass-pairip-checks/
- **Date**: Jan 2026

#### Key Findings
- PairIP performs four major tasks: License Verification, Signature Validation, Tamper Detection, Code Virtualization
- **Both pairipfix AND BetterKnownInstalled are required** (as of latest update)
- Also recommends HideMyApplist to hide root from the app
- Non-root methods (via Shizuku) did NOT work

---

## Summary of All Known Bypass Approaches

| Approach | Method | Requires Root | Works with Morphe | Notes |
|----------|--------|---------------|-------------------|-------|
| **LSPosed (pairipfix)** | Runtime hooks | Yes (LSPosed) | No | Hooks at runtime, no APK modification |
| **BetterKnownInstalled** | System-level faking | Yes (Magisk) | No | Modifies packages.xml at boot |
| **CRC32 Restoration** | ZIP metadata fix | No | Maybe | Restores original dex CRC after patching |
| **Manifest Swap** | Change app class | No | Maybe | Replace pairip Application with real one |
| **Native Library Replacement** | Replace .so | No | Maybe | Replace libpairipcore.so with stub/bypassed version |
| **Native Library Patching** | Patch .so functions | No | No | Complex, requires reverse engineering |
| **Lucky Patcher** | Auto-patch dex | Yes | No | Remove License Verification |
| **APKEditor + Scripts** | Full pipeline | No | No | Snailsoft's Termux-based approach |

## Critical Discovery: StartupLauncher Injection Point

`StartupLauncher.launch()` is injected into `androidx.multidex.MultiDexApplication.<clinit>()`:

```smali
.class public Landroidx/multidex/MultiDexApplication;
.super Landroid/app/Application;

.method static constructor <clinit>()V
    .registers 3
    invoke-static {}, Lcom/pairip/StartupLauncher;->launch()V
    return-void
.end method
```

This means the native VM runs when `MultiDexApplication` class is loaded, **regardless** of what the manifest application class is set to. Changing the manifest application class from `com.pairip.application.Application` to `com.jio.jioplay.tv.JioTVApplication` is insufficient because `JioTVApplication` extends `MultiDexApplication`, which triggers the `<clinit>`.

## Class Hierarchy

```
android.app.Application
  └── androidx.multidex.MultiDexApplication (has StartupLauncher.launch() in <clinit>)
        └── com.jio.jioplay.tv.JioTVApplication (real app code)
              └── com.pairip.application.Application (pairip wrapper)
```

The pairip Application class:
```java
public class Application extends JioTVApplication {
    @Override
    protected void attachBaseContext(Context context) {
        VMRunner.setContext(context);
        SignatureCheck.verifyIntegrity(context);
        super.attachBaseContext(context);
    }
}
```

## Native Library Analysis

### libpairipcore.so
- **Size**: 527,480 bytes
- **Architecture**: ARM64 (arm64-v8a)
- **Symbols**: Stripped
- **JNI methods**: Only `JNI_OnLoad` and `JNI_OnUnload` are visible
- **Dynamic dependencies**: libdl.so, libc.so, libm.so, liblog.so
- **Self-modifying**: Functions are decrypted/fixup at runtime

### Encrypted Assets
The VM bytecode files in `assets/` are encrypted:
- `mVBwD2didVTgj5k7` (224,303 bytes) — main VM bytecode (referenced by `StartupLauncher.startupProgramName`)
- 16+ other encrypted files (~90KB each) — additional VM modules
- Magic bytes: `.IAP` (encrypted format)
- Decrypted by `VmDecryptor.decrypt()` before being passed to `executeVM()`

## App Java Code Status

**The app's Java code is NOT encrypted** — it's fully present in the dex files. The encrypted assets are only for the pairip VM which performs integrity/license checks. The actual app logic (JioTVApplication, HomeActivity, etc.) is standard Java/Kotlin code that runs normally once the VM passes.

This means if we can prevent the native VM from crashing (by not modifying dex files, or by replacing the native library), the app should work.
