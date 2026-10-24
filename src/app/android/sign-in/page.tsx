"use client";
import { useEffect, useState } from "react";
import { signIn } from "next-auth/react";
export default function AndroidSignIn() {
  const [token, setToken] = useState("");
  const [state, setState] = useState("loading");
  const [name, setName] = useState("");
  useEffect(() => {
    const value = new URLSearchParams(location.hash.slice(1)).get("token") ?? "";
    if (!/^[A-Za-z0-9_-]{43}$/.test(value)) {
      setState("invalid");
      return;
    }
    setToken(value);
    if (new URLSearchParams(location.search).has("error")) {
      setState("error");
      return;
    }
    void fetch("/api/android/account", { cache: "no-store" })
      .then((r) => r.json())
      .then((data: { authenticated: boolean; github: boolean; githubReady: boolean; name: string }) => {
        setName(data.name ?? "you");
        setState(!data.githubReady ? "setup" : data.authenticated && data.github ? "approve" : "signin");
      })
      .catch(() => setState("error"));
  }, []);
  const connect = async () => {
    setState("saving");
    try {
      const response = await fetch("/api/hand-devices", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token }),
      });
      if (!response.ok) throw new Error();
      history.replaceState(null, "", "/android/sign-in");
      setToken("");
      setState("done");
      location.href = "historyhands://paired";
    } catch {
      setState("error");
    }
  };
  const buttonStyle = {
    background: "#d0f581",
    color: "#0d1015",
    padding: "16px 24px",
    border: 0,
    borderRadius: 16,
    fontWeight: 700,
    fontSize: 15,
    display: "inline-block",
    textDecoration: "none",
    cursor: "pointer",
  };
  return (
    <main
      style={{
        minHeight: "100vh",
        background: "#0d1015",
        color: "#f0f4ef",
        padding: "60px 24px",
        display: "grid",
        placeItems: "center",
      }}
    >
      <section style={{ maxWidth: 420, width: "100%" }}>
        <p style={{ color: "#d0f581", fontSize: 12, letterSpacing: 4, fontWeight: 700 }}>HANDS / ANDROID</p>
        <h1 style={{ fontSize: 46, lineHeight: 1.1, letterSpacing: -2, margin: "24px 0" }}>
          Less setup.
          <br />
          More rivalry.
        </h1>
        <p style={{ color: "#93a1ad", fontSize: 16, lineHeight: 1.7, marginBottom: 28 }}>
          One account for your games. Sign in once; Hands takes care of saving and syncing. Your phone&apos;s existing
          games stay with you.
        </p>
        {state === "loading" && <p role="status">Opening your account…</p>}
        {state === "signin" && (
          <button
            style={buttonStyle}
            onClick={() => {
              void signIn("github", { redirectTo: location.href });
            }}
          >
            Continue with GitHub ↗
          </button>
        )}
        {state === "approve" && (
          <>
            <p style={{ marginBottom: 20 }}>Save this phone&apos;s games to {name}&apos;s account?</p>
            <button
              style={buttonStyle}
              onClick={() => {
                void connect();
              }}
            >
              Connect & return to Hands
            </button>
          </>
        )}
        {state === "saving" && <p role="status">Connecting your account…</p>}
        {state === "done" && (
          <>
            <p>Connected. Everything else is automatic.</p>
            <a style={buttonStyle} href="historyhands://paired">
              Return to Hands ↗
            </a>
          </>
        )}
        {state === "invalid" && <p>Open Hands on your phone and tap “Sign in with GitHub” to start.</p>}
        {state === "setup" && (
          <p role="status">
            GitHub sign-in is being configured. Your games are safe on your phone; try again when setup is complete.
          </p>
        )}
        {state === "error" && (
          <>
            <p role="alert">
              Sign-in didn&apos;t finish. Your games are safe. Return to Hands and try signing in again.
            </p>
            <a style={{ ...buttonStyle, marginTop: 16 }} href="historyhands://paired?cancel=1">
              Return to Hands
            </a>
          </>
        )}
      </section>
    </main>
  );
}
