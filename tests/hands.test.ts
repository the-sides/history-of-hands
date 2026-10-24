import assert from "node:assert/strict";
import { activeRounds, isRound, mergeRounds, outcome, recommend, throws, type HandRound } from "../src/lib/hands.ts";
const expected = [
  ["tie", "loss", "win"],
  ["win", "tie", "loss"],
  ["loss", "win", "tie"],
];
for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) assert.equal(outcome(throws[i]!, throws[j]!), expected[i]![j]);
const round = (id: number, hers: HandRound["hers"] = "ROCK"): HandRound => ({
  id: String(id),
  mine: "PAPER",
  hers,
  playedAt: new Date(1700000000000 + id * 1000).toISOString(),
});
assert.equal(recommend([]).enough, false);
assert.equal(recommend(Array.from({ length: 7 }, (_, i) => round(i))).enough, false);
assert.equal(recommend(Array.from({ length: 10 }, (_, i) => round(i))).throw, "PAPER");
assert.equal(recommend(Array.from({ length: 10 }, (_, i) => round(i))).enough, true);
assert.equal(recommend(Array.from({ length: 9 }, (_, i) => round(i, throws[i % 3]!))).enough, false);
assert.equal(activeRounds([{ ...round(0), deleted: true }, round(1)]).length, 1);
assert.equal(mergeRounds([{ ...round(0), deleted: true }], [round(0)])[0]!.deleted, true);
assert.equal(mergeRounds([round(0)], [{ ...round(0), deleted: true }])[0]!.deleted, true);
assert.equal(mergeRounds([round(0)], [round(0)]).length, 1);
assert.equal(isRound({ ...round(0), mine: "LIZARD" }), false);
assert.equal(isRound({ ...round(0), playedAt: "yesterday" }), false);
assert.equal(isRound(round(0)), true);
// If she mostly plays scissors, recommend rock; don't merely counter her most frequent
// throw when the expected loss tradeoff makes another choice better.
assert.equal(recommend(Array.from({ length: 30 }, (_, i) => round(i, "SCISSORS"))).throw, "ROCK");
console.log("All outcome, advice, validation, merge, and tombstone checks passed.");
