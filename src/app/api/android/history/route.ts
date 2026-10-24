import { NextResponse } from "next/server";
import { z } from "zod";
import { handUser } from "~/lib/hand-auth";
import { db } from "~/server/db";
export const dynamic = "force-dynamic";
export async function GET(request: Request) {
  const user = await handUser(request);
  if (!user) return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  const [saved, legacy, github] = await Promise.all([
    db.handLog.findMany({ where: { userId: user.id } }),
    db.round.findMany({ where: { OR: [{ createdById: user.id }, { againstId: user.id }] } }),
    db.account.findFirst({ where: { userId: user.id, provider: "github" }, select: { id: true } }),
  ]);
  const rounds = new Map(
    legacy.map((r) => [
      `legacy-${r.id}`,
      {
        id: `legacy-${r.id}`,
        mine: r.createdById === user.id ? r.creatorThrew : r.againstThrew,
        hers: r.createdById === user.id ? r.againstThrew : r.creatorThrew,
        playedAt: r.createdAt.toISOString(),
        deleted: false,
        revision: 0,
      },
    ]),
  );
  for (const r of saved)
    rounds.set(r.id, {
      id: r.id,
      mine: r.mine,
      hers: r.hers,
      playedAt: r.playedAt.toISOString(),
      deleted: r.deleted,
      revision: Number(r.revision),
    });
  return NextResponse.json(
    { rounds: [...rounds.values()], user: { id: user.id, name: user.name ?? "You", github: !!github }, syncFormat: 2 },
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
        deleted: z.boolean().default(false),
        baseRevision: z.number().int().min(-1).max(Number.MAX_SAFE_INTEGER).optional(),
        revision: z.number().int().min(0).max(Number.MAX_SAFE_INTEGER).default(0),
      }),
    )
    .max(250),
});
export async function POST(request: Request) {
  const origin = request.headers.get("origin");
  if (origin && origin !== new URL(request.url).origin)
    return NextResponse.json({ error: "Invalid origin" }, { status: 403 });
  const user = await handUser(request);
  if (!user) return NextResponse.json({ error: "Sign in required" }, { status: 401 });
  let parsed;
  try {
    parsed = bodySchema.safeParse(await request.json());
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  if (!parsed.success) return NextResponse.json({ error: "Invalid games" }, { status: 400 });
  const conflicts: unknown[] = [];
  const accepted: string[] = [];
  await db.$transaction(
    async (transaction) => {
      for (const r of parsed.data.rounds) {
        const revision = BigInt(r.revision);
        const fields = { mine: r.mine, hers: r.hers, playedAt: new Date(r.playedAt), deleted: r.deleted, revision };
        if (r.baseRevision !== undefined) {
          const current = await transaction.handLog.findUnique({ where: { userId_id: { userId: user.id, id: r.id } } });
          const same = current && current.mine === r.mine && current.hers === r.hers && current.deleted === r.deleted && current.playedAt.getTime() === fields.playedAt.getTime();
          const conflict = (value: NonNullable<typeof current>) => conflicts.push({ id: value.id, mine: value.mine, hers: value.hers, deleted: value.deleted, playedAt: value.playedAt.toISOString(), revision: Number(value.revision) });
          if (same && current.revision === revision) { accepted.push(r.id); continue; }
          if (current && (Number(current.revision) !== r.baseRevision || revision <= current.revision)) { conflict(current); continue; }
          if (current) {
            const updated = await transaction.handLog.updateMany({ where: { userId: user.id, id: r.id, revision: BigInt(r.baseRevision) }, data: fields });
            if (updated.count) accepted.push(r.id);
            else { const latest = await transaction.handLog.findUniqueOrThrow({ where: { userId_id: { userId: user.id, id: r.id } } }); conflict(latest); }
          } else {
            // A legacy round has an implicit base revision of zero; new IDs use -1.
            if (r.baseRevision !== -1 && !(r.id.startsWith("legacy-") && r.baseRevision === 0))
              throw new Error("Server record unavailable");
            const created = await transaction.handLog.createMany({ data: [{ ...fields, userId: user.id, id: r.id }], skipDuplicates: true });
            if (created.count) accepted.push(r.id);
            else { const latest = await transaction.handLog.findUniqueOrThrow({ where: { userId_id: { userId: user.id, id: r.id } } }); conflict(latest); }
          }
          continue;
        }
        await transaction.handLog.createMany({
          data: [{ ...fields, userId: user.id, id: r.id }],
          skipDuplicates: true,
        });
        await transaction.handLog.updateMany({
          where: {
            userId: user.id,
            id: r.id,
            OR: [{ revision: { lt: revision } }, ...(r.revision === 0 && r.deleted ? [{ revision: BigInt(0) }] : [])],
          },
          data: fields,
        });
      }
    },
    { timeout: 30000 },
  );
  return NextResponse.json({ ok: true, accepted, conflicts });
}
