import { z } from "zod";

import {
  createTRPCRouter,
  protectedProcedure,
  publicProcedure,
} from "~/server/api/trpc";

export const gameRouter = createTRPCRouter({
  hello: publicProcedure
    .input(z.object({ text: z.string() }))
    .query(({ input }) => {
      return {
        greeting: `Hello ${input.text}`,
      };
    }),

  create: protectedProcedure
    .input(z.object({ name: z.string().min(1) }))
    .mutation(async ({ ctx, input }) => {
      return ctx.db.game.create({
        data: {
          name: input.name,
          createdBy: { connect: { id: ctx.session.user.id } },
        },
      });
    }),

  acceptInvite: protectedProcedure
    .input(z.object({ inviteId: z.string().cuid() }))
    .mutation(async ({ ctx, input }) => {
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

  createInvite: protectedProcedure
    .input(z.object({ gameId: z.number() }))
    .mutation(async ({ ctx, input }) => {
      // Confirm user owns game
      const game = await ctx.db.game.findFirstOrThrow({
        where: { id: input.gameId },
      });
      if (game.createdById !== ctx.session.user.id)
        throw new Error("User does not own game to send invites");

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
      where: { OR: [{createdBy: { id: ctx.session.user.id }}, {against: { id: ctx.session.user.id }}, ]},
    });

    return games ?? null;
  }),

  getOne: protectedProcedure
    .input(z.object({ gameId: z.number() }))
    .query(async ({ ctx, input }) => {
      const game = await ctx.db.game.findFirst({
        where: { id: input.gameId, OR: [{createdBy: { id: ctx.session.user.id }}, {against: { id: ctx.session.user.id }}, ]},
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

      return game && createdByUser
        ? { ...game, createdByUser, againstUser }
        : null;
    }),

  deleteOne: protectedProcedure
    .input(z.object({ gameId: z.number() }))
    .mutation(async ({ ctx, input }) => {
      const res = await ctx.db.game.delete({
        where: { createdBy: { id: ctx.session.user.id }, id: input.gameId },
      });

      return res ?? null;
    }),

  getSecretMessage: protectedProcedure.query(() => {
    return "you can now see this secret message!";
  }),
});
