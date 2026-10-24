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
