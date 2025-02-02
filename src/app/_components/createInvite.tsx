'use client';

import { useState } from "react";
import { toast } from "sonner";
import { api } from "~/trpc/react"

export default function CreateInvite({ gameId }: { gameId: number }) {
    const [link, setLink] = useState('');
    const [loading, setLoading] = useState(false);

    const createInvite = api.game.createInvite.useMutation({
        async onSuccess(data) {
            if (data?.id) {
                const fullLink = 'https://history-of-hands.vercel.app/accept-invite/' + data.id;
                setLink(fullLink);
                await navigator.clipboard.writeText(fullLink);
                toast.success('Copied invite link to clipboard!');
            }
        }
    })

    if (link) return <span className="break-all">{link}</span>;
    
    return <button 
        onClick={() => {
            setLoading(true);
            createInvite.mutate({ gameId });
        }} 
        className="underline"
    >
        {loading ? 'Loading Link...' : 'Copy Invite Link'}
    </button>
}