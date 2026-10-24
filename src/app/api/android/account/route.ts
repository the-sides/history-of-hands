import { NextResponse } from "next/server";
import { handUser, deviceHash } from "~/lib/hand-auth";
import { db } from "~/server/db";
export async function DELETE(request: Request) {
  const user = await handUser(request);
  const token = /^Bearer ([A-Za-z0-9_-]{43})$/.exec(request.headers.get("authorization") ?? "")?.[1];
  if (!user || !token) return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  await db.handDevice.deleteMany({ where: { userId: user.id, tokenHash: deviceHash(token) } });
  return NextResponse.json({ ok: true });
}
export async function GET(request: Request) {
  const user = await handUser(request);
  const github = user
    ? await db.account.findFirst({ where: { userId: user.id, provider: "github" }, select: { id: true } })
    : null;
  return NextResponse.json(
    {
      authenticated: !!user,
      name: user?.name ?? null,
      github: !!github,
      githubReady: !!(process.env.AUTH_GITHUB_ID && process.env.AUTH_GITHUB_SECRET),
    },
    { headers: { "Cache-Control": "private, no-store" } },
  );
}
