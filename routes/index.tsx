import { useSignal } from "@preact/signals";
import NameModal from "../islands/NameModal.tsx";
import { Handlers, PageProps } from "$fresh/server.ts";

export const handler: Handlers = {
  GET(_req, ctx) {
    console.log("State from index.tsx:", ctx.state.name);
    return ctx.render(ctx.state);
  },
};

export default function Home({ data }: PageProps) {
  const slugName = data.name.replace(/\s+/g, "-").toLowerCase();
  return (
    <main class={"flex-1 min-h-full"}>
      <div class="px-4 py-8 mx-auto text-white">
        <a href={"list/" + slugName} className="">Create List</a>
      </div>
    </main>
  );
}
