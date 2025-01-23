'use client';

import { api } from "~/trpc/react"

export default function CreateInvite({gameId}: {gameId: number}) {

    const createInvite = api.game.createInvite.useMutation({
        onSuccess(data) {
            console.log(data)
        }
    })
    return <button onClick={() => createInvite.mutate({gameId})} className="underline">Copy Invite Link</button>
}