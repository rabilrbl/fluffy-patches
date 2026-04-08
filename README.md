# Fluffy Patches

A [Morphe Patches](https://morpheapp.github.io) repository with patches for Android apps.

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rabilrbl/fluffy-patches

Or manually add as a patch source in Morphe: https://github.com/rabilrbl/fluffy-patches

## Supported Apps

### JioTV Mobile (`com.jio.jioplay.tv`)

> [!WARNING]
> The JioTV work here is currently split across two different targets and should not be treated as one coherent patch set.
>
> - **371 split build**: the current baseline from Uptodown, uses `licensecheck3`, and the untouched original signed splits launch on the x86_64 AVD.
> - **404 merged / antisplit path**: older APKMirror-style notes and patches.
>
> Re-signing the 371 split APK set is enough to trigger a native `libpairipcore.so` crash, so the Morphe APK-modification flow is currently blocked for that target.

| Track | Status | Notes |
|------|--------|-------|
| 371 split (`versionCode 371`, `versionName 7.1.7`) | Active baseline | Original signed splits launch. Runtime-hooking is the practical route. |
| 404 merged / antisplit (`7.1.7 (404)`) | Historical research | Older docs and class references live here. Keep separate from 371 work. |

| Patch | Description |
|-------|-------------|
| Disable FirebaseInitProvider | Removes `FirebaseInitProvider` from `AndroidManifest.xml` as a research aid when VM-backed config data is unavailable |
| Dismiss Google Play error dialog (371 split research) | Experimental `licensecheck3` patch that only suppresses `LicenseClientV3.handleError()` and avoids old VM-disabling edits |
| Remove root detection | Research patch for root / integrity-related Java-side checks |
| Remove emulator detection | Research patch for emulator / unsupported-device checks |
| Remove certificate pinning | Research patch for SSL/TLS inspection |
| Enable cleartext traffic | Enables cleartext traffic and trusts user CAs for network inspection |

See `docs/jiotv/targets.md` and `docs/jiotv/session-2026-04-08-latest-uptodown-xapk.md` before extending JioTV patches.

## Usage

1. Download the latest `.mpp` file from [Releases](../../releases).
2. Open Morphe Manager and import the patch package.
3. Select the target APK and apply the desired patches.

## Building from Source

Requires JDK 17.

```bash
./gradlew :patches:buildAndroid
```

Output: `patches/build/libs/patches-<version>.mpp`

## Contributing

### Adding a Patch for an Existing App

1. Create a `.kt` file under `patches/src/main/kotlin/app/template/patches/<appname>/`.
2. Define a `bytecodePatch` or `resourcePatch` with `compatibleWith(<APP_CONSTANT>)`.
3. Annotate the top-level `val` with `@Suppress("unused")`.

### Adding a New App

1. Add a `Compatibility(...)` entry in `shared/Constants.kt` with the app's package name, APK type, and icon color.
2. Create a subdirectory under `patches/<appname>/` for the patches.
3. Reference the new constant in each patch's `compatibleWith(...)` call.

### Workflow

- Development happens on `dev`; open PRs targeting `dev`.
- `main` is the release branch — do not push directly.
- Releases are automated via semantic-release; use conventional commits (`feat:`, `fix:`, `chore:`, `refactor:`).

## License

[GPLv3](LICENSE) with Section 7 restriction: the name "Morphe" may not be used in derivative works. See [NOTICE](NOTICE) for full conditions.
