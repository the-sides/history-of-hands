import { FreshContext } from "$fresh/server.ts";

interface State {
  name: string;
}

export async function handler(
  req: Request,
  ctx: FreshContext<State>,
) {
  const cookie = req.headers.get("Cookie");
  const name = cookie?.split("; ").find(row => row.startsWith("name="))?.split("=")[1] || '';
  ctx.state.name = name;
  
  return await ctx.next();
}
