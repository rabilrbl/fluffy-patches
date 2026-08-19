# Alarmy Patches

**Target:** Alarmy (`droom.sleepIfUCan`) v26.32.1

## Patch Status

### v26.32.1
- `premium/UnlockPremiumPatch.kt` unlocks premium and disables ads.
- Premium and remove-ad checks are patched together because the app gates ad initialization on either check.
- Separate Ads and Subscription patches were removed to avoid overlapping patch definitions.

## Version Details

The runtime class remains the obfuscated `bi.c` (`Lbi/c;`) in v26.32.1. JADX's display name `bi.PremiumState` is not a valid runtime descriptor.

Patched methods:
- `r():Z` — premium entitlement gate
- `s():Z` — remove-ad entitlement gate

Both method bodies are replaced, rather than prepended, so the original logic cannot overwrite the forced result.

## APK Architecture

Alarmy v26.32.1 is distributed as XAPK with multiple split APKs:
- `base.apk` — main application
- `split_config.arm64_v8a.apk` — native libraries
- Multiple split APKs for different configurations

**Testing:** Use the antisplit merged APK for patching.

## Premium State Flow

```kotlin
// Runtime descriptor: Lbi/c;
public final boolean r() { /* premium entitlement */ }
public final boolean s() { /* remove-ad entitlement */ }
```

The consolidated Premium patch forces both methods to `true`, which also prevents the startup ad initializer from running.

## Testing Notes

**v26.32.1 analysis:**
- Original smali verified in `classes18.dex` at `bi/c.smali`.
- Original methods `r()Z` and `s()Z` verified.
- The previous generated APK still contained the original method bodies because `addInstructions(0, ...)` only prepended instructions; this caused the runtime result to remain unchanged.
- The corrected patch replaces both method bodies before adding the forced return.

## References

- [MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)
