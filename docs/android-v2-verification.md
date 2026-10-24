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

## GitHub production setup

The GitHub OAuth application was registered under the-sides. Both `AUTH_GITHUB_ID` and `AUTH_GITHUB_SECRET` are stored as Production secrets in thesides-projects/history-of-hands. Production deployment `history-of-hands-5rhgfkc00-thesides-projects.vercel.app` completed and was aliased to the custom domain. The live providers endpoint advertises GitHub, and the Android sign-in button reaches GitHub's consent page with read-only profile/email scopes and the correct callback. The first real callback exposed an RFC 9207 issuer mismatch in Auth.js: its fallback expected `https://authjs.dev` while GitHub returns `https://github.com/login/oauth`. The provider now explicitly configures GitHub's issuer. A regression test exercises Auth.js provider normalization and its OAuth callback validator, accepts GitHub's issuer, and rejects a foreign issuer. Following deployment `history-of-hands-ctdpa3g05-thesides-projects.vercel.app`, real GitHub sign-in successfully returned to the Android account-connection approval screen. No test device was connected and no game history was uploaded during this check.

OAuth homepage: `https://hoh.jacobsides.com`. Callback: `https://hoh.jacobsides.com/api/auth/callback/github`.

## Production follow-through

The initial cached Vercel build exposed a stale generated Prisma client. The build script now regenerates the client before every Next.js build; the corrected deployment completed successfully. The live APK matched the signed local artifact (`af45b4ef3cefa6d9208537cb694b6235240e4eb415b0cff1dcc6121fb9e28d0d`).

Both old and v2 API integration suites passed against production. A signed-app account-connection fixture uploaded twelve local records, including corrections and trash. A newer server-side throw/date correction then downloaded automatically and appeared in dated native History. This verifies the app/server connection and data flow using an authenticated fixture; it does not substitute for the real GitHub OAuth consent flow, whose real callback was subsequently verified after correcting the issuer configuration. All fixture users, sessions, linked credentials, and history rows were deleted afterward.

## Android 2.1 follow-through

Signed versionCode 4 / 2.1.0 includes the adaptive full-frame launcher icon, three gesture controls (up win, down loss, completed double-tap tie), haptic confirmation and Undo. Visible History checkboxes and matching-game selection feed an atomic bulk editor for either throw or date, preserving unspecified fields and individual times. Bulk Undo restores each original record.

Release build, lint error checks, and all nine JVM tests passed, including all nine throw/result combinations. Emulator interaction checks confirmed each gesture adds exactly one game, Undo restores the count, short/horizontal gestures do not save, and selecting two checkboxes then bulk editing and undoing completes successfully. Only emulator fixtures were used.

## Android 2.2 automatic saving and conflicts

The app has no manual sync button. Added network-constrained periodic background refresh, in addition to automatic save/resume/foreground syncing. SQLite v3 tracks each game's base server revision and keeps conflicting account snapshots separately. The review card compares both throws, timestamps, and trash status and offers Keep phone, Keep account, or Decide later. Nonconflicting games continue to upload. Account switches reset baseline metadata.

API integration checks cover competing updates, higher-clock stale edits, delete/edit collisions, explicit resolution, retry idempotence, concurrent compare-and-swap, and unrelated uploads in a conflicting batch. Executable debug-only emulator storage checks cover base retention, local preservation, blocked conflict upload, unrelated changes, both resolutions, and edits made while an earlier upload is acknowledged. UI checks cover comparison, deferral, and account resolution. Release build, nine JVM tests, Android lint error checks, and web lint/typechecking passed. Older APKs must be upgraded to get conflict review.

## Android 2.2.1 swipe-to-trash

History game cards support left/right swipe-to-trash with directional feedback and a persistent Undo bar naming the action. The gesture uses a 30% card-width threshold, excludes vertical/short/cancelled/multi-touch input, and is disabled in bulk selection and Trash. The initial emulator run identified a vertical drag opening the editor on a short non-scrolling list; moved gestures now cancel click handling.

The corrected emulator acceptance passed left/right deletion, immediate visible Undo and restored counts, short/vertical non-deletion, and disabled swipe deletion in bulk selection. Signed release build, lint error checks, and all nine JVM tests passed.
