# History of Hands

A little rock-paper-scissors rivalry for you and your wife. The home screen recommends a move, then lets you log both throws in two taps. No game creation, invitation, or opponent account is needed.

## Use it

Open https://hoh.jacobsides.com. Choose **your** throw, then **hers**. The round saves immediately; **Undo last round** removes mistakes. **Our history** shows the scorecard and her throw frequencies.

In Chrome on Android, use **⋮ → Add to home screen → Install**. Long-press the installed icon for **Log a round** or **What's my next move?** The log shortcut opens directly at the controls. Once the app has loaded and its worker has installed, it also opens and logs offline.

## Storage and backup

The browser stores a versioned scorecard in `localStorage` (`hands:rounds:v1`). **Your app → Connect Discord** imports your original rounds and backs up new rounds to your account. Sync runs after changes, on reconnect, and every 30 seconds while open. The same account on another device downloads the scorecard. Browser data is one shared scorecard: use your own Discord account consistently. Signing out retains local rounds.

Export/import JSON from **Your app** to keep another copy or transfer without Discord. Clearing browser storage without a cloud backup or export loses local history. Undo uses tombstones so stale devices and old exports cannot resurrect removed rounds. Imports merge by stable ID.

All existing game/round/account tables remain intact. Legacy rounds are mapped to your perspective and treated as games with your wife. The additive `HandLog` table stores new personal rounds and deletion markers. Existing `/game/[id]` and invite routes remain available, although the home page uses the new flow.

## Advice

The recommender compares expected wins minus losses using her latest 30 throws, with Laplace smoothing. If there are at least eight examples of what she played after the same preceding throw and outcome, it uses that context instead. It shows sample size and observed frequency. Fewer than eight examples or equally strong options produce a request to mix your throws, rather than a confident prediction. These are descriptive hints, not a promise of an advantage against random play.

## Development

Node 22+ and pnpm 10:

```sh
pnpm install --frozen-lockfile
# Supply .env.local from the existing Vercel project, or see .env.example.
pnpm dev
pnpm check
pnpm build
node --experimental-strip-types tests/hands.test.ts
```

The database integration test uses disposable fixture accounts and deletes them in `finally`. It requires the app running and the same database credentials as that app:

```sh
node --env-file=.env.local tests/api.integration.mjs
```

`HANDS_TEST_URL` can target another instance. The test checks authorization, validation, idempotency, account isolation, deletion propagation, and read-only legacy import.

## Deployment and preservation

The original app at `787d26e` is preserved on `archive/before-couples-redesign-2026-10-23`. Main contains the couples redesign.

Production historically used Prisma schema push and has no `_prisma_migrations` table. The additive SQL in `prisma/migrations/20261024001000_personal_scorecard/migration.sql` was applied transactionally to that database after checking table existence; users, games, and original rounds were unchanged. Do not run the entire historical migration chain against this existing database without first establishing its baseline. New databases should use the full Prisma schema (`pnpm db:push`).

The service worker caches the public shell and its static assets, never authenticated API responses. Bump its cache version when changing its caching behavior. Keep old cached assets until the next worker finishes precaching. The authenticated API always uses `Cache-Control: private, no-store`.

## Android: log without opening the app

Install [Hands for Android](https://hoh.jacobsides.com/downloads/hands.apk) (Android 13+), open it once to connect backup, and add its widget. Tap the **your throw / her throw** pairing to save without opening an activity. Alternatively, hold the Hands icon, choose your throw, and pick hers in the compact popup. The menu and widget also show the current advice. See [Android setup and build details](android/README.md).

The second additive migration creates `HandDevice` for revocable hashed device credentials. It was applied without changing existing rounds. A device token only authorizes the scorecard API; browser approval is required to link it.

### Android 2.0

The Android redesign adds dark styling, dated/editable game history, reversible trash and bulk selection, richer analytics, and automatic saving. GitHub sign-in is configured with `AUTH_GITHUB_ID` and `AUTH_GITHUB_SECRET` using callback `https://hoh.jacobsides.com/api/auth/callback/github`. Its native sign-in page and versioned editing API are separate from the existing website UI. See [Android details](android/README.md).
