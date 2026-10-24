import assert from "node:assert/strict";
import { verifiedGitHubProfile } from "../src/server/auth/github.ts";
const fake = (emails: unknown) =>
  (async (url: RequestInfo | URL) =>
    new Response(
      JSON.stringify(
        String(url).endsWith("/emails") ? emails : { id: 42, login: "fixture", email: "untrusted@example.com" },
      ),
      { status: 200 },
    )) as typeof fetch;
const identity = await verifiedGitHubProfile(
  "fixture",
  fake([{ email: "verified@example.com", verified: true, primary: true }]),
);
assert.equal(identity.email, "verified@example.com");
await assert.rejects(() =>
  verifiedGitHubProfile("fixture", fake([{ email: "unverified@example.com", verified: false, primary: true }])),
);
await assert.rejects(() =>
  verifiedGitHubProfile("fixture", fake([{ email: "secondary@example.com", verified: true, primary: false }])),
);
console.log("GitHub linking accepts only the verified primary email, never a public or unverified claim.");

// Exercise the same normalized provider and issuer validator as Auth.js's callback.
const { createRequire } = await import("node:module");
const { dirname } = await import("node:path");
const { pathToFileURL } = await import("node:url");
const requireAuth = createRequire(import.meta.resolve("next-auth"));
const corePath = dirname(requireAuth.resolve("@auth/core"));
const { default: parseProviders } = await import(pathToFileURL(`${corePath}/lib/utils/providers.js`).href);
const oauth = await import(createRequire(`${corePath}/index.js`).resolve("oauth4webapi"));
const { githubProvider } = await import("../src/server/auth/github.ts");
const { provider } = parseProviders({
  providerId: "github",
  url: new URL("https://hoh.jacobsides.com/api/auth/callback/github"),
  config: { providers: [githubProvider("fixture-id", "fixture-secret")] },
});
const response = new URLSearchParams({ code: "fixture-code", iss: "https://github.com/login/oauth" });
const server = { issuer: provider.issuer ?? "https://authjs.dev" };
assert.equal(oauth.validateAuthResponse(server, { client_id: "fixture-id" }, response, oauth.skipStateCheck).get("code"), "fixture-code");
assert.throws(() => oauth.validateAuthResponse(server, { client_id: "fixture-id" }, new URLSearchParams({ code: "fixture-code", iss: "https://attacker.example" }), oauth.skipStateCheck));
console.log("GitHub callback accepts its documented issuer and rejects a foreign issuer.");
