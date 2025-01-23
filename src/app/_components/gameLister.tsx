"use client";

import Link from "next/link";
import { useState } from "react";

import { api } from "~/trpc/react";

export function GameLister() {
  const [games] = api.game.getGames.useSuspenseQuery();

  const utils = api.useUtils();
  const [name, setName] = useState("");
  const [isBeingDeleted, setIsBeingDeleted] = useState<number[]>([])
  const createGame = api.game.create.useMutation({
    onSuccess: async () => {
      await utils.game.invalidate();
      setName("");
    },
  });


  const deleteGame = api.game.deleteOne.useMutation({
    onSuccess: async () => {
      // Invalidate and refetch games after successful deletion
      await utils.game.invalidate();
    },
  });


  return (
    <div className="w-full text-[2rem] md:text-[4rem] max-w-fit">
      {games?.length > 0 ? (
        games.map((game, gameInd) =>
          <p key={'game-' + gameInd} className={isBeingDeleted.includes(game.id) ? 'opacity-50 cursor-not-allowed' : ''}>
            <button onClick={() => {
              setIsBeingDeleted([...isBeingDeleted, game.id])
              deleteGame.mutate({ gameId: game.id })
            }}>(x)</button> {' '}
            {game.name} - <Link href={'/game/' + game.id}>View</Link></p>
        )
      ) : (
        <p>You have no games yet.</p>
      )}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          createGame.mutate({ name });
        }}
        className="flex flex-col gap-2 mt-24"
      >
        {/* <input
          type="text"
          placeholder="Title"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full rounded-full px-4 py-2 text-black"
        /> */}
        <div className="flex item-center flex-col md:flex-row md:gap-x-20">
          <div className="relative w-fit after:content-[''] after:w-full after:h-full after:absolute after:inset-0 after:translate-x-8 after:translate-y-8 after:border-white after:border-[8px] after:-z-10">
            <input type="text"
              value={name}
              placeholder="Name a new game"
              onChange={(e) => setName(e.target.value)} name="guest" id="guest" className="bg-[rgb(28,17,61)] border-[8px] border-white  px-7 py-6  " />
          </div>
          <button
            type="submit"
            className="rounded-full bg-white/10 mt-12 px-10 py-3 transition hover:bg-white/20"
            disabled={createGame.isPending}
          >
            {createGame.isPending ? "Submitting..." : "Create"}
          </button>
        </div>
      </form>
    </div>
  );
}
