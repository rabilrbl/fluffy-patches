# Alarmy Patches

## App Info

- **Package:** `droom.sleepIfUCan`
- **Name:** Alarmy
- **Version:** 26.23.0
- **File Type:** XAPK

## Architecture

Alarmy uses a local `PremiumState` data class (`pi/b`) to track subscription status. This class is heavily obfuscated but retains Kotlin metadata. The app syncs subscription state from a server but gates all premium UI and features through the local `PremiumState` object.

### Key Classes

| Smali | Kotlin Metadata | Purpose |
|-------|-----------------|---------|
| `pi/b` | `PremiumState` | Core premium state data class |
| `pi/d` | `PremiumType` | Enum: GOOGLE_SUBSCRIPTION, DELIGHTROOM_SUBSCRIPTION, REMOVE_AD_SUBSCRIPTION, LIFETIME, PLAYPASS, NONE, MANUAL |
| `pi/c` | `PremiumStateType` | Enum: ACTIVE, ACCOUNT_HOLD, CANCEL, EXPIRED, FAIL |
| `th/b` | `UserSubscriptionStatusDelegatorImpl` | Repository that fetches and caches premium state |
| `gj/a` | `UserSubscriptionStatusDelegator` | Interface for accessing premium state |

### Premium Check Methods

All premium gating flows through `pi/b`:

| Smali Name | Kotlin Name | Logic |
|------------|-------------|-------|
| `r()` | `isPremium()` | Returns true if any premium type is active (lifetime, playpass, google, manual, delightroom) |
| `s()` | `isRemoveAdPremium()` | Returns true if premium type is REMOVE_AD_SUBSCRIPTION and not expired |
| `o()` | `isLifetimePremium()` | Returns true if premium type is LIFETIME |
| `q()` | `isPlayPassPremium()` | Returns true if premium type is PLAYPASS |
| `n()` | `isGooglePremium()` | Returns true if premium type is GOOGLE_SUBSCRIPTION and not expired |
| `p()` | `isManualPremium()` | Returns true if premium type is MANUAL and not expired |
| `k()` | `isDelightroomSubscriptionPremium()` | Returns true if premium type is DELIGHTROOM_SUBSCRIPTION and not expired |

## Patches

### Unlock Pro subscription

Patches `pi/b.r()` to always return `true`. This makes the app treat the user as having an active premium subscription regardless of actual purchase state.

### Remove ads

Patches `pi/b.s()` to always return `true`. This specifically bypasses the ad-removal subscription check.

## Notes

- The app uses Google Play Billing (`com.android.vending.BILLING` permission) for in-app purchases.
- Subscription state is cached locally in `PremiumStatePreferences` (`zg/h`).
- Patching the local state class is sufficient to unlock UI and features; server-side validation only affects receipt validation for new purchases.
