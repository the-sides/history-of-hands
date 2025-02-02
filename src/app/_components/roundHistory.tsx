import { Round } from "@prisma/client";
import { TRPCClientErrorLike } from "@trpc/client";
import { UseTRPCQueryResult } from "@trpc/react-query/shared";
import dayjs from "dayjs";
import { LoadedGame } from "../models/game";

const emojis = {
  ROCK: "🪨",
  PAPER: "📝",
  SCISSORS: "✂️",
};

export default function RoundHistory({ rounds, game }: { rounds: UseTRPCQueryResult<Round[], TRPCClientErrorLike<any>>, game: LoadedGame }) {
  return (
    <div
      className="judstify-center flex w-full items-center gap-12 overflow-auto px-12 py-6 transition-opacity"
      style={{
        scrollbarWidth: "none",
        maskImage: "linear-gradient(to right, transparent, black 5%, black 95%, transparent)",
      }}
    >
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
          <div
            key={round.id}
            className="flex w-fit flex-shrink-0 flex-col items-center justify-center transition-opacity"
          >
            <p className="text-xl md:text-2xl">
              {dayjs(round.createdAt.toLocaleString()).format("M/D")}{" "}
              <span className={`pl-1 text-xl md:text-2xl ${winnerColor}`}>
                {winner == "Tie" ? winner : winner.at(0)}
              </span>
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
  );
}
