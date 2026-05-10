# Frida and unpaircore Attempts

## Frida Approach

### Setup
- frida-server 17.9.1 running on AVD (x86_64)
- APK modified with Frida hooks
- Port forwarded via ADB: `adb forward tcp:27042 tcp:27042`

### Results

**Test 1: Frida spawn with hooks**
```
frida -H 127.0.0.1:27042 -f com.jio.jioplay.tv -l hooks.js
```
- App crashed immediately with SIGSEGV in libpairipcore.so
- Native library detects Frida instrumentation and crashes

**Test 2: Frida attach to running process**
```
frida -H 127.0.0.1:27042 -n com.jio.jioplay.tv -l hooks.js
```
- App already died from license check
- Could not attach in time

**Test 3: Frida await spawn**
```
frida -H 127.0.0.1:27042 -W com.jio.jioplay.tv -l hooks.js
```
- App dies too quickly for Frida to attach
- Process never appears in spawn list

### Root Cause
The x86_64 libpairipcore.so has anti-instrumentation checks that detect Frida and crash. The app dies before Frida can establish hooks.

### Frida Script Used
```javascript
Java.perform(function() {
  var LicenseClient = Java.use("com.pairip.licensecheck.LicenseClient");
  LicenseClient.initializeLicenseCheck.implementation = function() {
    console.log("[Frida] initializeLicenseCheck - bypassed!");
  };
  LicenseClient.connectToLicensingService.implementation = function() {
    console.log("[Frida] connectToLicensingService - bypassed!");
  };
  var SignatureCheck = Java.use("com.pairip.SignatureCheck");
  SignatureCheck.verifyIntegrity.implementation = function(ctx) {
    console.log("[Frida] verifyIntegrity - bypassed!");
  };
});
```

## unpaircore Approach

### Repository
https://github.com/Kitsuri-Studios/unpaircore

### Analysis
- This is a game hacking framework, NOT a pairip bypass library
- Contains hooking infrastructure (inline hook, sigscan, memory manipulation)
- Has a Minecraft telemetry bypass example (patch_libs function)
- No pairip-specific logic

### Verdict
Not useful for JioTV pairip bypass.

## Conclusion

Frida approach fails because:
1. Native library detects instrumentation
2. App dies before hooks can be established
3. Frida gadget modifies APK which triggers integrity check

unpaircore approach fails because:
1. It's a game hacking framework, not pairip-specific
2. No actual pairip bypass logic

**Recommendation**: Manual approach requires reverse engineering the VM bytecode encryption and creating a mock library that provides fake "verified" dex data.
