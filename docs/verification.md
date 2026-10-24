# Couples redesign acceptance — 2026-10-23

Implementation: `91f2ea7`. Production: https://hoh.jacobsides.com.

## Preserved

The original `787d26e` app is on the remote branch `archive/before-couples-redesign-2026-10-23`. The existing production database retained its original user, two games, and ten rounds. New tables are additive. Disposable integration users, sessions, device credentials, and personal rounds were removed after testing.

## Web

- `pnpm check` and `pnpm build` passed.
- Core tests passed for all nine outcomes, minimum evidence, balanced throws, input validation, deduplication, and irreversible deletion markers.
- Real browser checks passed for two-tap logging, saved-round persistence after reload, undo, valid import, rejected invalid import, and export download.
- The production build reopened and logged a round after the local HTTP server was stopped completely, using its cached shell and assets.
- Phone-width layout had no horizontal overflow. Production manifest and installed service worker were checked.
- Live API integration passed for session authorization, idempotency, account isolation, validation, origin protection, tombstones, all ten legacy rounds, Android pairing, hashed credential storage, bearer authorization, and revocation.

## Android

- Android 15 / API 35 emulator, Pixel 7 configuration. The delivered app supports Android 13+.
- Debug and signed release builds passed Android Lint's error checks. Three JVM tests passed, covering all outcomes, recommendation evidence, balanced throws, and deleted-round filtering. Lint retains English-only text/localization and layout advisory warnings.
- The signed release APK was installed successfully.
- A real launcher-hosted widget recorded a winning throw pair while the launcher remained the foreground activity. Undo restored the score without launching an activity.
- The compact shortcut picker saved the chosen pair, dismissed itself, and returned to the launcher. Dynamic shortcuts were registered for all three throws and the current advice.
- Actual signed-app networking was tested against production: nine emulator fixture rounds uploaded, then an additional server round downloaded and changed the phone's score. Fixture credentials were revoked and all server fixture rows were removed afterward.
- The live APK download matched the locally verified signed artifact. SHA-256: `9c934c3312325ea19a2e32b03e6db0831152e775abe2eb8a6630606808f8d4b8`.

Launcher behavior was tested with Android's emulator launcher. Placement, resizing, and menu display on the user's physical phone and launcher remain for the first installation. No physical phone was connected to this machine.
