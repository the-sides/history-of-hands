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

    // TODO: Question if this is beneficial
    if (session?.user) {
        void api.game.getOne.prefetch({ gameId: Number(id) });
    }

    const game = await api.game.getOne({ gameId: Number(id) })

    return <HydrateClient>
        <main className="flex min-h-[50vh] flex-col items-center ">
            <h1 className="text-display1">
                {game && game.name}
            </h1>
            <div className="container pb-32">
                <h2 className="text-display1">Players</h2>
                <div className="w-full px-20 space-y-8">
                    <p className="text-8xl text-blue-400">
                        {session?.user.name}
                    </p>
                    <div className="relative w-fit after:content-[''] after:w-full after:h-full after:absolute after:inset-0 after:translate-x-8 after:translate-y-8 after:border-white after:border-[8px] after:-z-10">
                        <input type="text" name="guest" id="guest" className="bg-[rgb(28,17,61)] border-[8px] border-white text-8xl px-7 py-6  " />
                    </div>
                </div>
            </div>
        </main>
    </HydrateClient>
}