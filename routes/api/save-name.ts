import { FreshContext } from "$fresh/server.ts";

export const handler = async (req: Request, _ctx: FreshContext): Promise<Response> => {
  const body = await req.json();
  const name = body.name ?? '';

  const headers = new Headers();
  headers.append(
    "Set-Cookie",
    `name=${name}; HttpOnly; Path=/; SameSite=Lax`
  );

  return new Response(name, { headers });
};
