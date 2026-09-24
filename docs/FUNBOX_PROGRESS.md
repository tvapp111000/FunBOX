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

## Stage 8 — live Clipbox catalog contract verified

- Confirmed account login/registration returns a user token and user ID; the token is stored in encrypted preferences. The APK's guest browsing does not require a guest token. No app-owned refresh endpoint was found, and device reporting does not issue a session.
- With the user's renewed permission, verified the signed Clipbox config and its TMDB catalog key without printing either credential. On the official TMDB API host, Home trending, movie discover, TV discover, and multi-search each returned HTTP 200 and 20 items. Movie details, TV details, season, and episode returned HTTP 200. Account token was **not** needed for those catalog endpoints.
- The first attempt to probe the APK-listed fallback host `api.clipbox.mov` was rejected by automatic approval review because that exact hostname was not named in the user's prior permission. The user then explicitly authorized it; a subsequent restricted probe succeeded. No secret was sent to any other destination.
- Gradle's base-services JAR has a normal ACL, is readable and copyable, and no Java process currently holds it. The source of Java's `ZipFileSystem.close` AccessDenied remains under investigation.

### Next

1. Build a native Clipbox catalog repository with the verified endpoints and bind Home, Movies, Series, details, seasons/episodes, and search.
2. Continue source/playback and account state flows separately; do not claim end-to-end Clipbox integration yet.
3. Isolate the Gradle/JDK file operation failure and obtain an APK locally or through CI, then push and open the PR.

## Stage 9 — native Clipbox catalog wiring and Gradle diagnosis

- Added `ClipboxCatalogRepository` in the data layer. It obtains the short-lived catalog key from signed Clipbox config, calls only `api.themoviedb.org`, blocks redirects, parses Home trending, movie/series pages, search, details, seasons, and episodes, and keeps recent pages in memory.
- Added Compose/ViewModel screens for Clipbox Home, Movies, Series, movie/series details, season selection and episode metadata. The three primary destinations now point at this native Clipbox data flow. New Clipbox detail routes are separate from existing StreamVault VOD detail routes.
- Episode source selection, playback, saved state, and unified search are **not** wired yet. The screens and repository need a successful build and on-device check before they can be marked complete.
- Traced the local Gradle error to Java `Path.toRealPath()` on sandbox-created files: it fails on the original JAR, a copy under the workspace, and a copy under Temp despite normal ACLs. A narrowly escalated `assembleDebug` run has passed the previous immediate failure and is still running at this checkpoint.

### Verification and next

- The live upstream catalog contract was verified in Stage 8; the new Kotlin layer has only static checks at this checkpoint. `git diff --check` passes.
- Finish the build, fix compiler errors, run unit tests, then implement unified search and legal source/playback flows. Push and open PR when GitHub authentication is available.

## Stage 10 — build and GitHub access checkpoint

- GitHub CLI device authorization for the user's `tvapp111000` account completed. The branch is still local at this checkpoint; push and PR remain to be done after build fixes.
- The escalated local Gradle run passed the previous JDK file access failure and reached Android resource linking. AAPT found an invalid literal color in the splash drawable; changed it to a valid shape fill. A new `assembleDebug` run is in progress.
- Corrected the Clipbox Compose screens' Hilt ViewModel import to the API used by the project. Neither compilation nor an APK has been verified yet.
- The user explicitly requires a signed, installable debug APK as a final deliverable. Verify the APK contents, signature, and Android TV manifest before reporting completion; install and launch through ADB if a device or emulator is available.

## Stage 11 — fork PR, APK CI, and unified search

- Pushed `funbox-full-integration` to `tvapp111000/FunBOX` and opened draft PR #1. Added a CI workflow that builds and uploads a signed debug APK. Its first run failed at Android SDK setup because the action requested the removed `tools` package; specifying supported SDK packages fixed setup, and new runs are in progress.
- Connected global search to the verified Clipbox catalog. IPTV channel matches still come from the active StreamVault provider; movie and series sections now come from Clipbox's signed-config/TMDB catalog and link to the new native detail routes. This code is pending compilation and device validation.
- Local `assembleDebug` has passed Android resource linking and the previous JDK access failure. No APK exists yet at this checkpoint. ADB is installed but no device is attached and no emulator system image is installed locally.

## Stage 12 — Clipbox guest library

- Added local guest favorites and watchlist state for Clipbox titles, with separate flags and private app storage. The native detail screen now offers favorite and watchlist actions, and the existing unified Favorites destination shows saved Clipbox movies, series, and watchlist beside StreamVault saved items.
- This guest state is deliberately separate from Clipbox account synchronization. No account session was supplied or created, and remote sync behavior is not yet verified. Continue watching and watched episode state remain open.
- Changes are awaiting an incremental build and UI validation. The first local full build is still finishing other app tasks; no APK is claimed yet.

## Stage 13 — first complete compiler pass and fix

- The first local `assembleDebug` compiled the data and feature modules, then failed in `app:compileDebugKotlin` because the shell imported `androidx.compose.ui.unit.size` instead of the layout modifier extension. Fixed the import; CI reported the same source error.
- A CI run also found configuration-cache incompatibilities in inherited project validation tasks. The FunBOX APK workflow now explicitly runs without Gradle's configuration cache.
- A new local build is running with the navigation fix and the guest library/search changes. The APK remains pending and must be verified after a successful build.

## Stage 14 — live default playlist check

- Fetched the user-supplied `http://tiny.cc/FanTV` URL with redirects. It returned a valid M3U with 14 channel entries, `tvg-id`, `tvg-logo`, and a `url-tvg` header. The advertised XMLTV URL returned HTTP 200 and about 7.4 MB of guide data.
- The playlist currently has no `group-title` fields, so it cannot provide source-based categories beyond the default all-channels category. This is a source-data limitation, not a parser claim.
- Stream playback, guide matching, and on-device first-run behavior are still unverified; ADB has no connected device or installed emulator image.
