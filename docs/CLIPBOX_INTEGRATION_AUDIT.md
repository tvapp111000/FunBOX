# Clipbox APK integration audit

Source: user-supplied `clipbox.apk` (analyzed locally; APK and decompiled output are not committed). This is a technical audit, not a claim that Clipbox has been integrated or validated in FunBOX. No credential values are recorded here.

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

Clipbox also uses its own remote service gate with fallback hosts, an `/api/config` request, account authentication endpoints (`/api/auth/...`), and synchronized state endpoints for favourites, watchlist, history, resume, and settings. The networking interceptor adds `User-Agent`, `X-App-Version`, `X-App-Key`, and a dynamically generated `X-App-Integrity` header. A static app key is present in the APK, but its value is deliberately absent here. After the user explicitly authorized credential-bearing tests to the original APK hosts, a restricted probe of `https://clipbox.mov/api/config` returned HTTP 200 with maintenance, update, and server-time fields. The fallback `https://superbox.mov/api/config` returned HTTP 526; TLS was not bypassed. Auth and sample catalog requests to `clipbox.mov` returned HTTP 403 with an integrity-class rejection. The exact signature and response contract remain under investigation. The APK also includes source resolver modules under a separate bundled package; their third party hosts are not assumed to be authorized for FunBOX and have not been copied.

`ServerGateActivity` runs a remote configuration check before entering TV browsing. The code includes account login and token related flows; a static credential alone cannot be assumed to replace a session. No authentication bypass has been implemented.

## Playback and state

`PlayerActivity` uses AndroidX Media3 `PlayerView`. The APK's `StreamSource` carries a stream URL plus source metadata, a headers map, and a subtitle list. This aligns with adapting sources to StreamVault's Media3 player, but the source retrieval and player handoff contract still need live verification. `MyListActivity`, `TvMyListActivity`, `HistoryActivity`, and detail state show favorites/watchlist/history/resume behavior; some settings and playback state are stored in SharedPreferences. Exact key schema and sync conflict behavior require further audit.

## Integration status

| Flow | Status | Evidence or gap |
| --- | --- | --- |
| Home | Not completed | TV hero/rows identified; no FunBOX data flow yet |
| Movies and series | Not completed | TMDB shaped Clipbox catalog identified; live API contract unverified |
| Details, seasons, episodes | Not completed | TV/detail models and views identified; no FunBOX route yet |
| Search | Not completed | Clipbox TV search identified; no unified repository yet |
| Favorites and watchlist | Not completed | Sync endpoints identified; authentication/state schema unverified |
| Continue Watching | Not completed | Resume/history endpoints identified; no state adapter yet |
| Sources and playback | Not completed | Media3 and source model identified; authorized live source retrieval unverified |
| Settings | Not completed | Clipbox settings identified; mapping to unified Settings pending |

## Credentials

No secret has been committed or copied into this report. A static `X-App-Key` value exists, while the TMDB key is loaded from remote configuration and account sessions have separate endpoints. A local authorized probe used the APK's static header values in memory and did not log them. None is configured in FunBOX yet. Any needed value must be provided through ignored local properties or environment/CI secrets. Temporary or account bound tokens must be obtained through their authorized flow; the dynamic integrity mechanism is not treated as permission to bypass access controls.

## Validation still required

Live Clipbox service response, Home, Movies, Series, details, seasons, episodes, search, favorites, watchlist, source retrieval, playback, and subtitles have not been demonstrated inside FunBOX. Do not mark those acceptance criteria as complete yet.
