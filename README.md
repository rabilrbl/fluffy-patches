# Fluffy Patches

A [Morphe Patches](https://morpheapp.github.io) repository with patches for Android apps.

## ❓ About

Patches for Android apps, ready to use with [Morphe Manager](https://github.com/MorpheApp/morphe-manager).

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rabilrbl/fluffy-patches

Or manually add as a patch source in Morphe: https://github.com/rabilrbl/fluffy-patches

## 🩹 Patches list

<!-- Do not modify the section between the PATCHES markers by hand. It is regenerated
     from patches-list.json when release.yml creates a new release.

     To collapse the patches list, remove the word 'EXPANDED' from the comment tag below. -->

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0](https://github.com/rabilrbl/fluffy-patches/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 BlockerX&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 5.0.81 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Bypass Instant Approval](#bypass-instant-approval) | Bypasses the local coin redemption step for Instant Approval actions. |  |
| [Enable Premium](#enable-premium) | Enables BlockerX premium gates and premium module access. |  |

</details>

<details open>
<summary>📦 Alarmy&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 26.32.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock Premium](#unlock-premium) | Unlocks premium features and disables ads by forcing both premium gates true. |  |

</details>

<!-- PATCHES_END -->

## 🚀 Usage

1. Download the latest `.mpp` file from [Releases](../../releases).
2. Open Morphe Manager and import the patch package.
3. Select the target APK and apply the desired patches.

Users can also apply `dev` branch pre-releases by enabling `pre-release` patch sources in Morphe Manager.

## 🛠️ Building locally

Requires JDK 21 and `ANDROID_HOME`.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export GITHUB_ACTOR="your-github-username"
export GITHUB_TOKEN="your-github-token"
./gradlew :patches:buildAndroid
```

The built patches `.mpp` file is found in `patches/build/libs/patches-*.mpp`.

## 🧑‍💻 Contributing

- **Make all changes to the `dev` branch.** Open pull requests targeting `dev`.
- `main` is the stable release branch — do not push directly. When `dev` is ready, merge `dev` into `main` (merge commit only, no squash) to create a stable release.
- **Always use semantic release ([release.yml](.github/workflows/release.yml))**. Do not manually create releases — generated files such as `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, and the README patches list are updated automatically by the release workflow.
- Always use [conventional commit](https://kapeli.com/cheat_sheets/Semantic_Commits.docset/Contents/Resources/Documents/index) messages:
  - `feat: Added a new feature` — creates a minor pre-release on `dev`
  - `fix: Some problem now fixed` — creates a patch pre-release on `dev`
  - `chore: Random change you do not want in the user facing changelog` — no release
- Do not force push semantic release commits, as that breaks all future releases.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guidelines.

## 📜 License

[GPLv3](LICENSE) with Section 7 restriction: the name "Morphe" may not be used in derivative works. See [NOTICE](NOTICE) for full conditions.
