"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { signIn, signOut } from "next-auth/react";
import {
  activeRounds,
  isRound,
  labels,
  mergeRounds,
  outcome,
  recommend,
  symbols,
  throws,
  type HandRound,
  type Throw,
} from "~/lib/hands";

const KEY = "hands:rounds:v1";
type InstallEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> };

export default function HandsApp() {
  const [rounds, setRounds] = useState<HandRound[]>([]);
  const [ready, setReady] = useState(false);
  const [mine, setMine] = useState<Throw | null>(null);
  const [tab, setTab] = useState<"play" | "history" | "settings">("play");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [cloud, setCloud] = useState("Checking backup…");
  const [account, setAccount] = useState<string | null>(null);
  const [install, setInstall] = useState<InstallEvent | null>(null);
  const current = useRef(rounds);
  const syncing = useRef(false);
  const syncQueued = useRef(false);
  const file = useRef<HTMLInputElement>(null);
  const logger = useRef<HTMLElement>(null);

  const persist = useCallback((next: HandRound[]) => {
    try {
      const stored: unknown = JSON.parse(localStorage.getItem(KEY) ?? "[]");
      if (Array.isArray(stored) && stored.every(isRound)) next = mergeRounds(next, stored);
      localStorage.setItem(KEY, JSON.stringify(next));
      current.current = next;
      setRounds(next);
      setError("");
      return true;
    } catch {
      setError("Your browser couldn't save this round. Free up storage or export your history before trying again.");
      return false;
    }
  }, []);

  const sync = useCallback(
    async function syncHistory() {
      if (syncing.current) {
        syncQueued.current = true;
        return;
      }
      syncing.current = true;
      try {
        const response = await fetch("/api/hand-history", { cache: "no-store", signal: AbortSignal.timeout(10000) });
        if (response.status === 401) {
          setCloud("Saved on this device");
          setAccount(null);
          return;
        }
        if (!response.ok) throw new Error("Backup unavailable");
        const data = (await response.json()) as { rounds: HandRound[]; name: string };
        if (!Array.isArray(data.rounds) || !data.rounds.every(isRound)) throw new Error("Invalid history");
        setAccount(data.name);
        const merged = mergeRounds(current.current, data.rounds);
        if (!persist(merged)) return;
        setCloud("Backing up…");
        // Batches keep long histories below the request-size limit.
        for (let start = 0; start < merged.length; start += 250) {
          const result = await fetch("/api/hand-history", {
            method: "POST",
            signal: AbortSignal.timeout(10000),
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ rounds: merged.slice(start, start + 250) }),
          });
          if (!result.ok) throw new Error("Backup unavailable");
        }
        setCloud("Backed up to your account");
      } catch {
        setCloud("Saved on this device · backup pending");
      } finally {
        syncing.current = false;
        if (syncQueued.current) {
          syncQueued.current = false;
          void syncHistory();
        }
      }
    },
    [persist],
  );

  useEffect(() => {
    try {
      const stored: unknown = JSON.parse(localStorage.getItem(KEY) ?? "[]");
      if (!Array.isArray(stored) || !stored.every(isRound)) throw new Error("Invalid data");
      current.current = stored;
      setRounds(stored);
      setReady(true);
    } catch {
      setError("Couldn't read your saved history. Export the stored data before replacing it.");
      return;
    }
    void sync();
    if ("serviceWorker" in navigator)
      void navigator.serviceWorker
        .register("/sw.js")
        .catch(() => setNotice("Offline mode couldn't start. Logging still works while this page is open."));
    const onInstall = (event: Event) => {
      event.preventDefault();
      setInstall(event as InstallEvent);
    };
    const onStorage = (event: StorageEvent) => {
      if (event.key !== KEY || !event.newValue) return;
      try {
        const incoming: unknown = JSON.parse(event.newValue);
        if (Array.isArray(incoming) && incoming.every(isRound)) {
          const next = mergeRounds(current.current, incoming);
          current.current = next;
          setRounds(next);
        }
      } catch {
        /* Ignore malformed data from another tab. */
      }
    };
    const onOnline = () => {
      void sync();
    };
    const timer = window.setInterval(() => {
      void sync();
    }, 30000);
    window.addEventListener("beforeinstallprompt", onInstall);
    window.addEventListener("online", onOnline);
    window.addEventListener("storage", onStorage);
    if (new URLSearchParams(location.search).get("action") === "log")
      logger.current?.scrollIntoView({ block: "center" });
    return () => {
      clearInterval(timer);
      window.removeEventListener("beforeinstallprompt", onInstall);
      window.removeEventListener("online", onOnline);
      window.removeEventListener("storage", onStorage);
    };
  }, [sync]);

  const history = activeRounds(rounds);
  const advice = recommend(rounds);
  const wins = history.filter((r) => outcome(r.mine, r.hers) === "win").length;
  const losses = history.filter((r) => outcome(r.mine, r.hers) === "loss").length;
  const ties = history.length - wins - losses;
  const last = history.at(-1);
  const save = (hers: Throw) => {
    if (!mine || !ready) return;
    const round: HandRound = { id: crypto.randomUUID(), mine, hers, playedAt: new Date().toISOString() };
    if (persist([...current.current, round])) {
      setMine(null);
      setNotice(
        `${outcome(mine, hers) === "win" ? "You win!" : outcome(mine, hers) === "loss" ? "She wins." : "A tie."} Round saved.`,
      );
      navigator.vibrate?.(20);
      void sync();
    }
  };
  const remove = (round: HandRound) => {
    if (persist(current.current.map((r) => (r.id === round.id ? { ...r, deleted: true } : r)))) {
      setNotice("Round removed.");
      void sync();
    }
  };
  const download = () => {
    // Keep the original bytes available even if storage was malformed.
    const blob = new Blob([localStorage.getItem(KEY) ?? "[]"], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `hands-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <main className="hands-shell">
      <header className="masthead">
        <Link prefetch={false} className="brand" href="/" aria-label="History of Hands home">
          <span className="brand-mark">
            h<span>h</span>
          </span>
          <span>
            HISTORY
            <br />
            OF HANDS
          </span>
        </Link>
        <span className="couple-tag">
          <i /> YOU & HER
        </span>
      </header>
      <nav className="tabs" aria-label="Main navigation">
        {(
          [
            ["play", "The next round"],
            ["history", "Our history"],
            ["settings", "Your app"],
          ] as const
        ).map(([id, label]) => (
          <button key={id} aria-current={tab === id ? "page" : undefined} onClick={() => setTab(id)}>
            {label}
            {id === "history" && <small>{history.length}</small>}
          </button>
        ))}
      </nav>
      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}
      {tab === "play" && (
        <>
          <section className="intro">
            <p className="eyebrow">A LITTLE RIVALRY. A LOT OF HISTORY.</p>
            <h1>
              Love her.
              <br />
              <em>Beat her at scissors.</em>
            </h1>
            <p>Your next move, with a little help from your last ones.</p>
          </section>
          <div className="play-grid">
            <section className="advice" aria-labelledby="advice-title">
              <div className="section-top">
                <span className="eyebrow">THE NEXT MOVE</span>
                <span className="pill">{advice.enough ? "PATTERN FOUND" : "GETTING TO KNOW HER"}</span>
              </div>
              <div className="recommendation">
                <span className="big-hand" aria-hidden="true">
                  {advice.enough ? symbols[advice.throw] : "✌️"}
                </span>
                <div>
                  <p id="advice-title">{advice.enough ? "Try throwing" : "Keep her guessing"}</p>
                  <h2>{advice.enough ? labels[advice.throw] : "Stay unpredictable."}</h2>
                </div>
              </div>
              <p className="reason">
                {advice.enough
                  ? `She threw ${labels[advice.predicted].toLowerCase()} ${advice.share}% of the time ${advice.conditional ? "after this same throw and result" : "in your recent rounds"}. ${labels[advice.throw]} has the best win–loss balance in that sample.`
                  : history.length < 8
                    ? `A few rounds aren't a pattern. Log ${Math.max(0, 8 - history.length)} more to start looking for an edge. Until then, mix up your throws.`
                    : "Her throws are balanced so far. There's no clear edge—mix up your throws."}
              </p>
              <div className="evidence">
                <span>
                  {advice.sampleSize} {advice.conditional ? "matching" : "recent"} rounds
                </span>
                <span>{advice.enough ? "A clue, never a guarantee ↗" : "No made-up predictions ↗"}</span>
              </div>
            </section>
            <section className="logger" ref={logger} aria-labelledby="logger-title">
              <div className="section-top">
                <span className="eyebrow">LOG A ROUND</span>
                <span className="step">{mine ? "02 / 02" : "01 / 02"}</span>
              </div>
              <h2 id="logger-title">{mine ? "And what did she throw?" : "What did you throw?"}</h2>
              <p className="logger-hint">
                {mine ? (
                  <>
                    <span>
                      {symbols[mine]} You threw {labels[mine].toLowerCase()}.
                    </span>{" "}
                    <button className="text-button" onClick={() => setMine(null)}>
                      Change
                    </button>
                  </>
                ) : (
                  "Your throw, then hers. That's it."
                )}
              </p>
              <div className="throw-buttons">
                {throws.map((t) => (
                  <button
                    key={t}
                    disabled={!ready}
                    aria-label={`${mine ? "She" : "You"} threw ${labels[t].toLowerCase()}`}
                    onClick={() => (mine ? save(t) : setMine(t))}
                  >
                    <span aria-hidden="true">{symbols[t]}</span>
                    <strong>{labels[t]}</strong>
                  </button>
                ))}
              </div>
              <div className="save-note" role="status" aria-live="polite">
                <span className="tiny-dot" />
                {notice || (ready ? "Saves instantly. Ready for the next round." : "Opening your history…")}
              </div>
            </section>
          </div>
          <section className="scoreboard" aria-label="Overall score">
            <div>
              <span className="eyebrow">THE RUNNING SCORE</span>
              <p>
                Same two players.
                <br />
                An ongoing story.
              </p>
            </div>
            <div className="score">
              <strong>{wins}</strong>
              <span>YOU</span>
            </div>
            <span className="score-divider">:</span>
            <div className="score her">
              <strong>{losses}</strong>
              <span>HER</span>
            </div>
            <div className="ties">
              <strong>{ties}</strong>
              <span>TIES</span>
            </div>
          </section>
          <section className="last-round">
            <div>
              <span className="eyebrow">{last ? "LAST ROUND" : "YOUR STORY STARTS HERE"}</span>
              <p>
                {last ? (
                  <>
                    {symbols[last.mine]} <span className="versus">vs</span> {symbols[last.hers]}{" "}
                    <strong>
                      {outcome(last.mine, last.hers) === "win"
                        ? "You won"
                        : outcome(last.mine, last.hers) === "loss"
                          ? "She won"
                          : "You tied"}
                    </strong>
                  </>
                ) : (
                  "Play a round together. We'll remember the throws."
                )}
              </p>
            </div>
            {last && (
              <button className="text-button" onClick={() => remove(last)}>
                Undo last round ↶
              </button>
            )}
          </section>
        </>
      )}
      {tab === "history" && (
        <section className="history-page">
          <p className="eyebrow">THE RECEIPTS</p>
          <h1>Every little victory.</h1>
          <p className="muted">
            {wins} wins · {losses} losses · {ties} ties. Always you versus her.
          </p>
          <section className="tendencies">
            <h2>Her go-to throws</h2>
            {throws.map((t) => {
              const count = history.filter((r) => r.hers === t).length;
              const share = history.length ? Math.round((count / history.length) * 100) : 0;
              return (
                <div className="bar-row" key={t}>
                  <span>
                    {symbols[t]} {labels[t]}
                  </span>
                  <div className="bar-track">
                    <div style={{ width: `${share}%` }} />
                  </div>
                  <strong>{share}%</strong>
                </div>
              );
            })}
          </section>
          {!history.length && (
            <div className="empty">
              <span>✊ ✋ ✌️</span>
              <h2>A clean scorecard.</h2>
              <p>Your first round is only two taps away.</p>
              <button className="primary" onClick={() => setTab("play")}>
                Log your first round
              </button>
            </div>
          )}
          <ol className="round-list">
            {[...history]
              .reverse()
              .slice(0, 200)
              .map((r) => (
                <li key={r.id}>
                  <span className={`result ${outcome(r.mine, r.hers)}`}>
                    {outcome(r.mine, r.hers) === "win" ? "W" : outcome(r.mine, r.hers) === "loss" ? "L" : "="}
                  </span>
                  <div>
                    <strong>
                      You: {labels[r.mine]} <span>·</span> Her: {labels[r.hers]}
                    </strong>
                    <time dateTime={r.playedAt}>
                      {new Date(r.playedAt).toLocaleString(undefined, {
                        month: "short",
                        day: "numeric",
                        hour: "numeric",
                        minute: "2-digit",
                      })}
                    </time>
                  </div>
                  <button className="text-button" aria-label={`Delete round ${r.id}`} onClick={() => remove(r)}>
                    Remove
                  </button>
                </li>
              ))}
          </ol>
          {history.length > 200 && (
            <p className="muted">Showing your latest 200 rounds. Export includes your full history.</p>
          )}
        </section>
      )}
      {tab === "settings" && (
        <section className="settings-page">
          <p className="eyebrow">MADE FOR YOUR LITTLE RIVALRY</p>
          <h1>Keep it close.</h1>
          <section className="setting">
            <h2>Log without opening the app</h2>
            <p>
              Hands for Android adds a one-tap home-screen widget with all nine throw pairings and your next-move
              advice. Hold its icon for “I threw rock/paper/scissors,” then pick hers in a small popup. Open Hands once
              to connect your history and add the widget.
            </p>
            <a className="primary download-link" href="/downloads/hands.apk" download>
              Download Hands for Android ↓
            </a>
            <p className="fine-print">
              Android 13 or newer. Install the downloaded APK, allowing this browser to install apps when Android asks.
              No Play Store account needed.
            </p>
          </section>
          <section className="setting">
            <h2>Install the web dashboard</h2>
            <p>
              Install Hands for a full-screen app, offline logging, and a “Log a round” shortcut when you hold its icon.
            </p>
            {install ? (
              <button
                className="primary"
                onClick={async () => {
                  await install.prompt();
                  await install.userChoice;
                  setInstall(null);
                }}
              >
                Install Hands ↗
              </button>
            ) : (
              <p className="instruction">
                In Chrome on Android, open ⋮ → Add to home screen → Install. Already installed? You’re all set.
              </p>
            )}
          </section>
          <section className="setting">
            <h2>Your history, backed up</h2>
            <p>
              {cloud}. Rounds are saved in this browser first. Connect your existing Discord account to import your past
              rounds and back up new ones across devices.
            </p>
            {account ? (
              <>
                <p className="account">Connected as {account}</p>
                <button
                  className="primary"
                  onClick={() => {
                    void sync();
                  }}
                >
                  Sync now
                </button>
                <button
                  className="text-button"
                  onClick={() => {
                    void signOut();
                  }}
                >
                  Disconnect
                </button>
              </>
            ) : (
              <button
                className="primary"
                onClick={() => {
                  void signIn("discord");
                }}
              >
                Connect Discord ↗
              </button>
            )}
            {account && (
              <button
                className="text-button"
                onClick={async () => {
                  try {
                    const response = await fetch("/api/hand-devices", { method: "DELETE" });
                    if (!response.ok) throw new Error();
                    setNotice("Android devices disconnected. Their local history is retained.");
                  } catch {
                    setError("Couldn't disconnect Android devices. Try again online.");
                  }
                }}
              >
                Disconnect Android devices
              </button>
            )}
            <p className="fine-print">
              This browser holds one shared scorecard for you and your wife. Connect only the account you want to back
              it up to.
            </p>
          </section>
          <section className="setting">
            <h2>A copy to keep</h2>
            <p>
              Export your full history as a file, or merge a previous export. Importing never removes existing rounds.
            </p>
            <div className="settings-actions">
              <button className="primary" onClick={download}>
                Export history ↓
              </button>
              <button className="secondary" onClick={() => file.current?.click()}>
                Import history ↑
              </button>
            </div>
            <input
              ref={file}
              hidden
              type="file"
              accept="application/json,.json"
              onChange={async (event) => {
                const selected = event.target.files?.[0];
                if (!selected) return;
                try {
                  if (selected.size > 10000000) throw new Error();
                  const data: unknown = JSON.parse(await selected.text());
                  if (!Array.isArray(data) || !data.every(isRound)) throw new Error();
                  if (persist(mergeRounds(current.current, data))) {
                    setNotice("History imported.");
                    void sync();
                  }
                } catch {
                  setError("That file isn't a valid Hands export. Your history hasn't changed.");
                }
                event.target.value = "";
              }}
            />
          </section>
          <section className="setting">
            <h2>About the advice</h2>
            <p>
              Hands looks at her last 30 throws. Once there are eight examples, it also checks what she tends to play
              after the same throw and result. It chooses the move with the best estimated wins minus losses, smoothing
              the counts so small samples don’t look certain. Balanced throws get no recommendation. People change;
              treat every suggestion as a hunch.
            </p>
          </section>
        </section>
      )}
      <div className="notice" role="status" aria-live="polite">
        {tab !== "play" ? notice : ""}
      </div>
      <footer>
        <span>FOR BETTER OR FOR SCISSORS.</span>
        <span>
          <i /> {cloud}
        </span>
      </footer>
    </main>
  );
}
