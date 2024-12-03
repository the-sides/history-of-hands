import { FreshContext, PageProps } from "$fresh/server.ts";
import { getList } from "../../lib/lists.ts";

export const handler = async (_req: Request, ctx: FreshContext) => {
    const name = ctx.params.name;
    const list = await getList(name);
    console.log(name, list);
    return ctx.render(ctx.data);
}

export default function List({ params }: PageProps) {
    return (
        <div class={'py-8 px-6 text-white'}>
            <h1>{params.name}</h1>
        </div>
    )
}