---
name: android-apk-analysis
description: Analyze Android APK structure, decompile with JADX, find classes/methods, understand app architecture, and identify targets for patching. Use when reverse-engineering an APK or preparing to write patches.
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: reverse-engineering
---

## What I do

- Guide APK decompilation and analysis workflows
- Identify classes, methods, and fields relevant to patching
- Understand app architecture and component relationships
- Find detection mechanisms, SSL pinning, root detection, and other security features
- Document findings for patch development

## When to use me

Use this skill when:
- Analyzing a new APK to understand its structure
- Finding the right class/method to patch
- Investigating why a patch isn't working
- Documenting app internals for future contributors
- Converting between Java and smali representations

## APK Analysis Workflow

### 1. Initial APK Inspection

```bash
# Get APK info
aapt dump badging app.apk

# List dex files
unzip -l app.apk | grep ".dex"

# Extract and analyze
apktool d app.apk -o output_dir
```

### 2. JADX Decompilation

```bash
# Full decompilation
jadx app.apk -d output_dir

# With specific options
jadx app.apk -d output_dir --show-bad-code --deobf
```

### 3. Key Areas to Investigate

#### Application Entry Points
- Main Activity (from AndroidManifest.xml)
- Application class (onCreate method)
- Content Providers
- Services
- Broadcast Receivers

#### Security Mechanisms
- SSL/TLS pinning (look for `CertificatePinner`, `TrustManager`, `X509TrustManager`)
- Root detection (look for `su`, `Superuser`, `RootTools`, `Magisk`)
- Emulator detection (look for `Build.FINGERPRINT`, `Build.MODEL`, `TelephonyManager`)
- Integrity checks (look for `SafetyNet`, `PlayIntegrity`, `signature`)
- Debugger detection (look for `Debug.isDebuggerConnected`)

#### Feature Targets
- Network requests (Retrofit, OkHttp, Volley)
- Authentication flows
- Premium/feature gates
- Ad loading
- Analytics/telemetry

### 4. Finding Patch Targets

#### By Class Name
```bash
# Search for specific patterns
grep -r "isRooted" output_dir/
grep -r "sslPinning" output_dir/
grep -r "isPremium" output_dir/
```

#### By String Reference
```bash
# Find strings in the APK
jadx --deobf app.apk -d output_dir
grep -r "feature locked" output_dir/
```

#### By Method Usage
```bash
# Find callers of a method
# In JADX: right-click method → Find Usage
# Or search in decompiled code
grep -r "methodName(" output_dir/
```

### 5. Smali Analysis

When Java decompilation fails or is unclear, analyze smali directly:

```bash
# Using apktool
apktool d app.apk -o smali_output

# Key smali patterns:
# Method return types:
#   ->Z = boolean
#   ->V = void
#   ->Ljava/lang/String; = String
#   ->I = int
#   ->Ljava/util/List; = List

# Register types:
#   v0, v1, ... = local registers
#   p0, p1, ... = parameter registers (p0 = this for instance methods)
```

### 6. Common Smali Patterns for Patching

#### Boolean method returning false
```smali
.method public isFeatureLocked()Z
    .registers 2
    const/4 v0, 0x0
    return v0
.end method
```

#### Null return
```smali
.method public getRestriction()Ljava/lang/String;
    .registers 2
    const/4 v0, 0x0
    return-object v0
.end method
```

#### Empty list return
```smali
.method public getBlockedFeatures()Ljava/util/List;
    .registers 3
    new-instance v0, Ljava/util/ArrayList;
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
    return-object v0
.end method
```

#### Skip conditional branch
```smali
# Original: if-eqz v0, :cond_10
# Patched:  goto :cond_10
# Or: const/4 v0, 0x1
```

## Documentation Guidelines

When documenting findings, create files in `docs/<appname>/`:

- `ssl-pinning.md` — SSL pinning implementation details
- `root-detection.md` — Root detection mechanisms
- `emulator-detection.md` — How the app detects emulators
- `debugging-journey.md` — Step-by-step debugging notes
- `apk-structure.md` — Overall app architecture

Include:
- Class names (both Java and Dalvik formats)
- Method signatures
- Key strings used for detection
- Failed patch attempts and why they failed
- Useful smali patterns discovered

## Tools Reference

### JADX MCP Tools Available

- `jadx_get_android_manifest` — Get AndroidManifest.xml
- `jadx_get_main_activity_class` — Get main activity
- `jadx_search_classes_by_keyword` — Search classes by keyword
- `jadx_get_class_source` — Get full class source
- `jadx_get_xrefs_to_method` — Find method references
- `jadx_get_xrefs_to_class` — Find class references
- `jadx_get_strings` — Get string resources

### Command Line Tools

- `jadx` — Java decompiler
- `apktool` — APK decompiler/recompiler
- `aapt` — Android Asset Packaging Tool
- `dex2jar` — Convert dex to jar
- `jd-gui` — Java decompiler GUI

## Tips

1. Always check multiple APK versions — class names may change
2. Look for obfuscated names (a.b.c()) — track by usage patterns
3. Check for reflection-based loading — patches may need to target multiple classes
4. Note the app's minimum SDK version for compatibility
5. Document everything — future you (or contributors) will thank you
