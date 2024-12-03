import { type PageProps } from "$fresh/server.ts";
import NameModal from "../islands/NameModal.tsx";
export default function App({ Component, state }: PageProps) {
  console.log(state);
  return (
    <html>
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>QuickWish</title>
        <link rel="stylesheet" href="/styles.css" />
      </head>
      <body class="pt-12 flex min-h-screen bg-[#191f27]">
        <header class="fixed top-0 w-full text-white">
          <nav class="flex items-center p-3 bg-slate-700">
            <img src="/logo.svg" alt="QuickWish logo" class="h-8 mr-4" />
            <a href="/">Home</a>
            <p className="ml-auto">Viewing as {state.name}</p>
          </nav>
        </header>
        <Component />
        <NameModal existingName={state.name as string} />
      </body>
    </html>
  );
}
