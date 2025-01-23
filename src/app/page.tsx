import Link from "next/link";

import { LatestGame } from "~/app/_components/game";
import { auth } from "~/server/auth";
import { api, HydrateClient } from "~/trpc/server";

export default async function Home() {
  const hello = await api.game.hello({ text: "from tRPC" });
  const session = await auth();

  if (session?.user) {
    void api.game.getLatest.prefetch();
  }

  return (
    <HydrateClient>
      <main className="flex flex-1 flex-col items-center justify-center">
        <div className="container flex flex-col items-center justify-center gap-12 px-4 py-16">
              {!session?.user && (
                <a
                  href="/api/auth/signin"
                  className="rounded-[90px] bg-white/10 px-10 text-[2rem] text-center py-3 font-semibold no-underline transition hover:bg-white/20"
                >
                  
                  <span className="text-[7rem] ">Sign in</span>
                  <br />
                  Uses discord, signs-up in the process
                </a>
              )}

          {session?.user && <LatestGame />}
        </div>
      </main>
    </HydrateClient>
  );
}
