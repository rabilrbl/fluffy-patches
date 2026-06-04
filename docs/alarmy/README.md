# Alarmy Patches

## App Info

- **Package:** `droom.sleepIfUCan`
- **Name:** Alarmy
- **Version:** 26.23.0
- **File Type:** XAPK

## Architecture

Alarmy uses a local `PremiumState` data class (`pi/b`) to track subscription status. Patching the local state class is sufficient to unlock UI and features; server-side validation only affects receipt validation for new purchases.

### Premium State

| Smali | Kotlin Name | Logic |
|-------|-------------|-------|
| `r()` | `isPremium()` | Returns true if any premium type is active (lifetime, playpass, google, manual, delightroom) |
| `s()` | `isRemoveAdPremium()` | Returns true if premium type is REMOVE_AD_SUBSCRIPTION and not expired |
| `o()` | `isLifetimePremium()` | Returns true if premium type is LIFETIME |
| `q()` | `isPlayPassPremium()` | Returns true if premium type is PLAYPASS |
| `n()` | `isGooglePremium()` | Returns true if premium type is GOOGLE_SUBSCRIPTION and not expired |
| `p()` | `isManualPremium()` | Returns true if premium type is MANUAL and not expired |
| `k()` | `isDelightroomSubscriptionPremium()` | Returns true if premium type is DELIGHTROOM_SUBSCRIPTION and not expired |

## Patches

### Premium Patches

| Patch | Target | What it does |
|-------|--------|--------------|
| Unlock Pro subscription | `PremiumState.isPremium()` | Forces the method to always return `true`, unlocking all premium features |
| Remove ads | `PremiumState.isRemoveAdPremium()` | Forces the method to always return `true`, disabling ads |

## Notes

- The app uses Google Play Billing (`com.android.vending.BILLING` permission) for in-app purchases.
- Subscription state is cached locally in `PremiumStatePreferences` (`zg/h`).
- Patching the local state class is sufficient to unlock UI and features; server-side validation only affects receipt validation for new purchases.