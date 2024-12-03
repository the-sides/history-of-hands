import { FreshContext } from "$fresh/server.ts";

export const handler = (_req: Request, ctx: FreshContext) => {
    return ctx.render(ctx.data);
}

export default function About({ data }: FreshContext) {
    return (
        <div>
            <h1>About</h1>
            <p>{data.name}</p>
        </div>
    )
}