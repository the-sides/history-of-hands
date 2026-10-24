export const throws = ["ROCK", "PAPER", "SCISSORS"] as const;
export type Throw = (typeof throws)[number];
export type HandRound = { id: string; mine: Throw; hers: Throw; playedAt: string; deleted?: boolean };
export const symbols: Record<Throw, string> = { ROCK: "✊", PAPER: "✋", SCISSORS: "✌️" };
export const labels: Record<Throw, string> = { ROCK: "Rock", PAPER: "Paper", SCISSORS: "Scissors" };
const beats: Record<Throw, Throw> = { ROCK: "SCISSORS", PAPER: "ROCK", SCISSORS: "PAPER" };
export function outcome(mine: Throw, hers: Throw) {
  return mine === hers ? "tie" : beats[mine] === hers ? "win" : "loss";
}
export function activeRounds(rounds: HandRound[]) {
  return rounds
    .filter((r) => !r.deleted)
    .sort((a, b) => a.playedAt.localeCompare(b.playedAt) || a.id.localeCompare(b.id));
}
export function recommend(rounds: HandRound[]) {
  const history = activeRounds(rounds);
  const last = history.at(-1);
  const recent = history.slice(-30);
  // Use a conditional pattern only after at least eight examples of the same context.
  const conditional = last
    ? history.slice(1).filter((_, i) => {
        const previous = history[i]!;
        return previous.hers === last.hers && outcome(previous.mine, previous.hers) === outcome(last.mine, last.hers);
      })
    : [];
  const sample = conditional.length >= 8 ? conditional : recent;
  const counts = Object.fromEntries(throws.map((t) => [t, sample.filter((r) => r.hers === t).length])) as Record<
    Throw,
    number
  >;
  // Laplace smoothing avoids certainty from tiny samples. Maximize win minus loss.
  const total = sample.length + 3;
  const probability = (t: Throw) => (counts[t] + 1) / total;
  const ranked = throws
    .map((t) => ({ t, score: probability(beats[t]) - probability(throws.find((h) => beats[h] === t)!) }))
    .sort((a, b) => b.score - a.score);
  const top = ranked[0]!;
  const tied = Math.abs(top.score - ranked[2]!.score) < 0.0001;
  const predicted = throws.reduce((a, b) => (counts[a] >= counts[b] ? a : b));
  return {
    throw: top.t,
    predicted,
    counts,
    sampleSize: sample.length,
    conditional: conditional.length >= 8,
    enough: sample.length >= 8 && !tied,
    share: sample.length ? Math.round((counts[predicted] / sample.length) * 100) : 0,
    winChance: Math.round(probability(beats[top.t]) * 100),
  };
}
export function isRound(value: unknown): value is HandRound {
  if (!value || typeof value !== "object") return false;
  const r = value as Partial<HandRound>;
  return (
    typeof r.id === "string" &&
    r.id.length > 0 &&
    r.id.length <= 100 &&
    throws.includes(r.mine!) &&
    throws.includes(r.hers!) &&
    typeof r.playedAt === "string" &&
    Number.isFinite(Date.parse(r.playedAt)) &&
    (r.deleted === undefined || typeof r.deleted === "boolean")
  );
}
export function mergeRounds(local: HandRound[], remote: HandRound[]) {
  const merged = new Map<string, HandRound>();
  for (const round of [...remote, ...local]) {
    const existing = merged.get(round.id);
    merged.set(round.id, { ...round, deleted: existing?.deleted === true || round.deleted === true });
  }
  return [...merged.values()];
}
