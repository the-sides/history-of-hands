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

  sendInvite: protectedProcedure
    .input(z.object({ email: z.string().default('N/A'), gameId: z.number() }))
    .mutation(async ({ ctx, input }) => {
      // Confirm user owns game
      const game = await ctx.db.game.findFirstOrThrow({
        where: { id: input.gameId }
      })
      if (game.createdById !== ctx.session.user.id)
        throw new Error('User does not own game to send invites')

      return ctx.db.invite.create({
        data: {
          email: input.email,
          gameId: input.gameId
        }
      })
    }),

  getGames: protectedProcedure.query(async ({ ctx }) => {
    const games = await ctx.db.game.findMany({
      orderBy: { createdAt: "desc" },
      where: { createdBy: { id: ctx.session.user.id } },
    });

    return games ?? null;
  }),

  getOne: protectedProcedure
    .input(z.object({ gameId: z.number() }))
    .query(async ({ ctx, input }) => {
      const game = await ctx.db.game.findFirst({
        where: { createdBy: { id: ctx.session.user.id }, id: input.gameId },
      });

      return game ?? null;
    }),

    deleteOne: protectedProcedure
    .input(z.object({ gameId: z.number() }))
    .mutation(async ({ ctx, input }) => {
      const game = await ctx.db.game.delete({
        where: { createdBy: { id: ctx.session.user.id }, id: input.gameId },
      });

      return game ?? null;
    }),

  getSecretMessage: protectedProcedure.query(() => {
    return "you can now see this secret message!";
  }),
});
