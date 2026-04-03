# Fluffy Patches

A [Morphe Patches](https://morpheapp.github.io) repository with patches for Android apps.

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rabilrbl/fluffy-patches

Or manually add as a patch source in Morphe: https://github.com/rabilrbl/fluffy-patches

## Supported Apps

### JioTV Mobile (`com.jio.jioplay.tv`)

> [!WARNING]
> Patches are currently in development and do not work yet.

| Patch | Description |
|-------|-------------|
| Remove Play Store license check | Bypasses pairip licensing enforcement and app-side update redirects |
| Remove root detection | Disables root checks and custom security validation |
| Remove emulator detection | Bypasses device/emulator detection and Fire TV compatibility blocks |
| Remove certificate pinning | Disables OkHttp3 SSL certificate pinning for MITM proxy support |
| Enable cleartext traffic | Allows HTTP traffic and trusts user-installed CA certificates |
| Enable debugging | Sets the internal debug flag to bypass all security gates (includes root + emulator patches) |

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
