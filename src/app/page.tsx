import { GameLister } from "~/app/_components/gameLister";
import { auth } from "~/server/auth";
import { api, HydrateClient } from "~/trpc/server";
import AuthButton from "./_components/signInBtn";

export default async function Home() {
  const session = await auth();

  if (session?.user) {
    void api.game.getGames.prefetch();
  }

  return (
    <HydrateClient>
      <main className="flex flex-1 flex-col items-center justify-center">
        <div className="container flex flex-col items-center justify-center gap-12 px-4 py-16">
          {!session?.user && (
            <>
            <div className="grid grid-cols-2 gap-8">
              <div className="h-[198px] text-[12rem] rotate-12 ">
                🤜
              </div>
              <div className="h-[198px] text-[12rem] rotate-[-105deg]">
                ✌️
              </div>
              <div className="h-[198px] text-[12rem] ">
                🫲
              </div>
            </div>
              <AuthButton
                asSignIn
                className="rounded-[90px] bg-white/10 px-10 py-3 text-center font-semibold no-underline transition hover:bg-white/20"
              >
                <span className="text-xl">Sign in via discord</span>
                <br />
              </AuthButton>
            </>
          )}

          {session?.user && <GameLister />}
        </div>
      </main>
    </HydrateClient>
  );
}
