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
