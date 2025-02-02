"use client";

import dayjs from "dayjs";
import type { LoadedGame, typeOfHand } from "../models/game";
import CardSlider from "./cardSlider";
import { useState } from "react";
import { api } from "~/trpc/react";
import { toast } from "sonner";

// const winner = (creatorSelected: typeOfHand, againstSelected: typeOfHand) => {

// }
const emojis = {
  ROCK: "🪨",
  PAPER: "📝",
  SCISSORS: "✂️",
};

export default function GameStage({ game }: { game: LoadedGame }) {
  const rn = dayjs();
  const rounds = api.game.getRounds.useQuery({ gameId: game.id });
  console.log("rounds", rounds);
  const [creatorSelected, setCreatorSelected] = useState<typeOfHand | null>(null);
  const [againstSelected, setAgainstSelected] = useState<typeOfHand | null>(null);
  const bothSelected = creatorSelected && againstSelected;
  const saveHand = api.game.saveRound.useMutation({
    onSuccess() {
      setCreatorSelected(null);
      setAgainstSelected(null);
      rounds.refetch();
      toast.success("Round saved!");
    },
    onError() {
      toast.error("Saving round failed :(");
    },
  });

  return (
    <div className="flex w-[calc(100vw-20px)] flex-1 flex-col items-center justify-center pb-24 text-3xl md:text-5xl">
      <h2 className="mt-12">Save a New Round</h2>
      <h4 className="mt-4 text-4xl">{rn.format("MMM DD")}</h4>
      <h5 className="text-2xl">{rn.format("h:mm a")}</h5>

      <div className="flex w-full justify-between overflow-hidden md:-mt-24">
        <CardSlider
          selected={creatorSelected}
          handleSelect={(value) => setCreatorSelected(value)}
          user={game.createdByUser}
          isOwner={true}
        />
        <CardSlider
          selected={againstSelected}
          handleSelect={(value) => setAgainstSelected(value)}
          user={game.againstUser}
          isOwner={false}
        />
      </div>
      {bothSelected && (
        <button
          onClick={() =>
            saveHand.mutate({ gameId: game.id, creatorThrew: creatorSelected, againstThrew: againstSelected })
          }
          className="mt-12"
        >
          Confirm
        </button>
      )}

      <h4 className="mt-24 w-full text-center">History</h4>
      <div className=" flex w-full gap-12 px-12 py-6 overflow-auto items-center judstify-center transition-opacity">
        {rounds.data?.map((round) => {
          let winner = "Tie";
          let winnerColor = "text-neutral-400";
          if (round.winnerId === game.createdByUser?.id) {
            winner = game.createdByUser.name as string;
            winnerColor = "text-blue-400";
          } else if (round.winnerId === game.againstUser?.id) {
            winner = game.againstUser?.name as string;
            winnerColor = "text-red-400";
          }
          // const winnerElm = winner !== 'tie' ? <p className={`text-5xl md:text-9xl ${winnerColor}`}>{winner} Won </p>
          return (
            <div key={round.id} className="flex flex-shrink-0 w-fit flex-col items-center justify-center transition-opacity">
              <p className="text-xl md:text-2xl">
                {dayjs(round.createdAt.toLocaleString()).format("M/D")}
                {" "}
                <span className={`pl-1 text-xl md:text-2xl ${winnerColor}`}>{winner == 'Tie' ? winner : winner.at(0)}</span>
                {winner !== "Tie" ? " Won" : ""}
              </p>
              <div className="flex gap-2">
                <p className="text-xl md:text-2xl">{emojis[round.creatorThrew]}</p>
                <p className="text-xl md:text-2xl">{emojis[round.againstThrew]}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
