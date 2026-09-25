# דוח מסירה — FunBOX

תאריך: 25 בספטמבר 2026. מצב זה מתאר את הענף `funbox-full-integration` ואת קובץ ה־APK המקומי שנבנה ממנו. הסימון ✅ פירושו שהמימוש והאימות המצוינים בשורה בוצעו; הוא אינו מעיד על בדיקת מכשיר כאשר זו לא בוצעה.

## APK ובדיקות

| פריט | תוצאה |
| --- | --- |
| קובץ | `FunBOX-debug.apk` — APK חתום להתקנה |
| גרסה | `versionName=1.0.19-debug`, `versionCode=21` |
| חבילה | `com.streamvault.app.debug`; הזהות הפנימית נשמרה כדי לא לשבור אינטגרציות קיימות |
| גודל | 55,964,291 בתים (53.37 MiB) |
| SHA-256 | `4BA840615DB88CD5E5D4BFB0946FC0D0E15CF90E51932AE6573C97E551609755` |
| חתימה | `apksigner verify` עבר; APK Signature Scheme v2, אישור Android Debug אחד |
| Android TV | `aapt` מצא Leanback launcher, אייקון, באנר ו־splash; minSdk 25, targetSdk 36 |
| Build | `:app:assembleDebug` מקומי עבר; GitHub Actions run `36106467342` עבר build ו־`testDebugUnitTest` |
| התקנה והרצה | לא נבדקו: `adb devices` לא מצא מכשיר ולא מותקנת תמונת אמולטור מקומית |
| Release | לא נבנה: אין מפתח חתימה ל־Release. קובץ ה־Debug חתום וניתן להתקנה |

ה־APK המקומי כולל את ערכי ההזדהות המורשים הדרושים לבקשת תצורת Clipbox. הם אינם נמצאים ב־Git או ב־GitHub Actions. Artifact ציבורי של CI שנבנה ללא Secrets אינו תחליף ל־APK המקומי לצורך קטלוג Clipbox.

## מצב רכיבי המוצר

| רכיב | מצב | מה בוצע / מגבלה | קבצים או מודולים |
| --- | --- | --- | --- |
| שם ומיתוג | ✅ בוצע | תווית FunBOX ו־130 קובצי תרגום עודכנו; הקרדיט המקורי נשמר | `app`, `feature`, `README.md` |
| אייקון, Adaptive icon, באנר ו־Splash | ✅ בוצע | משאבים מנכסי המיתוג נארזו ב־APK ונמצאו ב־`aapt` | `app/src/main/res` |
| עברית כברירת מחדל ו־RTL | 🟡 בוצע חלקית | מוגדר בקוד ועבר build; מראה ופוקוס על מכשיר לא נבדקו | `app`, `core/ui` |
| ניווט ראשי | 🟡 בוצע חלקית | שבעת היעדים בסדר המבוקש מחוברים; D-Pad וחזרת פוקוס לא נבדקו במכשיר | `app/navigation` |
| ספק M3U מובנה | ✅ בוצע | `FunBOX / עידן פלוס` נוצר בהפעלה ראשונה; URL חי החזיר 14 ערוצים | `FunboxDefaultProviderInitializer`, `data` |
| קבוצת עידן פלוס | ✅ בוצע | 14 ההוראות `#EXTGRP:עידן פלוס` נמצאו; parser קיים תומך בהן | `data/parser/M3uParser` |
| M3U חלופי ובחירת ספק | 🟡 בוצע חלקית | מנגנון הספקים של StreamVault נשמר; מעבר ספק בפועל לא נבדק במכשיר | `feature/provider`, `feature/settings`, `data` |
| EPG | 🟡 בוצע חלקית | `url-tvg` נקלט; 13 מתוך 14 מזהי הערוצים תואמים. נתוני XMLTV הסתיימו ב־15.8.2026 ולכן אין Now/Next עדכני מהמקור | `data/parser`, `data/sync` |
| טלוויזיה ונגן חי | 🟡 בוצע חלקית | מנוע StreamVault ו־Media3 נשמרו; ניגון ושני ערוצים לאורך זמן לא נבדקו | `feature/live`, `player` |
| מדריך ערוצים | 🟡 בוצע חלקית | הגריד המקורי נשמר ומקושר למנגנון הספק הפעיל; נתונים עדכניים ובדיקת מכשיר חסרים | `feature/live`, `data` |
| ניתוח Clipbox APK | ✅ בוצע | ארכיטקטורה, UI, auth, מודלים, קטלוג, מצב משתמש ונגן תועדו ללא סודות | `docs/CLIPBOX_INTEGRATION_AUDIT.md` |
| חתימת API ותצורה | ✅ בוצע | בקשת תצורה חתומה הצליחה; קטלוג TMDB שמוגדר ב־Clipbox החזיר נתונים חיים | `ClipboxApi`, `ClipboxCatalogRepository` |
| בית Clipbox | 🟡 בוצע חלקית | מסך Compose מקורי מחובר לקטלוג חי מאומת; הרצה חזותית במכשיר חסרה | `feature/catalog/.../clipbox` |
| סרטים | 🟡 בוצע חלקית | קטלוג ודפי פריטים מחוברים; בחירת מקור וניגון חסרים | `ClipboxCatalogRepository`, `ClipboxScreens` |
| סדרות | 🟡 בוצע חלקית | קטלוג, פרטי סדרה, עונות ופרקי metadata מחוברים; ניגון פרק חסר | `ClipboxCatalogRepository`, `ClipboxScreens` |
| פרטי סרט/סדרה, עונות ופרקים | 🟡 בוצע חלקית | endpoints החזירו מידע ו־UI מקורי נבנה; מעבר במכשיר לא נבדק | `ClipboxViewModels`, `ClipboxScreens` |
| חיפוש גלובלי | 🟡 בוצע חלקית | תוצאות ערוצים, סרטים וסדרות אוחדו; ניווט תוצאה במכשיר לא נבדק | `feature/catalog/search` |
| מועדפים ורשימת צפייה | 🟡 בוצע חלקית | מצב אורח מקומי לסרטים/סדרות לצד מועדפי ערוצים; סנכרון חשבון Clipbox חסר | `ClipboxUserStateRepository`, `feature/catalog/favorites` |
| Continue Watching ומצב צפייה | ❌ לא בוצע | חוזה ההיסטוריה אותר, אך מתאם Clipbox ושמירת התקדמות לא יושמו | `docs/CLIPBOX_INTEGRATION_AUDIT.md` |
| מקורות, כתוביות וניגון VOD | ❌ לא בוצע | לא אומתו מקורות מורשים לניגון. לא שולבו resolvers צד שלישי ולא נבדק Media3 ל־Clipbox | `player`, `docs/CLIPBOX_INTEGRATION_AUDIT.md` |
| הגדרות מאוחדות | 🟡 בוצע חלקית | הגדרות StreamVault ו־About ממותג נשמרו; הגדרות ייעודיות ל־Clipbox לא שולבו במלואן | `feature/settings` |
| Cache וביצועים | 🟡 בוצע חלקית | מטמון עמודים של Clipbox לזמן קצר ומנגנוני StreamVault נשמרו; ביצועי TV לא נמדדו | `ClipboxCatalogRepository`, `data` |
| פוקוס Android TV | 🟡 בוצע חלקית | Leanback וניווט מקוריים קיימים; בדיקת D-Pad, RTL וחזרת פוקוס חסרה | `app`, `feature` |
| בדיקות ו־CI | ✅ בוצע | build ו־unit tests עברו ב־GitHub Actions; בדיקות מכשיר וניגון חסרות | `.github/workflows/funbox-debug.yml` |
| ענף, commits ו־PR | ✅ בוצע | `funbox-full-integration` נדחף ל־fork; PR #1 פתוח כטיוטה | `tvapp111000/FunBOX` |

## Clipbox: מה נמצא ואיך שולב

ה־APK הוא `org.clipbox.player` גרסה 1.6.1 (161). ממשק ה־TV מבוסס XML ו־Activities, ונגן הווידאו משתמש ב־Media3. `ServerGateActivity` היא נקודת הכניסה. בקשת `/api/config` חתומה ב־HMAC על נתוני האפליקציה והזמן. כניסת חשבון דרך `/api/auth/login` מחזירה user token הנשמר ב־Encrypted SharedPreferences; גלישת אורח בקטלוג אינה דורשת user token. הקטלוג משתמש ב־TMDB key שמתקבל מתצורת Clipbox. בקשות חיות ל־Home, Discover Movies, Discover TV, Search, פרטי סרט/סדרה, עונה ופרק החזירו HTTP 200 ונתונים. פרטי הרשת והמודלים מופיעים ב־`docs/CLIPBOX_INTEGRATION_AUDIT.md` ללא ערכי Secrets.

| Flow | סוג השילוב | מצב מדויק |
| --- | --- | --- |
| Home, Movies, Series | Reimplemented | מסכי Compose מקוריים מחוברים לקטלוג שאומת ברשת; ללא בדיקת מכשיר |
| Details, Seasons, Episodes | Reimplemented | Metadata וניווט נבנו; ללא בחירת מקור וניגון |
| Search | Adapted | תוצאות Clipbox שולבו בחיפוש StreamVault |
| Favorites, Watchlist | Adapted | מצב אורח מקומי מאוחד עם ספריית הערוצים; ללא sync חשבון |
| Continue Watching | Not completed | אין שמירת resume ל־Clipbox |
| Sources, Playback | Not completed | אין מסלול VOD מורשה ומאומת בתוך FunBOX |
| Settings | Not completed | הגדרות Clipbox הספציפיות לא אוחדו במלואן |

## Secrets והרשאות

| שם | מצב | מיקום |
| --- | --- | --- |
| `CLIPBOX_API_KEY` | ✅ נמצא והוגדר לבנייה מקומית | `local.properties`: `clipbox.api.key` |
| `CLIPBOX_SIGNING_DIGEST` | ✅ נמצא והוגדר לבנייה מקומית | `local.properties`: `clipbox.signing.digest` |
| `CLIPBOX_API_SECRET` | לא נדרש כערך נפרד בחוזה שמומש | — |
| `CLIPBOX_CLIENT_ID` | לא נדרש בקטלוג אורח | — |
| `CLIPBOX_TOKEN` | 🔑 דרוש רק ליכולות חשבון; מתקבל ב־login מורשה | אין snapshot של token |
| `TMDB_API_KEY` | מתקבל דינמית מתצורת Clipbox חתומה | אינו נשמר ב־Git |

לבניית CI עם קטלוג פעיל יש להגדיר GitHub Actions Secrets בשם `CLIPBOX_API_KEY` ו־`CLIPBOX_SIGNING_DIGEST` ולחברם לסביבת הבנייה; הם **לא** הועלו ל־GitHub במסגרת עבודה זו. אין ערך Secret בדוח, ב־commits או ב־PR.

## רישוי ומקורות עזר

רישיון StreamVault המקורי נשמר: קרדיט גלוי ל־David Nashash / Davidona, הכיתוב `Based on StreamVault`, קישור למאגר המקורי וקישור ל־Ko-fi. הרישיון מגביל שימוש מסחרי ומחייב פרסום קוד המקור של נגזרת מופצת באותו רישיון. רשימת המאגרים להלן נבדקה מבחינת זמינות, סטטוס archive ורישיון המדווח ב־GitHub; לא הועתק מהם קוד לפרויקט למעט בסיס StreamVault. `none` פירושו ש־GitHub לא זיהה רישיון, לא קביעה משפטית על הרישיון.

| Repository | רישיון מדווח | שימוש בפועל |
| --- | --- | --- |
| `Davidona/StreamVault-IPTV` | NOASSERTION — רישיון מקור זמין לא מסחרי, נקרא מקומית | בסיס וקוד reused |
| `Inside4ndroid/M3U-XCAPI-EPG-IPTV-Stremio` | none | Reference בלבד, ללא קוד |
| `Stremio/stremio-addon-sdk` | MIT | Protocol reference בלבד, ללא addon בשלב זה |
| `haaihond/Nuvio-Wiki` | MIT | Reference בלבד |
| `mrcanelas/tmdb-addon` | Apache-2.0 | לא נדרש |
| `semi-column/tmdb-discover-plus` | MIT | לא נדרש |
| `redd-ravenn/stremio-trakt-addon` | MIT | לא נדרש |
| `TheBeastLT/stremio-kitsu-anime` | Apache-2.0 | לא נדרש |
| `rleroi/Stremio-Streaming-Catalogs-Addon` | none | לא נדרש |
| `Stremio/stremio-official-addons` | MIT | לא נדרש |
| `SSanderV/imdb-popular-stremio` | MIT | לא נדרש |
| `fxqd/letterboxd-stremio` | none | לא נדרש |
| `Inside4ndroid/TMDB-Embed-API` | MIT | Architecture reference בלבד, ללא providers |
| `Inside4ndroid/PyEmbed-Api` | none | Architecture reference בלבד, ללא resolvers |
| `Inside4ndroid/ResolveURL` | GPL-2.0 | Reference בלבד, ללא קוד Python |
| `Inside4ndroid/Cinema-HQ` | none | UX reference בלבד |
| `Toysoft/e2iplayer` | none | Architecture reference בלבד |
| `lemonhead94/Movie4k.to-TVOS` | none | Reference היסטורי בלבד |

כל המאגרים לעיל דווחו כלא מאורכבים בבדיקת GitHub API ב־25.9.2026. לא נוספו מקורות תוכן בלתי מורשים או עקיפות DRM/בקרות גישה.
