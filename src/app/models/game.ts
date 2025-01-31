import { Game, User } from "@prisma/client"

export interface LoadedGame extends Game {
    createdByUser: User
    againstUser: User
}