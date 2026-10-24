import GitHubProvider, { type GitHubProfile } from "next-auth/providers/github";
import { z } from "zod";

// Only GitHub's verified primary email can join an existing scorecard. Public profile
// email alone is not proof of ownership and must never enable account linking.
export async function verifiedGitHubProfile(
  accessToken: string,
  fetcher: typeof fetch = fetch,
): Promise<GitHubProfile> {
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    "User-Agent": "Hands",
    Accept: "application/vnd.github+json",
  };
  const responses = await Promise.all([
    fetcher("https://api.github.com/user", { headers }),
    fetcher("https://api.github.com/user/emails", { headers }),
  ]);
  if (responses.some((response) => !response.ok)) throw new Error("GitHub identity verification failed");
  const profile = (await responses[0].json()) as GitHubProfile;
  const emails = z
    .array(z.object({ email: z.string().email(), verified: z.boolean(), primary: z.boolean() }))
    .parse(await responses[1].json());
  const email = emails.find((value) => value.primary && value.verified)?.email;
  if (!email || !Number.isInteger(profile.id) || !profile.login)
    throw new Error("A verified primary GitHub email is required");
  return { ...profile, email };
}
export function githubProvider(clientId: string, clientSecret: string) {
  return GitHubProvider({
    clientId,
    clientSecret,
    allowDangerousEmailAccountLinking: true,
    userinfo: {
      url: "https://api.github.com/user",
      request: async ({ tokens }: { tokens: { access_token?: string } }) => {
        if (!tokens.access_token) throw new Error("Missing GitHub authorization");
        return verifiedGitHubProfile(tokens.access_token);
      },
    },
  });
}
