# Alarmy Patches

**Target:** Alarmy (`droom.sleepIfUCan`) v26.23.0 → v26.32.1

## Patch Status

### Working Patches (v26.32.1)
- ✅ Unlock Pro subscription (`subscription/UnlockProPatch.kt`)
- ✅ Remove ads (`subscription/RemoveAdsPatch.kt`, `ads/RemoveAdsPatch.kt`)

### Removed Patches
- ❌ Unlock Premium (`premium/UnlockPremiumPatch.kt`) - `ty.a.a()` returns null, not boolean

## Version Changes

### v26.32.1 (2026-08-18)
**Class name change:** `bi.c` → `bi.PremiumState`

The obfuscated class `bi.c` was renamed to `bi.PremiumState` in v26.32.1. Methods remain the same:
- `r(): Boolean` - checks if user has any premium state
- `s(): Boolean` - checks if user has remove-ad premium

**Patches updated:**
1. `subscription/UnlockProPatch.kt` - target changed to `Lbi/PremiumState;`
2. `subscription/RemoveAdsPatch.kt` - target changed to `Lbi/PremiumState;`
3. `ads/RemoveAdsPatch.kt` - target changed to `Lbi/PremiumState;`

## APK Architecture

Alarmy v26.32.1 is distributed as XAPK with multiple split APKs:
- `base.apk` - main application
- `split_config.arm64_v8a.apk` - native libraries
- Multiple split APKs for different configurations

**Testing:** Use the antisplit merged APK for patching.

## Premium State Flow

```kotlin
// bi.PremiumState class
public final boolean r() {  // isPremium
    return o() || q() || n() || p() || k();
    // lifetime || playpass || google || manual || delightroom
}

public final boolean s() {  // isRemoveAdPremium
    return !m() && this.premiumType == e.REMOVE_AD_SUBSCRIPTION;
}
```

## Testing Notes

**v26.32.1 Testing (2026-08-18):**
- Patches compile successfully
- Target class `bi.PremiumState` confirmed present in APK
- Methods `r()` and `s()` signatures verified
- Manual testing required via Morphe Manager

## References

- [MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)
- JADX decompilation: `~/Downloads/Backups/droom.sleepIfUCan v26.32.1_antisplit.apk.jadx`
