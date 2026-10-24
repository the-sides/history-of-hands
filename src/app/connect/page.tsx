"use client";
import { useEffect, useState } from "react";
import { signIn } from "next-auth/react";
export default function Connect() {
  const [token, setToken] = useState("");
  const [status, setStatus] = useState("checking");
  const [name, setName] = useState("");
  useEffect(() => {
    const value = new URLSearchParams(location.hash.slice(1)).get("token") ?? "";
    if (!/^[A-Za-z0-9_-]{43}$/.test(value)) {
      setStatus("invalid");
      return;
    }
    setToken(value);
    void fetch("/api/auth/session", { cache: "no-store" })
      .then((r) => r.json())
      .then((session: { user?: { name?: string } }) => {
        setName(session.user?.name ?? "you");
        setStatus(session.user ? "ready" : "signin");
      })
      .catch(() => setStatus("error"));
  }, []);
  return (
    <main className="hands-shell settings-page">
      <p className="eyebrow">HANDS FOR ANDROID</p>
      <h1>
        Your rivalry.
        <br />
        On your home screen.
      </h1>
      <section className="setting">
        <h2>Connect your phone</h2>
        <p>
          Link the Android app to your account once. Widget rounds will back up here, and your existing history will
          appear on your phone. After this, logging works right from the launcher, even offline.
        </p>
        {status === "checking" && <p role="status">Checking your account…</p>}
        {status === "invalid" && <p>Start from “Connect website & cloud backup” inside the Hands Android app.</p>}
        {status === "signin" && (
          <button
            className="primary"
            onClick={() => {
              void signIn("discord", { redirectTo: location.href });
            }}
          >
            Sign in with Discord ↗
          </button>
        )}
        {status === "ready" && (
          <>
            <p>Connect to {name}&apos;s scorecard?</p>
            <button
              className="primary"
              onClick={async () => {
                setStatus("connecting");
                try {
                  const r = await fetch("/api/hand-devices", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ token }),
                  });
                  if (!r.ok) throw new Error();
                  history.replaceState(null, "", "/connect");
                  setToken("");
                  setStatus("done");
                } catch {
                  setStatus("error");
                }
              }}
            >
              Connect this phone
            </button>
          </>
        )}
        {status === "connecting" && <p role="status">Connecting…</p>}
        {status === "done" && (
          <>
            <p>Your phone is connected. Return to Hands to finish syncing, then use the widget or hold its app icon.</p>
            <a className="primary" href="historyhands://paired">
              Return to Hands ↗
            </a>
          </>
        )}
        {status === "error" && (
          <p role="alert">Couldn&apos;t connect. Open the connection link from Hands again and retry.</p>
        )}
      </section>
    </main>
  );
}
