import { NextResponse } from "next/server";
import { z } from "zod";
import { handUser } from "~/lib/hand-auth";
import { db } from "~/server/db";
import { mergeRounds, type HandRound } from "~/lib/hands";
export const dynamic = "force-dynamic";
export async function GET(request: Request) {
  const user = await handUser(request);
  if (!user) return NextResponse.json({ error: "Sign in to back up" }, { status: 401 });
  const userId = user.id;
  const [saved, legacy] = await Promise.all([
    db.handLog.findMany({ where: { userId } }),
    db.round.findMany({
      where: { OR: [{ createdById: userId }, { againstId: userId }] },
      orderBy: { createdAt: "asc" },
    }),
  ]);
  const rounds: HandRound[] = saved.map((r) => ({
    id: r.id,
    mine: r.mine,
    hers: r.hers,
    playedAt: r.playedAt.toISOString(),
    deleted: r.deleted,
  }));
  const old: HandRound[] = legacy.map((r) => ({
    id: `legacy-${r.id}`,
    mine: r.createdById === userId ? r.creatorThrew : r.againstThrew,
    hers: r.createdById === userId ? r.againstThrew : r.creatorThrew,
    playedAt: r.createdAt.toISOString(),
  }));
  return NextResponse.json(
    { name: user.name ?? "you", rounds: mergeRounds(rounds, old) },
    { headers: { "Cache-Control": "private, no-store" } },
  );
}
const bodySchema = z.object({
  rounds: z
    .array(
      z.object({
        id: z.string().min(1).max(100),
        mine: z.enum(["ROCK", "PAPER", "SCISSORS"]),
        hers: z.enum(["ROCK", "PAPER", "SCISSORS"]),
        playedAt: z.string().datetime({ offset: true }),
        deleted: z.boolean().optional(),
      }),
    )
    .max(250),
});
export async function POST(request: Request) {
  // Same-origin cookie authentication; prevent cross-site writes to the scorecard.
  const origin = request.headers.get("origin");
  if (origin && origin !== new URL(request.url).origin)
    return NextResponse.json({ error: "Invalid origin" }, { status: 403 });
  const user = await handUser(request);
  if (!user) return NextResponse.json({ error: "Sign in to back up" }, { status: 401 });
  let parsed;
  try {
    parsed = bodySchema.safeParse(await request.json());
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  if (!parsed.success) return NextResponse.json({ error: "Invalid rounds" }, { status: 400 });
  const userId = user.id;
  // Immutable rounds, monotonic tombstones: a stale device can never resurrect an undone round.
  await db.$transaction(
    parsed.data.rounds.map((r) =>
      db.handLog.upsert({
        where: { userId_id: { userId, id: r.id } },
        create: { ...r, playedAt: new Date(r.playedAt), userId },
        update: r.deleted ? { deleted: true } : {},
      }),
    ),
  );
  return NextResponse.json({ ok: true });
}
