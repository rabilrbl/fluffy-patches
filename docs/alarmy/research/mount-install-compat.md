# Alarmy mount-install compatibility audit

Issue scope: `rabilrbl/fluffy-patches#19` (cross-posted from `MorpheApp/morphe-patches#1956`).

## Summary

- Current bundle patches for Alarmy only force local premium booleans in `PremiumState` (`Lpi/b;` methods `r()` and `s()`).
- No patch in this repository modifies Alarmy's package signature checks, Google Sign-In flow, Google Drive backup flow, Play Integrity handling, or MicroG prerequisites.
- Because of that, a mount-install specific compatibility pass is **not expected to improve PairIP bypass** for Alarmy by itself.

## Mount-install compat checklist

| Check | Result | Notes |
|------|--------|-------|
| Signature-check strip needed for Google Sign-In / backup | Not required by current patches | No signature verification logic is touched by `Unlock Pro subscription` or `Remove ads`. |
| MicroG-RE prerequisite | Not applicable in current patch scope | Patches do not hook Play Services auth APIs. |
| Play Integrity fallback | Not implemented | Current patches do not interact with integrity APIs. |
| Known mount-install regressions in this bundle | None known right now | No app-specific mount breakage reports for current Alarmy patch set. |
| PairIP bypass impact | No direct impact | PairIP bypass is separate from this mount-compat audit scope. |
