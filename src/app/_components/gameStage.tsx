import { Game } from "@prisma/client";

// This will not include the user objects I'm adding
export default function GameStage({ game }: { game: Game }) {
    return (
        <div className="flex flex-col items-center text-8xl container">
            <h2>Save a New Round</h2>
        </div>
    );
}