# FunBOX progress

## Stage 1 — foundation and branding

- Checked the supplied `clipbox.apk` and seven PNG branding images from `logo.rar`.
- Cloned StreamVault `master` and created `funbox-full-integration`.
- Read `LICENSE` and `AGENTS.md` before modifying code. The derivative must preserve visible attribution, both original links, the exact license, public source on distribution, and non-commercial use.
- Replaced launcher artwork, adaptive icon foreground, Android TV banner, and added FunBOX logo, splash and background resources from the supplied images.
- Set Hebrew as the first-install language in the preferences flow and initial Compose state; the existing locale logic sets RTL for Hebrew.
- Changed the application label to FunBOX in localized app-name resources and added "Based on StreamVault" to About with the required original repository and Ko-fi links.

### Verification

- Inspected the source artwork and generated resource dimensions.
- Static source checks only so far. Build, tests, emulator playback, focus, and UI inspection remain pending.

### Next

1. Finish the authorized Clipbox APK audit without committing credentials or decompiled source.
2. Add the built-in FanTV provider and verify automatic EPG header handling.
3. Integrate native Clipbox Home, Movies, Series, details, state, search, and playback where the audited contracts permit.
4. Complete navigation, settings, Android TV focus and RTL checks.
5. Run Gradle build/tests, produce an APK, and open a PR when repository access permits.

## Boundaries

- No Clipbox network flow is marked integrated until it returns data and the requested screens and playback are verified.
- The original StreamVault license remains in the repository.
