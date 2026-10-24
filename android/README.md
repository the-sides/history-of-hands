# Hands for Android

A small native Android 13+ app with no runtime dependencies. The public signed APK is at `../public/downloads/hands.apk` and is served as https://hoh.jacobsides.com/downloads/hands.apk.

## The launcher is the product

- **Widget:** nine buttons, each labeled **your throw / her throw**. One tap logs a round, updates the score and recommendation, and keeps the launcher visible. Undo is available directly on the widget. The widget can be resized.
- **Long-press shortcuts:** “I threw rock/paper/scissors” open only a small throw picker. Picking hers records the round and dismisses it. The fourth shortcut displays the current recommendation.
- **Offline first:** SQLite commits the round before any network work. The widget and shortcuts use the same store and recommender as the native screen. Uninstalling clears phone data; export or connect backup first.
- **Connect once:** from Hands, choose **Connect website & cloud backup**. Sign into Discord in your browser, approve linking the phone, and return to Hands. The existing scorecard downloads automatically. Android's JobScheduler retries backup when a network is available; the website syncs while open.
- **Private credentials:** the app generates a random 256-bit device token and opens a pairing link with it in the URL fragment. The authenticated website requires explicit approval, stores only a SHA-256 digest, and removes the fragment on success. No token is embedded in the APK. Android's private preferences retain the token; OS backups exclude it. **Your app → Disconnect Android devices** on the website revokes all linked tokens. Device credentials only authorize the personal history API, not general account access.

The recommendation logic intentionally mirrors `src/lib/hands.ts`: a recent 30-round baseline, at least eight contextual examples before using after-result patterns, Laplace-smoothed expected win–loss choice, and no claimed edge for balanced throws.

## Build

Use JDK 21, Android SDK platform 35/build tools 35, and the checked-in Gradle wrapper:

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

For a signed personal release, supply `HANDS_KEYSTORE` and `HANDS_STORE_PASSWORD`, then run `./gradlew :app:assembleRelease`. The local signing key for this installation is in `~/.local/share/history-of-hands/`, outside the repository. Preserve it for future in-place upgrades; never commit it or its password. Release signing uses alias `hands`. Update `versionCode` and `versionName` for subsequent releases. Copy the result from `app/build/outputs/apk/release/app-release.apk` to `../public/downloads/hands.apk` before deployment.

## Verification

JVM tests cover all nine outcomes, minimum evidence, balanced throws, and deleted-round filtering. Android Lint runs for debug/release. The emulator acceptance flow uses the real launcher-hosted widget, confirms the foreground remains the launcher after a log, checks score and undo, then launches the shortcut picker and confirms it dismisses after logging. The web API integration test covers device pairing, digest storage, bearer auth, and revocation in addition to legacy import and isolation.

## Version 2: Android redesign

The website UI is unchanged. Android now has **Play**, **Insights**, **History**, and **Account** tabs, a dark Manrope-based design, and a matching quick picker and widget. History is grouped by local date and shows each game's time. Tap a game to edit either throw and its date/time. Filters, search, multi-select, reversible trash, restore, and export/import are available directly in the app.

Insights supports 7-day, 30-day, and all-time ranges, decisive win rate (ties excluded), a daily stacked chart, both players' throw statistics, per-throw performance, streaks, a matchup matrix, and observed repeat-after-result patterns. Sparse samples are labeled explicitly.

SQLite upgrades in place to schema 2 using additive columns, preserving all v1 data. Each edit, deletion, and restore has a monotonically increasing revision. Only pending changes upload; acknowledgments match both ID and revision so edits made during an upload remain pending. The v2 history API conditionally applies newer revisions. Older exports and old-client tombstones cannot overwrite a corrected or restored game. Trash remains recoverable and is excluded from analytics.

Saving is automatic: immediately on the phone, opportunistically in the background, and while the app is open. Account shows the current save state and last successful save. There is no manual sync button. Signing out retains phone data and pauses backup.

**GitHub setup:** register an OAuth application with homepage `https://hoh.jacobsides.com` and callback `https://hoh.jacobsides.com/api/auth/callback/github`; set `AUTH_GITHUB_ID` and `AUTH_GITHUB_SECRET` on the Vercel production project and redeploy. Credentials never go into the APK. The native-only `/android/sign-in` page uses GitHub while the existing website's Discord flow remains available. Only a verified primary GitHub email can link to an existing email-based scorecard. Pending credentials replace the active device credential only after the server verifies the completed sign-in.

Backend schema addition: `20261024010000_editable_android_history` adds `HandLog.revision` with default zero, preserving all existing rows. It was applied additively to production before introducing the v2 API.
