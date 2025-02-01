import type { Game, User } from "@prisma/client"

export interface LoadedGame extends Game {
    createdByUser: User
    againstUser: User
}

export type typeOfHand = 'rock' | 'scissors' | 'paper'