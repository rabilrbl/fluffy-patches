# [1.0.0-dev.12](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.11...v1.0.0-dev.12) (2026-04-06)


### Features

* add dismiss Google Play error dialog patch and remove obsolete license check patch ([7ba9dff](https://github.com/rabilrbl/fluffy-patches/commit/7ba9dff920100ffaa639b95d94c351e29a464137))

# [1.0.0-dev.11](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.10...v1.0.0-dev.11) (2026-04-05)


### Features

* Enhance APK analysis documentation and add JADX CLI usage instructions across multiple files ([c58ab08](https://github.com/rabilrbl/fluffy-patches/commit/c58ab086e6b18d0dea5bf5e704632872f92ccb3b))

# [1.0.0-dev.10](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.9...v1.0.0-dev.10) (2026-04-05)


### Features

* Add new commands and skills for OpenSpec workflow ([7f98188](https://github.com/rabilrbl/fluffy-patches/commit/7f981887fbfbd965bf4085f79b16a850bb33fc39))

# [1.0.0-dev.9](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.8...v1.0.0-dev.9) (2026-04-04)


### Bug Fixes

* bypass AppDataManager.inu flag and remove broken constructor no-op ([e78b041](https://github.com/rabilrbl/fluffy-patches/commit/e78b0411bfb0f5431a2281dbe116a66e36a2dc4a))

# [1.0.0-dev.8](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.7...v1.0.0-dev.8) (2026-04-03)


### Bug Fixes

* inject setCheckAppUpadteData(null) at start of HomeActivity.onCreate and no-op onResume ([77fb430](https://github.com/rabilrbl/fluffy-patches/commit/77fb430c0ec5bf87d3ab2185b51b38b8f1d9e8fb))

# [1.0.0-dev.7](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2026-04-03)


### Bug Fixes

* add AppUpdateHelper constructor no-op and AppUpdateManagerFactory.create null return ([e795f5f](https://github.com/rabilrbl/fluffy-patches/commit/e795f5fda0ac45b86d5716c0b43cffa363f95234))

# [1.0.0-dev.6](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2026-04-03)


### Bug Fixes

* bypass app update checks by modifying CommonUtils and AppUpdateHelper methods ([4c43c6d](https://github.com/rabilrbl/fluffy-patches/commit/4c43c6de1bb56cb789c4fb33a23feb4f278c7624))

# [1.0.0-dev.5](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2026-04-03)


### Bug Fixes

* bypass AppUpdateHelper methods to prevent update checks ([1790d4b](https://github.com/rabilrbl/fluffy-patches/commit/1790d4bef48bcddd19539b7bf1460061f126193b))

# [1.0.0-dev.4](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2026-04-03)


### Bug Fixes

* stop neutralizing pairip VM execution to prevent splash crash ([a3b4752](https://github.com/rabilrbl/fluffy-patches/commit/a3b4752ef82e79e229ccaed1084d7a364a989fa6))

# [1.0.0-dev.3](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2026-04-03)


### Bug Fixes

* correct smali class names and method types for JioTV patches ([d9bef48](https://github.com/rabilrbl/fluffy-patches/commit/d9bef48cbd13c3f839ebae35e9144f9f1acc3cba))

# [1.0.0-dev.2](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2026-04-03)


### Bug Fixes

* rebuild all JioTV patches from scratch using JADX analysis ([b7bc14c](https://github.com/rabilrbl/fluffy-patches/commit/b7bc14ca28406890b93127d1641e27ae335ae7e0))

# 1.0.0-dev.1 (2026-04-03)


### Bug Fixes

* **patches:** use mutable methods for instruction injection ([571fb6a](https://github.com/rabilrbl/fluffy-patches/commit/571fb6a85fa31a9b1dd5a0d96cb2408a4a06eb7c))


### Features

* **patches:** add JioTV patch set for checks and networking ([2bbe390](https://github.com/rabilrbl/fluffy-patches/commit/2bbe390ff30c8e11ce70414e56a92270bbea5ddd))
* **patches:** correct application class package in disablePairipManifestPatch and enhance Play Store license check patch with detailed comments ([5809024](https://github.com/rabilrbl/fluffy-patches/commit/5809024ecbe17a62e805c55e5112fb21a4fa6f71))
* **patches:** enhance license check bypassing in RemovePlayStoreLicenseCheckPatch ([3f835bb](https://github.com/rabilrbl/fluffy-patches/commit/3f835bb5ff6bb0549c63cda4a88f582797178449))
* **patches:** enhance license check bypassing in RemovePlayStoreLicenseCheckPatch ([a1d782b](https://github.com/rabilrbl/fluffy-patches/commit/a1d782b2406543858810127ae31612552c3dab40))
* **patches:** enhance license check patch to block LicenseClient methods and bypass local installer check ([a839ae9](https://github.com/rabilrbl/fluffy-patches/commit/a839ae99d12d406a40b6bda1817fbb55584fd758))
* **patches:** enhance license check patch to block service connections and spoof InstallReferrerClient ([4823107](https://github.com/rabilrbl/fluffy-patches/commit/4823107ee0aa713ff6f1fa6b5c2d2caf100e16da))
* **patches:** enhance license check patch to remove LicenseContentProvider and block licensing service methods ([56ceb66](https://github.com/rabilrbl/fluffy-patches/commit/56ceb6626d7c7dec8742827822e005e462284d78))
* **patches:** enhance Play Store license check patch to block additional methods and prevent initialization ([e20e6cd](https://github.com/rabilrbl/fluffy-patches/commit/e20e6cd5a0351bee3184c11194e41779270f7f4a))
* **patches:** implement global manifest-level disable of pairip license checks ([6f0c1be](https://github.com/rabilrbl/fluffy-patches/commit/6f0c1beee13cb0ab51c81d9a6ad34a2c8f62c003))
* **patches:** refine license check bypass and enhance root detection patch safety ([859cacb](https://github.com/rabilrbl/fluffy-patches/commit/859cacb60897e78f2311ae6708f3a963aa9331d0))
* **patches:** simplify emulator detection patch by removing redundant checks and ensuring proceedApplication is always called ([809fa2d](https://github.com/rabilrbl/fluffy-patches/commit/809fa2d56b81e4d1927fbbad2cee62c279ad67e5))
* **patches:** update application class package in RemovePlayStoreLicenseCheckPatch to bypass pairip ([1d8774c](https://github.com/rabilrbl/fluffy-patches/commit/1d8774c449e1f83714fee516a9e0cdc82654634c))
* **patches:** update compatibility constant to JioTV Mobile across patches ([8fb3db8](https://github.com/rabilrbl/fluffy-patches/commit/8fb3db8cd09af8370a7ae98a3d8d3b746515f4dc))
* **patches:** update disablePairipManifestPatch to remove LicenseContentProvider and change application class to bypass pairip ([3df0d3d](https://github.com/rabilrbl/fluffy-patches/commit/3df0d3dc833a04d39f7700f72bb1bed817106920))
* **playstore:** enhance license check patch to prevent loading of pairipcore library ([0886241](https://github.com/rabilrbl/fluffy-patches/commit/0886241c102ec81fc15a57e5ae85e23408dfbc89))
