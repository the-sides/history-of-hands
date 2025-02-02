-- AlterTable
ALTER TABLE "Round" ADD COLUMN     "userId" TEXT,
ADD COLUMN     "winnerId" TEXT;

-- AddForeignKey
ALTER TABLE "Round" ADD CONSTRAINT "Round_winnerId_fkey" FOREIGN KEY ("winnerId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Round" ADD CONSTRAINT "Round_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
