import { NextPage } from "next";
import Link from "next/link";

import { LatestGame } from "~/app/_components/game";
import { auth } from "~/server/auth";
import { api, HydrateClient } from "~/trpc/server";

export default async function GamePage({
    params,
}: {
    params: Promise<{ id: number }>
}) {
    const id = (await params).id
    const session = await auth();

    if (session?.user && id) {
        void api.game.getOne({gameId: id}).prefetch();
    }
    return <HydrateClient>
        <main className=""></main>
    </HydrateClient>
}