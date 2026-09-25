# FunBOX

FunBOX is an Android and Android TV app in development, based on StreamVault. The current branch adds FunBOX branding and Hebrew as the first-install language; IPTV and Clipbox integration are tracked in [docs/FUNBOX_PROGRESS.md](docs/FUNBOX_PROGRESS.md). Do not treat this branch as a completed Clipbox integration or validated release.

## Source and attribution

FunBOX is **Based on StreamVault**, originally developed by **David Nashash (Davidona)**. The original project is [StreamVault-IPTV](https://github.com/Davidona/StreamVault-IPTV), and its [support page](https://ko-fi.com/davidona) remains accessible in the app's About screen. The [original README](docs/STREAMVAULT_ORIGINAL_README.md) is retained for technical reference.

The derivative retains the [StreamVault Source-Available License (Non-Commercial)](LICENSE). Distribution requires the full corresponding source under the same license, visible attribution, and the original links. Commercial use requires prior written permission from the copyright holder.

## Development

Build with Android SDK 36 and a compatible JDK using `./gradlew assembleDebug`. Run `./gradlew testDebugUnitTest` for unit tests. Progress and validation evidence are recorded in [docs/FUNBOX_PROGRESS.md](docs/FUNBOX_PROGRESS.md).
