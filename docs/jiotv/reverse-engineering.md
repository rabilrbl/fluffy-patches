# JioTV Reverse Engineering Methodology

## Target Application

- **Package**: `com.jio.jioplay.tv`
- **Version**: 7.1.7 (404)
- **APK**: `JioTV_v7.1.7(404)_antisplit.apk` (anti-split, single APK)
- **Source**: User-provided at `/var/home/rabil/Documents/JioTV_v7.1.7(404)_antisplit.apk`

## Tools Used

| Tool | Purpose |
|------|---------|
| **JADX-GUI** | Primary decompiler with MCP plugin for programmatic access |
| **morphe-cli** | Official CLI for testing patches against the APK |
| **unzip/strings** | Quick APK content inspection |
| **Python3** | Smali analysis scripts |

## JADX MCP Plugin Commands

```
jadx_fetch_current_class           # Get currently selected class
jadx_get_class_source              # Get full Java source of a class
jadx_get_smali_of_class            # Get smali representation (CRITICAL)
jadx_search_classes_by_keyword     # Search by keyword with scope filters
jadx_get_methods_of_class          # List all method names in a class
jadx_get_xrefs_to_method           # Find callers of a method
jadx_get_xrefs_to_class            # Find references to a class
```

## Critical Discovery: JADX Deobfuscation vs Real Smali Names

### The Problem

JADX deobfuscates package names. The decompiled Java shows:
- `com.jio.jioplay.p037tv.utils.CommonUtils`
- `com.jio.media.p062tv.p063ui.permission_onboarding.PermissionActivity`
- `com.jio.jioplay.p037tv.data.firebase.FirebaseConfig`

But the **actual smali names** in the APK are:
- `Lcom/jio/jioplay/tv/utils/CommonUtils;`
- `Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;`
- `Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;`

The obfuscation tokens (`p037`, `p062`, `p063`) are simply **removed** in the real smali.

### How to Find Real Smali Names

Always use `jadx_get_smali_of_class` and check the `.class` declaration line:

```
jadx_get_smali_of_class("com.jio.jioplay.p037tv.utils.CommonUtils")
```

Returns:
```
.class public final Lcom/jio/jioplay/tv/utils/CommonUtils;
```

**Rule**: Always verify class names via smali before writing patches. Never trust JADX's deobfuscated package names.

## Critical Discovery: Direct vs Virtual Methods

`.directMethods` and `.virtualMethods` in the Morphe patcher API correspond to smali's `# direct methods` and `# virtual methods` sections.

**Direct methods include**:
- Constructors (`<init>`, `<clinit>`)
- `private` methods
- `static` methods
- `final` methods (sometimes)

**Virtual methods include**:
- `public`/`protected` non-final instance methods
- **Override methods** (`onCreate`, `onStart`, `attachBaseContext`, etc.)

### Common Mistakes

| Method | Assumed | Actual | Reason |
|--------|---------|--------|--------|
| `onCreate` (Activity/ContentProvider) | direct | **virtual** | Lifecycle override |
| `onStart` (Activity) | direct | **virtual** | Lifecycle override |
| `attachBaseContext` (Application) | direct | **virtual** | Lifecycle override |
| `initializeLicenseCheck` (LicenseClient) | direct | **virtual** | Public instance method |
| `isSslPining` (FirebaseConfig) | direct | **virtual** | Public instance method |
| `check$okhttp` (CertificatePinner) | direct | **virtual** | Public instance method |
| `isRunningOnEmulator` (PermissionActivity) | direct | **virtual** | Public instance method |
| `isSupportedDevice` (PermissionActivity) | direct | **virtual** | Public instance method |
| `checkUpdate` (AppUpdateHelper) | direct | **virtual** | Public instance method |
| `checkUpdatefordiag` (AppUpdateHelper) | direct | **virtual** | Public instance method |
| `resumeUpdate` (AppUpdateHelper) | direct | **virtual** | Public instance method |
| `checkIsUpdateAvailable` (CommonUtils) | direct | direct | `static` method ✓ |
| `getCheckAppUpadteData` (CommonUtils) | direct | direct | `static` method ✓ |
| `redirectToPlayStore` (CommonUtils) | direct | direct | `static` method ✓ |
| `takeToPlayStore` (CommonUtils) | direct | direct | `static` method ✓ |
| `a` (AppUpdateHelper) | direct | direct | `static` method ✓ |

### How to Verify Method Types

Use `jadx_get_smali_of_class` and look for section markers:

```smali
# direct methods
.method public static checkIsUpdateAvailable()V  # <-- DIRECT
    ...
.end method

# virtual methods
.method public onStart()V  # <-- VIRTUAL
    ...
.end method
```

Or use a Python script to scan the smali output:

```python
import json
with open('smali_output.json') as f:
    data = json.load(f)
content = data['response']
lines = content.split('\n')

in_direct = False
in_virtual = False
for i, line in enumerate(lines):
    if '# direct methods' in line:
        in_direct, in_virtual = True, False
    elif '# virtual methods' in line:
        in_virtual, in_direct = True, False
    elif '.method' in line and 'targetMethodName' in line:
        print(f"{'DIRECT' if in_direct else 'VIRTUAL'}")
```

## Known Issues & Caveats

### 1. Synthetic Lambda Methods

Kotlin generates synthetic methods with names like `lambda$methodName$0`. When searching by method name, `.first { it.name == "methodName" }` may match a synthetic lambda instead of the real method. Always check the smali to confirm.

Example in `LicenseClient`:
- `lambda$initializeLicenseCheck$0()` - synthetic lambda (direct)
- `lambda$initializeLicenseCheck$1(Z)` - synthetic lambda (direct)
- `initializeLicenseCheck()` - real public method (virtual)

### 2. Multiple Overloaded Methods

Some methods have multiple overloads. `.first { it.name == "methodName" }` returns the first match, which may not be the one you want. Check parameter signatures in smali.

### 3. Resource Patching

Resource patches use `document("AndroidManifest.xml")` and `document("res/xml/...")`. The `document()` function returns a closable `org.w3c.dom.Document`. Always use `.use { }` block.

### 4. Network Security Config

The original `network_security_config.xml` has domain-specific configs. To enable full MITM:
- Set `cleartextTrafficPermitted="true"` on `<base-config>`
- Add user CA certificates to `<trust-anchors>`
- The manifest `usesCleartextTraffic` attribute must also be set

### 5. R8 Obfuscated Method Names

Some classes use R8 single-letter method names (`a`, `b`, `c`). These are static bridge methods that call the actual logic. When patching, use the obfuscated name as it appears in smali, not the deobfuscated Java name.

Example: `AppUpdateHelper.a(AppUpdateHelper, AppUpdateInfo)` is the method that calls `startUpdateFlowForResult()`.

### 6. Multiple Dex File Copies

Morphe-cli may redistribute classes across different dex files during patching. A class that exists in one dex file in the original APK may exist in multiple dex files in the patched APK. The patch only targets ONE copy. If `HomeActivity` references a different copy, the patch won't take effect.

**Workaround**: Patch at the call site level (e.g., `CommonUtils.getCheckAppUpadteData()` returning `null`) rather than at the callee level when multiple dex copies exist.
