'use client';

import Link from "next/link";
import { useState } from "react";
import { api } from "~/trpc/react"

export default function CreateInvite({ gameId }: { gameId: number }) {

    const [link, setLink] = useState('');
    const [loading, setLoading] = useState(false)

    const createInvite = api.game.createInvite.useMutation({
        onSuccess(data) {
            console.log(data)
            if (data?.id) setLink('history-of-hands.vercel.app/accept-invite/' + data.id)
        }
    })
    if (link) return <Link prefetch={false} href={'https://'+link} className="break-all underline">{link}</Link>
    return <button onClick={() => {
        setLoading(true)
        createInvite.mutate({ gameId })
    }} className={`underline`}>{loading ? 'Loading Link...' : 'Copy Invite Link'}</button>
}