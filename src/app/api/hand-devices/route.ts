import { NextResponse } from "next/server";
import { z } from "zod";
import { auth } from "~/server/auth";
import { db } from "~/server/db";
import { deviceHash } from "~/lib/hand-auth";
export async function POST(request: Request) {
  const origin = request.headers.get("origin");
  if (!origin || origin !== new URL(request.url).origin)
    return NextResponse.json({ error: "Invalid origin" }, { status: 403 });
  const user = (await auth())?.user;
  if (!user) return NextResponse.json({ error: "Sign in first" }, { status: 401 });
  let data;
  try {
    data = z.object({ token: z.string().regex(/^[A-Za-z0-9_-]{43}$/) }).safeParse(await request.json());
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  if (!data.success) return NextResponse.json({ error: "Invalid connection" }, { status: 400 });
  const tokenHash = deviceHash(data.data.token);
  const existing = await db.handDevice.findUnique({ where: { tokenHash } });
  if (existing && existing.userId !== user.id)
    return NextResponse.json({ error: "Device already connected to another account" }, { status: 409 });
  await db.handDevice.upsert({ where: { tokenHash }, create: { tokenHash, userId: user.id }, update: {} });
  return NextResponse.json({ ok: true });
}
export async function DELETE(request: Request) {
  const origin = request.headers.get("origin");
  if (!origin || origin !== new URL(request.url).origin)
    return NextResponse.json({ error: "Invalid origin" }, { status: 403 });
  const user = (await auth())?.user;
  if (!user) return NextResponse.json({ error: "Sign in first" }, { status: 401 });
  await db.handDevice.deleteMany({ where: { userId: user.id } });
  return NextResponse.json({ ok: true });
}
