import { Game } from "@prisma/client";
import CreateInvite from "~/app/_components/createInvite";
import GameStage from "~/app/_components/gameStage";
import { LoadedGame } from "~/app/models/game";
import { auth } from "~/server/auth";
import { api, HydrateClient } from "~/trpc/server";


export default async function GamePage({
    params,
}: {
    params: Promise<{ id: number }>
}) {
    const id = (await params).id
    const session = await auth();

    if (session?.user) {
        void api.game.getOne.prefetch({ gameId: Number(id) });
    }

    const game = await api.game.getOne({ gameId: Number(id) })

    return (
        <HydrateClient>
            <main className="flex min-h-[50vh] flex-col items-center">
                {renderGameSwitch(game as LoadedGame)}
            </main>
        </HydrateClient>
    );
}

function renderGameSwitch(game: LoadedGame) {
    if (!game) {
        return (
            <p className="flex-1 block mt-32 text-[5rem] break-words text-center mx-auto">
                Loading Game...
            </p>
        );
    }

    if (!game.againstUser) {
        return (
            <div className="container pb-32">
                <h2 className="text-display1">Players:</h2>
                <div className="w-full px-20 space-y-8 text-8xl">
                    <p className="text-blue-400">{game.createdByUser?.name}</p>
                    <CreateInvite gameId={game.id} />
                </div>
            </div>
        );
    }

    return (<main className="min-h-[calc(100vh-144px)] flex flex-col">
            <div className="flex gap-x-5 w-full justify-center text-4xl md:text-7xl container mx-auto">
                <p className="flex-1 text-right text-blue-400">{game.createdByUser?.name}</p>
                vs.
                <p className="flex-1 text-left text-red-400">{game.againstUser?.name}</p>
            </div>
            <GameStage game={game}></GameStage>
        </main>
    );
}