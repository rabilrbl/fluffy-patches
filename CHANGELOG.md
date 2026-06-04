# [1.0.0-dev.19](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.18...v1.0.0-dev.19) (2026-06-04)


### Bug Fixes

* **alarmy:** remove pairip bypass patches and docs ([9178d49](https://github.com/rabilrbl/fluffy-patches/commit/9178d49f0a2cdbe1cc9110781ca61eda1ea243d5))

# [1.0.0-dev.18](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.17...v1.0.0-dev.18) (2026-06-04)

# [1.0.0-dev.16](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.15...v1.0.0-dev.16) (2026-06-03)

# [1.0.0-dev.15](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.14...v1.0.0-dev.15) (2026-06-03)


### Features

* **alarmy:** add unlock pro and remove ads patches ([e23da2c](https://github.com/rabilrbl/fluffy-patches/commit/e23da2ce2d3e21290375f663509d8e50909466d8))

# [1.0.0-dev.14](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.13...v1.0.0-dev.14) (2026-04-06)

# [1.0.0-dev.13](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.12...v1.0.0-dev.13) (2026-04-06)


### Features

* add jiotv-base-patched-temporary-files to .gitignore ([fa335d8](https://github.com/rabilrbl/fluffy-patches/commit/fa335d8053f19263166372a2bc2c11ee6eafedd3))
* add play store disabled log for debugging purposes ([436f19e](https://github.com/rabilrbl/fluffy-patches/commit/436f19ee23f287b75d028cdc645ab0d34553d5f8))
* update dismiss Google Play error dialog patch to prevent dialog from appearing on license check failure ([fca2782](https://github.com/rabilrbl/fluffy-patches/commit/fca27827d54ba84e0c5c55c62cf068121d836cb0))

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

* inject setCheckAppUpaduteData(null) at start of HomeActivity.onCreate and no-op onResume ([77fb430](https://github.com/rabilrbl/fluffy-patches/commit/77fb430c0ec5bf87d3ab2185b51b38b8f1d9e8fb))

# [1.0.0-dev.7](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2026-04-03)


### Bug Fixes

* add AppUpdateHelper constructor no-op and AppUpdateManagerFactory.create null return ([e795f5f](https://github.com/rabilrbl/fluffy-patches/commit/e795f5fda0ac45b86d5716c0b43cffa363f95234))

# [1.0.0-dev.6](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2026-04-03)


### Bug Fixes

* bypass app update checks by modifying CommonUtils and AppUpdateHelper methods ([4c43c6d](https://github.com/rabilrbl/fluffy-patches/commit/4c43c6de1bb56cb789c4fb33a23feb4f278c7624))

# [1.0.0-dev.5](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2026-04-03)


### Bug Fixes

# [1.0.0-dev.4](https://github.com/rabilrbl/fluffy-patches/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2026-04-03)

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
