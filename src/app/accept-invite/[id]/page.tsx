import AcceptInviteClient from "~/app/_components/acceptInviteClient";
import { auth } from "~/server/auth";
import { HydrateClient } from "~/trpc/server";

export default async function AcceptInvitePage({
    params,
}: {
    params: Promise<{ id: string }>
}) {
    const inviteId = (await params).id
    const session = await auth();

    return <HydrateClient>
        {session?.user ?
            <AcceptInviteClient inviteId={inviteId} />
            : <a className="flex-1 block mt-32 text-[5rem] break-words text-center mx-auto" href="/api/auth/signin">Sign In</a>

        }
    </HydrateClient>

}
