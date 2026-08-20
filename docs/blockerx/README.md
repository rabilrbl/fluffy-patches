# BlockerX Patches

**Target:** BlockerX (`io.funswitch.blocker`) v5.0.81

## Premium state analysis

The APK uses RevenueCat entitlements and mirrors entitlement state into `BlockerXAppSharePref`.
The primary entitlement names observed in the APK are `BlockerX premium access`, `BlockerX premium lite`, and `tier_premium`.
The app also exposes dedicated local gates for premium modules.

## Patch

`premium/EnablePremiumPatch.kt` replaces these boolean getters in the runtime class `Lio/funswitch/blocker/utils/sharePrefUtils/BlockerXAppSharePref;`:

- `getSUB_STATUS()Z`
- `getSUB_STATUS_LITE()Z`
- `getIS_ACTIVE_CODI_MODE_PREMIUM()Z`
- `getIS_ACTIVE_DESKTOP_PREMIUM()Z`
- `getIS_ACTIVE_ED_COURSE_PREMIUM()Z`
- `getIS_ACTIVE_PREMIUM_PLUS()Z`
- `getIS_ACTIVE_URGES_MODE_PREMIUM()Z`

Each method is fully replaced with `true`, preventing the preference delegate from supplying the original value during the patched app process.

This patch changes client-side gates only. Server-side entitlement validation and purchase APIs remain unchanged.

## APK analysis evidence

- Package: `io.funswitch.blocker`
- Version: `5.0.81` / version code `5081`
- RevenueCat SDK classes are present.
- Premium checks are present in `androidx/compose/foundation/text/BasicTextFieldKt$$ExternalSyntheticLambda3` and `io/funswitch/blocker/utils/revenuecatUtils/RevenuecatSDKOperation$$ExternalSyntheticLambda5`.
- The decoded target was `/tmp/funswitch-apktool`; patch against the antisplit APK.

## Testing

Build the patch package, apply it to the target APK with Morphe Manager or CLI, then verify premium-gated modules on a test device. No device was available during repository verification.
