import { createHash } from "node:crypto";
import { auth } from "~/server/auth";
import { db } from "~/server/db";
export const deviceHash = (token: string) => createHash("sha256").update(token).digest("hex");
export async function handUser(request: Request) {
  const authorization = request.headers.get("authorization");
  if (authorization) {
    const match = /^Bearer ([A-Za-z0-9_-]{43})$/.exec(authorization);
    if (!match) return null;
    const device = await db.handDevice.findUnique({
      where: { tokenHash: deviceHash(match[1]!) },
      include: { user: { select: { id: true, name: true } } },
    });
    return device?.user ?? null;
  }
  return (await auth())?.user ?? null;
}
