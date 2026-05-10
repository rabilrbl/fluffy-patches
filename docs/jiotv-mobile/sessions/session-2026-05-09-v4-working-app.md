# Session 2026-05-09 (v4): Working JioTV App — Pairip + Firebase + OkHttp Fixed!

## Summary

JioTV app launches, shows HomeActivity content, no pairip block wall, no fatal crashes. Three critical issues fixed in this session:

1. **ClassNotFoundException for pairip Application**: Fixed by changing `android:name` in AndroidManifest.xml from `com.pairip.application.Application` to `com.jio.jioplay.tv.JioTVApplication`. This bypasses pairip entirely at the manifest level.

2. **FirebaseApp not initialized**: Fixed by adding `FirebaseInitProvider` back to the manifest (Morphe patch had removed it). FirebaseInitProvider is required for Firebase services used by the app.

3. **OkHttp NoClassDefFoundError (Hpack/Header)**: Fixed by adding `sget-object pkOPEgq.hxbBMeNJrlJB` in `JioTVApplication.attachBaseContext()` to force-initialize the pairip string class before OkHttp loads. The pairip runtime was disabled, so its string decryption class `pkOPEgq` wasn't being initialized by the runtime — our clinit injections set the values, but the class needed explicit loading before `Header.clinit` ran.

## Build Details

### APK: jiotv-v20-aligned.apk (90MB)

**Base**: v12 (clinit-v2) APK with manifest modifications via apktool

**Manifest Changes**:
- `android:name="com.jio.jioplay.tv.JioTVApplication"` (was `com.pairip.application.Application`)
- `android:extractNativeLibs="true"` (was `"false"`, needed for our signing)
- Added `FirebaseInitProvider` with authority `com.jio.jioplay.tv.firebaseinitprovider`

**Dex Files (all 12)**:
| Dex | Source | Patches |
|-----|--------|---------|
| classes.dex | Recompiled | LicenseClientV3 bypass + clinit injections |
| classes2.dex | Recompiled | SSL pinning bypass (JioPlayer k.c) |
| classes3.dex | Recompiled | pkOPEgq clinit injections + **JioTVApplication.attachBaseContext pkOPEgq init** |
| classes4.dex | Original v12 | Can't recompile (65536 method limit) |
| classes5.dex | Recompiled | LicenseClientV3 patches (initializeLicenseCheck, processResponse, showPaywall, showErrorDialog) |
| classes6.dex | Recompiled | APIManager SSL pinning bypass |
| classes7.dex | Recompiled | AppCompatActivity patch |
| classes8.dex | Original v12 | Can't recompile (65536 method limit) |
| classes9.dex | Recompiled | clinit injections |
| classes10.dex | Original v12 | Unmodified |
| classes11.dex | Recompiled | FirebaseConfigUtil null check |
| classes12.dex | Recompiled | clinit injections |

**Build Process**:
1. Start from v12 (clinit-v2) APK base
2. Modify AndroidManifest.xml via apktool to change Application class, add FirebaseInitProvider, set extractNativeLibs=true
3. apktool rebuild failed on resources (split APK issue) — used v18 manifest instead
4. Replace v12's manifest with v18's manifest (apktool-built)
5. Replace v12's resources.arsc with v18's (apktool-built with placeholder drawables) — **WRONG, this caused ClassNotFoundException**
6. **Final approach**: v12 base APK + v18 manifest only (resources from v12) + all 12 patched dex files
7. 4096-byte page alignment for native libs + apksigner

**Key Insight**: apktool-rebuilt resources.arsc breaks class loading (ClassNotFoundException for pairip Application). The v12 base's original resources.arsc works fine. Only the manifest needed apktool rebuild.

## Patches Applied

### New in v20: JioTVApplication.attachBaseContext — pkOPEgq initialization
```smali
# In JioTVApplication.attachBaseContext, after MultiDex.install:
sget-object v0, Lcom/iab/omid/library/ril/HNCT/pkOPEgq;->hxbBMeNJrlJB:Ljava/lang/String;
```
Forces `pkOPEgq.clinit` to run before any OkHttp code, ensuring pairip string fields are initialized.

### Previous patches (carried forward):
- LicenseClientV3 bypass (initializeLicenseCheck → return-void, processResponse → OK state, showPaywall → return-void, showErrorDialog → return-void)
- SSL pinning bypass (APIManager isSslPining → goto skip, JioPlayer k.c cert pin → goto skip)
- FirebaseInitProvider restored in manifest
- Application class changed to JioTVApplication
- extractNativeLibs set to true

## Known Issues

1. **Layout InflateException**: `tab_content_adapter_item` line #364 — Error inflating class `<unknown>`. Non-fatal, caused by missing split APK resources. The app continues to work but some UI elements may not render.
2. **Content playback**: Not yet tested — will likely crash with `Resources$NotFoundException` for ExoPlayer drawables (same issue as v12).
3. **JSON parsing errors**: `isNewPdp` (null int) and `Banners` (null JSONArray) — from API responses with null fields. Non-fatal.
4. **30 unresolved String fields**: From 14 missing classes. Non-critical.

## Build Command Sequence

```bash
# 1. Modify AndroidManifest.xml in apktool-work (Application class, FirebaseInitProvider, extractNativeLibs)
# 2. Rebuild with apktool
apktool b apktool-work -o apktool-v18.apk

# 3. Create v20 APK from v12 base + v18 manifest + patched dex files
cp work/jiotv-clinit-v2.apk jiotv-v20.apk
zip -d jiotv-v20.apk "classes*.dex" "AndroidManifest.xml"
# Add v18's manifest
cd v18-manifest && zip -0 ../jiotv-v20.apk AndroidManifest.xml
# Add patched dex files (stored, not compressed)
cd work && for i in "" 2 3 5 6 7 9 11 12; do zip -0 ../jiotv-v20.apk classes${i}.dex; done
# Add v12 originals for classes4/8/10
cp /tmp/dex-patch/v12-dex/classes4.dex /tmp/dex-patch/v12-dex/classes8.dex /tmp/dex-patch/v12-dex/classes10.dex .
for f in classes4.dex classes8.dex classes10.dex; do zip -0 ../jiotv-v20.apk $f; done

# 4. Sign and align
zipalign -f 4096 jiotv-v20.apk jiotv-v20-aligned.apk
apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android jiotv-v20-aligned.apk
```

## Files Modified

- `/tmp/dex-patch/work/smali_classes3/com/jio/jioplay/tv/JioTVApplication.smali` — Added pkOPEgq initialization in attachBaseContext
- `/tmp/dex-patch/apktool-work/AndroidManifest.xml` — JioTVApplication, FirebaseInitProvider, extractNativeLibs=true