CREATE TABLE "HandLog" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "mine" "ThrowType" NOT NULL,
    "hers" "ThrowType" NOT NULL,
    "playedAt" TIMESTAMP(3) NOT NULL,
    "deleted" BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT "HandLog_pkey" PRIMARY KEY ("userId", "id")
);
ALTER TABLE "HandLog" ADD CONSTRAINT "HandLog_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
