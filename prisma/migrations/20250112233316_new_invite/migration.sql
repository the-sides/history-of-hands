-- DropForeignKey
ALTER TABLE "Game" DROP CONSTRAINT "Game_againstId_fkey";

-- AlterTable
ALTER TABLE "Game" ALTER COLUMN "againstId" DROP NOT NULL;

-- CreateTable
CREATE TABLE "Invite" (
    "id" TEXT NOT NULL,
    "gameId" INTEGER NOT NULL,
    "email" TEXT NOT NULL,

    CONSTRAINT "Invite_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Invite_gameId_key" ON "Invite"("gameId");

-- AddForeignKey
ALTER TABLE "Game" ADD CONSTRAINT "Game_againstId_fkey" FOREIGN KEY ("againstId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Invite" ADD CONSTRAINT "Invite_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
