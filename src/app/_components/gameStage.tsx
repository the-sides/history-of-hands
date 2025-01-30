import { Game } from "@prisma/client";
import CardSlider from "./cardSlider";

// This will not include the user objects I'm adding
export default function GameStage({ game }: { game: Game }) {
    return (
        <div className="flex flex-1 flex-col justify-center items-center text-8xl container">
            <CardSlider/>
            <h2>Save a New Round</h2>
        </div>
    );
}