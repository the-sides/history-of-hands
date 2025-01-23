import CreateInvite from "~/app/_components/createInvite";
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
            {game && 
                <div className="container pb-32">
                <h1 className="text-display1">
                    Game: {game.name}
                </h1>
                    <h2 className="text-display1">Players:</h2>
                    <div className="w-full px-20 space-y-8 text-8xl">
                        <p className=" text-blue-400">
                            {game.createdByUser?.name}
                        </p>
                        {
                            game.againstUser ?
                                <p className=" text-blue-400">
                                    {game?.againstUser?.name}
                                </p> :
                                <CreateInvite gameId={game.id} />
                        }
                    </div>
                </div>}
        </main>
    </HydrateClient>
}