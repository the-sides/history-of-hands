'use client';

import { useState } from "react";
import { api } from "~/trpc/react"

export default function CreateInvite({ gameId }: { gameId: number }) {

    const [link, setLink] = useState('');
    const [loading, setLoading] = useState(false)

    const createInvite = api.game.createInvite.useMutation({
        onSuccess(data) {
            console.log(data)
            if (data?.id) setLink('localhost:3000/accept-invite/' + data.id)
        }
    })
    if (link) return <p className="break-all">{link}</p>
    return <button onClick={() => {
        setLoading(true)
        createInvite.mutate({ gameId })
    }} className={`underline`}>{loading ? 'Loading Link...' : 'Copy Invite Link'}</button>
}