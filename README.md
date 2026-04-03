# Fluffy Patches

A [Morphe Patches](https://morpheapp.github.io) repository with patches for Android apps.

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rabilrbl/fluffy-patches

Or manually add as a patch source in Morphe: https://github.com/rabilrbl/fluffy-patches

## Supported Apps

### JioTV Mobile (`com.jio.jioplay.tv`)

> [!WARNING]
> Patches are currently in development and may not work on all devices or APK versions.

| Patch | Description |
|-------|-------------|
| Disable pairip license check (manifest) | Removes the pairip `LicenseContentProvider` from AndroidManifest to prevent auto-initialization of license checking |
| Remove Play Store license check | Bypasses pairip DRM (signature check, paywall, error dialogs), server-driven update checks (`getCheckAppUpadteData` returns null), Google Play Core in-app updates (`AppUpdateHelper` no-ops), and Play Store redirect helpers |
| Remove root detection | Disables Firebase `isRooted()` check, `SecurityUtils.isValidVersionName()`, and Xposed framework detection dialog |
| Remove emulator detection | Bypasses `isRunningOnEmulator()`, `isSupportedDevice()`, and skips all detection logic in `PermissionActivity.onCreate()` |
| Remove certificate pinning | Disables Firebase-controlled `isSslPining()` toggle and neutralizes OkHttp3 `CertificatePinner.check()` |
| Enable cleartext traffic | Sets `usesCleartextTraffic=true` in manifest and rewrites `network_security_config.xml` to trust user CAs |

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
