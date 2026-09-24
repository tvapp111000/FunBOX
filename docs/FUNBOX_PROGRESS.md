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

## Stage 2 — built-in TV provider and Clipbox mapping

- Added an idempotent first-launch provider initializer using StreamVault's existing M3U setup and sync path. It registers `FunBOX / עידן פלוס` at `http://tiny.cc/FanTV` and restores an existing user's active provider after initialization.
- Removed the first-run requirement to visit provider setup; startup now proceeds to Home while provider sync runs.
- Confirmed in source that StreamVault's M3U parser reads `tvg-id`, `tvg-name`, `tvg-logo`, `group-title`, `url-tvg`, and `x-tvg-url`; its sync path assigns playlist header EPG URLs to the provider. OkHttp is configured to follow redirects.
- Added `docs/CLIPBOX_INTEGRATION_AUDIT.md` with findings from the authorized APK audit and explicitly unverified flows.

### Verification and limitations

- `http://tiny.cc/FanTV` could not be fetched from this Windows environment (request timed out); channel import and playback are not verified.
- `assembleDebug` was attempted with JDK 21 and JDK 17. Both stop before project compilation because Gradle's generated accessor compiler receives `java.nio.file.AccessDeniedException` on a local Gradle JAR. The same failure reproduces with standalone `javac -cp` on that JAR. The sandbox policy rejected an unsandboxed build request. No APK is available yet.
- `testDebugUnitTest`, emulator playback, and TV focus validation remain pending.

### Next

1. Resolve or bypass the local Gradle/JDK filesystem issue through an authorized build environment, then compile and fix source errors.
2. Complete native Clipbox integration after live API and authentication contracts are validated; do not substitute unrelated scraping providers.
3. Complete unified navigation, favorites, search, settings, player, and RTL focus checks.

## Stage 3 — update channel safety

- Replaced the upstream StreamVault release check with a configurable FunBOX GitHub repository. The repository setting is intentionally blank until a FunBOX release destination exists.
- Disabled automatic release checks on a fresh installation while no FunBOX release repository is configured.
- Changed update download labels and filenames to FunBOX, and the Android TV input service label to FunBOX.
- Added a placeholder in `local.properties.example`; it contains no credential.

### Verification and next steps

- Review of the changed release URL and assets was static. Gradle remains blocked before project compilation by the local Java/JAR access restriction described above.
- Continue with the requested primary navigation and native Clipbox integration. Set `funbox.release.repository` only after the user identifies the FunBOX repository.

## Stage 4 — splash, locale completeness, and GitHub fork

- Wired the supplied FunBOX splash artwork into the launch window without enlarging the 1280×720 image.
- Added the 17 Hebrew string translations missing from the app resource bundle, and changed Android TV input setup copy to FunBOX across localized strings.
- The user authorized a fork in their account; `https://github.com/tvapp111000/FunBOX` was created from StreamVault `master` and configured as the app's default release repository.
- Read-only probes of both Clipbox host candidates returned HTTP 403 from this environment. The API contract, catalog responses, playback, and authentication remain unverified.

### Verification and next steps

- Parsed all edited XML resource files and ran `git diff --check` successfully. No emulator check yet.
- A fresh local `assembleDebug` attempt still stops during Gradle generated-accessor compilation with `AccessDeniedException` when closing a Gradle JAR; no project source has compiled locally.
- Commit and push the branch to the fork, inspect GitHub Actions, then fix any reported build failures. Continue native Clipbox implementation only against verified authorized endpoints.

## Stage 5 — visible branding and Clipbox authentication boundary

- Changed remaining visible product text in the navigation shell, failure reporting, recording notifications, provider pairing, Cast default title, and plugin installation screens to FunBOX. Internal plugin service names and provider protocol identifiers remain compatible with StreamVault.
- Further APK inspection confirmed that Clipbox's own catalog uses TMDB data and that its private service requests include an app key plus a dynamic integrity header. Values were not logged or committed.
- A credential-bearing service probe was automatically rejected because permission to transmit the recovered key to the particular remote hosts was not established. No key was transmitted. Continue the IPTV and UI work while this access requirement remains unresolved.
- Four local commits exist. The fork exists, but pushing currently needs a GitHub CLI authorization; a narrow user decision is pending.

## Stage 6 — FunBOX primary navigation and authorized Clipbox probe

- Added the seven requested primary destinations in order: Home, TV, Movies, Series, Favorites, Guide, Search. Settings remains available from the shell header. The existing saved library is now reachable from the Favorites route, while legacy downloads and plugins remain accessible through existing flows.
- Kept separate Movies and Series destinations for the FunBOX shell even when a provider uses StreamVault's unified VOD layout. Updated navigation route and unit test expectations. The catalog screens still use StreamVault data and have **not** been represented as Clipbox integration.
- The user explicitly authorized credential-bearing tests only against the original Clipbox hosts found in the APK. A restricted HTTPS probe to `clipbox.mov` returned HTTP 200 from `/api/config`; the fallback `superbox.mov` returned HTTP 526. Catalog/auth requests to `clipbox.mov` returned HTTP 403 with an integrity-class rejection, so the app integrity signature algorithm is being reconstructed. No credential value was printed or committed.

### Verification and remaining work

- `git diff --check` passes. Source compilation and unit tests are still blocked locally by the Gradle/JAR access error before project compilation; navigation changes need CI compilation and device validation.
- Implement verified Clipbox authentication and native catalog flows, source selection and playback; finish provider selection and state adapters; run build, tests, emulator checks and CI; push branch and open PR.

## Stage 7 — signed Clipbox service adapter

- Reproduced Clipbox's HMAC request signature from the authorized APK without logging or committing the static app key or signing digest. A signed service request passes the integrity gate: `/api/config` returns the complete config and `/api/auth/me` returns 401 specifically because no user token is available.
- Added a dedicated `ClipboxApi` data-layer adapter and Hilt configuration. It allows credentials only on the three HTTPS hosts listed by the APK and disables redirects; the account check keeps user authentication separate. Local credentials are in ignored `local.properties`. CI can provide `CLIPBOX_API_KEY` and `CLIPBOX_SIGNING_DIGEST` as secrets.
- Confirmed that the Clipbox Home, Movies, Series, details, and search catalog comes from TMDB using a key in signed Clipbox config. Because the user's host restriction forbids transmitting that key to TMDB under the current authorization, destination-specific approval is pending. No catalog request has been made using that key.

### Verification and remaining work

- Live signed request statuses were checked against the original Clipbox host; no key, signature, token, or full response body was logged. `git diff --check` passes.
- Integrate the Clipbox catalog and playback once the permitted host boundary is clarified. Local Gradle still cannot compile before source compilation due to the JAR access error; CI push requires GitHub CLI authorization.
