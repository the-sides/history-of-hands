import { type Config, defineConfig } from "drizzle-kit";
import { env } from "~/env";

console.log({
  url: env.TURSO_DATABASE_URL,
  authToken: env.TURSO_AUTH_TOKEN,
})
export default defineConfig({
  schema: './src/server/db/schema.ts',
  dialect: 'turso',
  dbCredentials: {
    url: env.TURSO_DATABASE_URL,
    authToken: env.TURSO_AUTH_TOKEN,
  },
});
