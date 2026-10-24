# Android 2.0 acceptance — 2026-10-23

The website's home page, dashboard components, CSS, and existing Discord connection page were left unchanged. The Android-only sign-in page and editing API are new routes.

## Delivered behavior

- Dark native design, embedded Manrope type, fixed Play / Insights / History / Account navigation, matching bottom picker and widget.
- Two-tap logging and launcher actions remain. Local saves are immediate and server synchronization is automatic.
- Dated history with local date/time, search, date/result filters, multi-select, throw and timestamp correction, trash/restore, bulk trash, undo, and full export/import.
- 7-day / 30-day / all-time analytics, decisive win rate, weekly stacked bars, throw distributions and effectiveness, streaks, matchup matrix, and observed after-result patterns. Ties and sparse samples are explicit.
- GitHub provider and Android-only connection flow; the active credential is retained until the pending sign-in is verified server-side. Only a verified primary GitHub email can associate an existing email-based account.

## Verification

- Signed v2 APK installed over signed v1 using the same signing certificate. All nine active emulator fixture games and the original 8–1 score survived the additive SQLite schema upgrade.
- Emulator UI: logged another game, corrected its throws, changed its date through the system date/time pickers, checked recalculated 90% win rate, moved it to trash, restored it, and checked the analytics again.
- Seven JVM advice/analytics tests passed. Release build and Android Lint's error checks passed; English-only localization and layout advisory warnings remain.
- Web lint/typecheck and production build passed. Existing core tests and verified-GitHub-email tests passed.
- Versioned API integration passed: throw/date correction, stale-device rejection, deletion, restoration, compatibility with old-client tombstones, validation, and authorization. Fixtures were removed afterward.
- Production's additive `HandLog.revision` column migration preserved all existing user/game/round counts.

## External setup still required

At verification time, `AUTH_GITHUB_ID` and `AUTH_GITHUB_SECRET` were absent from the production Vercel project. GitHub's real OAuth consent/return flow cannot be completed until the OAuth app is registered, those values are set, and the deployment is refreshed. The app's dedicated sign-in page reports setup pending rather than sending users into a broken provider flow. Existing authenticated device backup remains supported.

OAuth homepage: `https://hoh.jacobsides.com`. Callback: `https://hoh.jacobsides.com/api/auth/callback/github`.

## Production follow-through

The initial cached Vercel build exposed a stale generated Prisma client. The build script now regenerates the client before every Next.js build; the corrected deployment completed successfully. The live APK matched the signed local artifact (`af45b4ef3cefa6d9208537cb694b6235240e4eb415b0cff1dcc6121fb9e28d0d`).

Both old and v2 API integration suites passed against production. A signed-app account-connection fixture uploaded twelve local records, including corrections and trash. A newer server-side throw/date correction then downloaded automatically and appeared in dated native History. This verifies the app/server connection and data flow using an authenticated fixture; it does not substitute for the real GitHub OAuth consent flow, which still awaits the production OAuth credentials. All fixture users, sessions, linked credentials, and history rows were deleted afterward.
