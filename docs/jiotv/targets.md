# JioTV Targets

Keep these two tracks separate.

## Track A: 371 split baseline

- Source style: latest Uptodown XAPK / split install
- Tested package on AVD: `versionName 7.1.7`, `versionCode 371`
- Signature path: original signed splits launch, re-signed splits crash natively
- Pairip flavor: `licensecheck3` / `LicenseClientV3`
- Practical route: runtime hooks, installer spoofing, or other non-re-sign approaches

## Track B: 404 merged / antisplit history

- Source style: older APKMirror merged / antisplit APK workflows
- Older docs refer to `7.1.7 (404)` and `licensecheck/*`
- Useful for historical class mapping and failed experiments
- Do not copy VM-disabling edits from this branch into the 371 split track

## Cleanup rule

Before adding or changing a patch, write down which track it belongs to:

- `371 split` if it targets the currently working original split install
- `404 merged` if it depends on older antisplit / merged APK assumptions

If a change does not clearly belong to one track, stop and split the work first.
