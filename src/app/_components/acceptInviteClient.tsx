'use client';
import { useRouter } from "next/navigation";
import { useRef } from "react";
import { api } from "~/trpc/react";

export default function AcceptInviteClient({ inviteId }: { inviteId: string }) {
    const router = useRouter();
    const accepted = useRef(false);

    const acceptInvite = api.game.acceptInvite.useMutation({
        onSuccess(data) {
            // Redirect to game page after accepting
            router.push(`/game/${data.id}`)
        }
    });
    if (!accepted.current) {
        accepted.current = true; acceptInvite.mutate({ inviteId })
    }
    return <p className="flex-1 block mt-32 text-[5rem] break-words text-center mx-auto">Accepting Invite...</p>
}
