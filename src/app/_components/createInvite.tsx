'use client';

import { useState } from "react";
import { toast } from "sonner";
import { api } from "~/trpc/react"

export default function CreateInvite({ gameId }: { gameId: number }) {
    const [link, setLink] = useState('');
    const [loading, setLoading] = useState(false);
    const [copied, setCopied] = useState(false);

    const createInvite = api.game.createInvite.useMutation({
        onSuccess(data) {
            if (data?.id) {
                const fullLink = 'https://history-of-hands.vercel.app/accept-invite/' + data.id;
                setLink(fullLink);
                navigator.clipboard.writeText(fullLink);
                toast.success('Copied invite link to clipboard!');
                // setCopied(true);
                // setTimeout(() => setCopied(false), 2000); // Reset copied state after 2 seconds
            }
        }
    })

    if (link) return <span className="break-all">{copied ? 'Copied!' : link}</span>;
    
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