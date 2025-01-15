"use client";

import Link from "next/link";
import { useState } from "react";

import { api } from "~/trpc/react";

export function LatestGame() {
  const [latestGame] = api.game.getLatest.useSuspenseQuery();

  const utils = api.useUtils();
  const [name, setName] = useState("");
  const createGame = api.game.create.useMutation({
    onSuccess: async () => {
      await utils.game.invalidate();
      setName("");
    },
  });

  return (
    <div className="w-full max-w-xs">
      {latestGame ? (
        <p className="truncate">Your most recent game: {latestGame.name} - <Link href={'/game/'+latestGame.id}>View</Link></p>
      ) : (
        <p>You have no games yet.</p>
      )}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          createGame.mutate({ name });
        }}
        className="flex flex-col gap-2"
      >
        <input
          type="text"
          placeholder="Title"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full rounded-full px-4 py-2 text-black"
        />
        <button
          type="submit"
          className="rounded-full bg-white/10 px-10 py-3 font-semibold transition hover:bg-white/20"
          disabled={createGame.isPending}
        >
          {createGame.isPending ? "Submitting..." : "Submit"}
        </button>
      </form>
    </div>
  );
}
