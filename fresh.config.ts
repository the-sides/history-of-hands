import { defineConfig } from "$fresh/server.ts";
import tailwind from "$fresh/plugins/tailwind.ts";
import { authPlugin } from "./plugins/auth.ts";

// Set up some types we'll reference later
export type SignedInState = {
  session: Session;
};

export type SignedOutState = {
  session?: null;
};

export type AuthState = SignedInState | SignedOutState;
export default defineConfig({
  plugins: [authPlugin, tailwind()],
});
