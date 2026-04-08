# SSL Certificate Pinning

## Overview

JioTV uses SSL certificate pinning controlled by Firebase Remote Config. The app pins certificates for `tv.media.jio.com`.

## Pinning Toggle

### Location
`com.jio.jioplay.tv.data.firebase.FirebaseConfig` (`Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;`)

#### `isSslPining()` → **virtual method**
Returns a boolean from Firebase Remote Config that controls whether pinning is enabled.

**Bypass**: `return false`

## OkHttp3 CertificatePinner

### Location
`okhttp3.CertificatePinner` (`Lokhttp3/CertificatePinner;`)

#### `check$okhttp(String, List<Certificate>)` → **virtual method**
The actual pin verification method. Called by OkHttp3 when making HTTPS requests.

**Bypass**: `return-void`

## Legacy OkHttp CertificatePinner

### Location
`com.squareup.okhttp.CertificatePinner` (`Lcom/squareup/okhttp/CertificatePinner;`)

#### `check(String, List<Certificate>)` → **virtual method**
Legacy OkHttp pin verification. May or may not be present depending on the APK version.

**Bypass**: `return-void` (wrapped in `runCatching` for safety)

## Network Security Config

The original `res/xml/network_security_config.xml` contains:
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">tv.media.jio.com</domain>
        <pin-set>
            <pin digest="SHA-256">...</pin>
            <pin digest="SHA-256">...</pin>
        </pin-set>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

The cleartext traffic patch rewrites this to allow cleartext and user CAs:
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```
