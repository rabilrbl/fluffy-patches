# Fluffy Patches

A [Morphe Patches](https://morpheapp.github.io) repository with patches for Android apps.

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rabilrbl/fluffy-patches

Or manually add as a patch source in Morphe: https://github.com/rabilrbl/fluffy-patches

## Supported Apps

### Alarmy (`droom.sleepIfUCan`)

| Patch | Description |
|-------|-------------|
| Unlock Pro subscription | Forces `PremiumState.isPremium()` to always return `true`, unlocking all premium features |
| Remove ads | Forces `PremiumState.isRemoveAdPremium()` to always return `true`, disabling ads |

**Target:** Alarmy v26.23.0 (XAPK)

See `docs/alarmy/README.md` for APK architecture details.

## Usage

1. Download the latest `.mpp` file from [Releases](../../releases).
2. Open Morphe Manager and import the patch package.
3. Select the target APK and apply the desired patches.

## Building from Source

Requires JDK 17 and `ANDROID_HOME`.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export GITHUB_ACTOR="your-github-username"
export GITHUB_TOKEN="your-github-token"
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
4. Run `./gradlew :patches:generatePatchesList` to regenerate metadata.

### Workflow

- Development happens on `dev`; open PRs targeting `dev`.
- `main` is the release branch — do not push directly.
- Releases are automated via semantic-release; use conventional commits (`feat:`, `fix:`, `chore:`, `refactor:`).

## License

[GPLv3](LICENSE) with Section 7 restriction: the name "Morphe" may not be used in derivative works. See [NOTICE](NOTICE) for full conditions.
