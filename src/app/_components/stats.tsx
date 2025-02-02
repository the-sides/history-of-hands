import type { Round, User } from "@prisma/client";
import type { LoadedGame, typeOfHand } from "../models/game";

function PlayerStats({ user, color, rounds }: { user: User; color: string; rounds: Round[] }) {
  const wonRounds = rounds.filter((round) => round.winnerId === user.id);
  const handFreqs = rounds.reduce(
    (acc, round) => {
      if (round.winnerId === null) return acc;
      return { ...acc, [round.creatorThrew]: acc[round.creatorThrew] + 1 };
    },
    { ROCK: 0, PAPER: 0, SCISSORS: 0 },
  );
  const mostPlayed = Object.entries(handFreqs).reduce(
    (acc, curr) => {
      if (acc[1] > curr[1]) {
        return acc;
      }
      return curr;
    },
    ["ROCK", 0] as [typeOfHand, number],
  );

  const { currentStreak, bestStreak } = [...rounds].reverse().reduce(
    (acc, round) => {
      if (!round.winnerId) return acc; // ignore ties

      if (round.winnerId === user.id) {
        const newCurrentStreak = acc.currentStreak + 1;
        return {
          currentStreak: newCurrentStreak,
          bestStreak: Math.max(acc.bestStreak, newCurrentStreak),
        };
      } else {
        return {
          ...acc,
          currentStreak: 0,
        };
      }
    },
    { currentStreak: 0, bestStreak: 0 },
  );

  return (
    <div className="flex-1 px-6 md:px-12">
      <p className={`text-3xl md:text-5xl ${color}`}>{user.name}</p>
      <p className="text-2xl md:text-4xl">Rounds Won: {wonRounds.length}</p>
      <p className="text-2xl md:text-4xl">
        Most Played Hand: {mostPlayed[0]} ({mostPlayed[1]})
      </p>
      <p className="text-2xl md:text-4xl">Current Streak: {currentStreak}</p>
      <p className="text-2xl md:text-4xl">Best Streak: {bestStreak}</p>
    </div>
  );
}
export default function Stats({ game, rounds }: { game: LoadedGame; rounds: Round[] }) {
  if (!game.createdByUser || !game.againstUser) {
    return <div>Loading...</div>;
  }
  return (
    <div className="mt-6 space-y-4 md:space-y-0 md:flex w-full justify-around">
      <PlayerStats user={game.createdByUser} color="text-blue-400" rounds={rounds}></PlayerStats>
      <PlayerStats user={game.againstUser} color="text-red-400" rounds={rounds}></PlayerStats>
    </div>
  );
}
