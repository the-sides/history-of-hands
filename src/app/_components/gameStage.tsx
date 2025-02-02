'use client';

import dayjs from "dayjs";
import type { LoadedGame, typeOfHand } from "../models/game";
import CardSlider from "./cardSlider";
import { useState } from "react";
import { api } from "~/trpc/react";
import { toast } from 'sonner'


const rn = dayjs();
export default function GameStage({ game }: { game: LoadedGame }) {
  const [creatorSelected, setCreatorSelected] = useState<typeOfHand | null>(null);
  const [againstSelected, setAgainstSelected] = useState<typeOfHand | null>(null);
  const bothSelected = creatorSelected && againstSelected;
  const saveHand = api.game.saveRound.useMutation({
    onSuccess(){
      setCreatorSelected(null)
      setAgainstSelected(null)
    },
    onError(){
      toast.error('Saving round failed :(')
    }
  })

  return (
    <div className="pb-24 w-[calc(100vw-20px)] flex flex-1 flex-col items-center justify-center text-3xl md:text-5xl">
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
        {bothSelected && <button className="mt-12">Confirm</button>}
    </div>
  );
}
