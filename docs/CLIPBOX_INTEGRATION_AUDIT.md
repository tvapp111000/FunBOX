# Clipbox APK integration audit

Source: user-supplied `clipbox.apk` (analyzed locally; APK and decompiled output are not committed). This is a technical audit with implementation status updated after the native catalog work. No credential values are recorded here.

## Package and platform

| Item | Finding |
| --- | --- |
| Package | `org.clipbox.player` |
| Version | 1.6.1 (`versionCode` 161) |
| Android | min SDK 24, target SDK 35 |
| Entry | `ServerGateActivity` is the launcher and Leanback entry point |
| TV screens | `TvHomeActivity`, `TvDetailActivity`, `TvCategoryActivity`, `TvSearchActivity`, `TvMyListActivity`, `TvSettingsActivity`, `TvAccountActivity`, `TvAboutActivity` |
| Other screens | browse, detail, player, search, my list, history, settings, account, support, register, update |
| Manifest components | App activities, FileProvider, media/cast and AndroidX WorkManager components, receivers and initializers |
| Permissions | Network, foreground media playback, notifications, storage compatibility, boot receive, package queries, install packages, and others documented in the APK manifest |

## UI and navigation

The TV experience uses Android XML layouts and view based activities, with separate TV layouts such as `tv_activity_home`, `tv_hero`, `tv_poster_cell`, `tv_rail`, `tv_activity_detail`, and `tv_episode_cell`. This is not a Compose UI that can be copied into StreamVault. The TV home has a hero and rows including trending, popular movies, popular TV, genres, and Continue Watching. The detail flow uses TV specific controls, seasons, episodes, and watched state. Search has movie/series filters and D-Pad focus rules. Native Compose recreation is required for a single FunBOX UI.

## Data and network

The APK contains `TmdbMovie`, `TmdbDetail`, `TmdbImages`, episode, cast and extra metadata models. The observed home, detail, and search activities use these models. Static call sites show that Clipbox itself obtains catalog data from TMDB paths including `/trending/all/week`, `/discover/movie`, `/discover/tv`, `/search/multi`, `/movie/`, and `/tv/`. Its TMDB credential is populated from remote configuration; it is not a reusable literal in the catalog class. Recreating the Clipbox catalog must follow its actual filters and presentation rather than substitute an unrelated catalog.

Clipbox also uses its own remote service gate with fallback hosts, an `/api/config` request, account authentication endpoints (`/api/auth/...`), and synchronized state endpoints for favourites, watchlist, history, resume, and settings. The networking interceptor adds `User-Agent`, `X-App-Version`, `X-App-Key`, and a dynamically generated `X-App-Integrity` header. A static app key is present in the APK, but its value is deliberately absent here. After the user explicitly authorized credential-bearing tests to the original APK hosts, a restricted probe of `https://clipbox.mov/api/config` returned HTTP 200 with maintenance, update, and server-time fields. The fallback `https://superbox.mov/api/config` returned HTTP 526; TLS was not bypassed. The signed `/api/config` request returned the complete configuration, including a nonempty `tmdb_api_key` field, without printing its value. `/api/auth/me` then returned HTTP 401 for a missing account token, confirming the app signature passed the integrity gate. No user account token was found or invented. Sample `/api/movie/...` and `/api/tv/...` requests returned 404; those paths are part of a separate source module, not Clipbox's Home catalog. The APK also includes source resolver modules under a separate bundled package; their third party hosts are not assumed to be authorized for FunBOX and have not been copied.

`ServerGateActivity` runs a remote configuration check before entering TV browsing. The signature is HMAC-SHA256 over the APK signing certificate digest, app version, and server-adjusted current minute. An account token is obtained through `/api/auth/login` (username, password, optional locally generated UUID `deviceId`) or registration; the response has `token` and `userId`. Clipbox stores the token in encrypted SharedPreferences and sends it as Bearer for account and sync requests. `/api/devices/report` is a device report, not guest token issuance. No guest/anonymous token or refresh endpoint was identified in the app-owned auth client; guest UI represents browsing without an account. No account authentication bypass has been implemented.

The primary Clipbox catalog is fetched from TMDB using a key delivered in signed Clipbox configuration. After destination-specific user approval, live requests using that key **only to the official TMDB API host** returned HTTP 200 with 20 results each for `/trending/all/week`, `/discover/movie`, `/discover/tv`, and `/search/multi`. Movie and TV details, season, and episode requests also returned HTTP 200. These catalog requests did not use a user account token. The key and full request URLs were not logged.

## Playback and state

`PlayerActivity` uses AndroidX Media3 `PlayerView`. The APK's `StreamSource` carries a stream URL plus source metadata, a headers map, and a subtitle list. This aligns with adapting sources to StreamVault's Media3 player, but the source retrieval and player handoff contract still need live verification. `MyListActivity`, `TvMyListActivity`, `HistoryActivity`, and detail state show favorites/watchlist/history/resume behavior; some settings and playback state are stored in SharedPreferences. Exact key schema and sync conflict behavior require further audit.

## Integration status

| Flow | Status | Evidence or gap |
| --- | --- | --- |
| Home | Reimplemented | Native FunBOX Compose screen uses the verified Clipbox configuration and catalog flow; device rendering is unverified |
| Movies and series | Reimplemented | Native catalog screens and pages are wired; device navigation is unverified |
| Details, seasons, episodes | Reimplemented | Native details and episode metadata are wired; source selection and device navigation are unverified |
| Search | Adapted | Unified search combines active IPTV channels with Clipbox movie and series catalog results; device navigation is unverified |
| Favorites and watchlist | Adapted | Local guest favorite/watchlist state is wired into the unified library; Clipbox account sync is unverified |
| Continue Watching | Not completed | Resume/history endpoints identified; no state adapter yet |
| Sources and playback | Not completed | Media3 and source model identified; authorized live source retrieval unverified |
| Settings | Not completed | Clipbox settings identified; mapping to unified Settings pending |

## Credentials

No secret has been committed or copied into this report. A static `X-App-Key` value exists, while the TMDB key is loaded from remote configuration and account sessions have separate endpoints. A local authorized probe used the APK's static header values in memory and did not log them. The key and signing digest are configured in ignored local properties for local builds, with environment variables available for CI. Temporary or account bound tokens must be obtained through their authorized flow.

## Validation still required

Live Clipbox configuration and its TMDB catalog endpoints are verified. Native Home, Movies, Series, details, seasons, episodes, search, and guest saved state are implemented and compile, but no Android device or emulator was available to demonstrate their UI flows inside FunBOX. Source retrieval, VOD playback, subtitles, and account synchronization remain unimplemented or unverified. Do not mark those end-to-end acceptance criteria complete yet.
