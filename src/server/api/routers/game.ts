import { z } from "zod";

import { createTRPCRouter, protectedProcedure } from "~/server/api/trpc";

export const gameRouter = createTRPCRouter({
  create: protectedProcedure.input(z.object({ name: z.string().min(1) })).mutation(async ({ ctx, input }) => {
    return ctx.db.game.create({
      data: {
        name: input.name,
        createdBy: { connect: { id: ctx.session.user.id } },
      },
    });
  }),

  acceptInvite: protectedProcedure.input(z.object({ inviteId: z.string().cuid() })).mutation(async ({ ctx, input }) => {
    const invite = await ctx.db.invite.findFirstOrThrow({
      where: { id: input.inviteId },
    });

    const res = await ctx.db.$transaction([
      ctx.db.invite.delete({
        where: {
          id: invite.id,
        },
      }),
      ctx.db.game.update({
        where: {
          id: invite.gameId,
        },
        data: {
          against: { connect: { id: ctx.session.user.id } },
        },
      }),
    ]);
    return res[1];
  }),

  createInvite: protectedProcedure.input(z.object({ gameId: z.number() })).mutation(async ({ ctx, input }) => {
    // Confirm user owns game
    const game = await ctx.db.game.findFirstOrThrow({
      where: { id: input.gameId },
    });
    if (game.createdById !== ctx.session.user.id) throw new Error("User does not own game to send invites");

    const existingInvite = await ctx.db.invite.findFirst({
      where: {
        gameId: input.gameId,
      },
    });

    if (existingInvite) {
      console.log("FOUND?", existingInvite);
      return existingInvite;
    }

    return ctx.db.invite.create({
      data: {
        gameId: input.gameId,
      },
    });
  }),

  getGames: protectedProcedure.query(async ({ ctx }) => {
    const games = await ctx.db.game.findMany({
      orderBy: { createdAt: "desc" },
      where: { OR: [{ createdBy: { id: ctx.session.user.id } }, { against: { id: ctx.session.user.id } }] },
    });

    return games ?? null;
  }),

  getOne: protectedProcedure.input(z.object({ gameId: z.number() })).query(async ({ ctx, input }) => {
    const game = await ctx.db.game.findFirst({
      where: {
        id: input.gameId,
        OR: [{ createdBy: { id: ctx.session.user.id } }, { against: { id: ctx.session.user.id } }],
      },
    });

    const createdByUser = await ctx.db.user.findFirstOrThrow({
      where: {
        id: game?.createdById,
      },
    });

    const againstUser = await ctx.db.user.findFirst({
      where: {
        id: game?.againstId ?? "",
      },
    });

    return game && createdByUser ? { ...game, createdByUser, againstUser } : null;
  }),

  deleteOne: protectedProcedure.input(z.object({ gameId: z.number() })).mutation(async ({ ctx, input }) => {
    const res = await ctx.db.game.delete({
      where: { createdBy: { id: ctx.session.user.id }, id: input.gameId },
    });

    return res ?? null;
  }),

  saveRound: protectedProcedure
    .input(
      z.object({
        gameId: z.number(),
        creatorThrew: z.enum(["ROCK", "PAPER", "SCISSORS"]),
        againstThrew: z.enum(["ROCK", "PAPER", "SCISSORS"]),
      }),
    )
    .mutation(async ({ ctx, input }) => {
      const game = await ctx.db.game.findFirstOrThrow({
        where: { id: input.gameId },
      });

      const isParticipating = [game.createdById, game.againstId].includes(ctx.session.user.id);
      if (!isParticipating) {
        throw new Error("User is not in this game");
      }

      let winner = null;
      const beats = {
        'ROCK': 'SCISSORS',
        'PAPER': 'ROCK',
        'SCISSORS': 'PAPER'
      };
      if(beats[input.creatorThrew] === input.againstThrew) {
        winner = game.createdById;
      } else if(beats[input.againstThrew] === input.creatorThrew) {
        winner = game.againstId;
      }

      return ctx.db.round.create({
        data: {
          againstThrew: input.againstThrew,
          creatorThrew: input.creatorThrew,
          // Undefined when game is made initially, but when you're able
          // to save rounds, that's only possible when an againstId is defined
          against: { connect: { id: game.againstId ?? '' } },
          createdBy: { connect: { id: game.createdById } },
          game: { connect: { id: game.id } },
          winner: winner ? { connect: { id: winner } } : undefined,
        },
      });
    }),

  getRounds: protectedProcedure.input(z.object({ gameId: z.number() })).query(async ({ ctx, input }) => {
    // To verify or not to verify? Who cares???
    return ctx.db.round.findMany({ where: { gameId: input.gameId }, orderBy: { createdAt: "desc" } });
  }),
});
