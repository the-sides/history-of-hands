
import dayjs from "dayjs";
import type { LoadedGame } from "../models/game";
import CardSlider from "./cardSlider";

const rn = dayjs()
export default function GameStage({ game }: { game: LoadedGame }) {
    return (
        <div className="flex flex-1 flex-col justify-center items-center text-3xl md:text-5xl w-[calc(100vw-20px)]">
            <h2 className="mt-12">Save a New Round</h2>
            <h4 className="text-4xl mt-4">{rn.format('MMM DD')}</h4>
            <h5 className="text-2xl">{rn.format('h:mm a')}</h5>
            

            <div className="md:-mt-24 flex justify-between w-full overflow-hidden">
                <CardSlider user={game.createdByUser} isOwner />
                <CardSlider user={game.againstUser} isOwner={false} />
            </div>
        </div>
    );
}