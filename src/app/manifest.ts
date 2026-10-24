import type { MetadataRoute } from "next";
export default function manifest(): MetadataRoute.Manifest {
  return {
    id: "/",
    name: "History of Hands",
    short_name: "Hands",
    description: "Your ongoing rock, paper, scissors rivalry.",
    start_url: "/",
    scope: "/",
    display: "standalone",
    background_color: "#f4f0e7",
    theme_color: "#f4f0e7",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icons/maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
    shortcuts: [
      {
        name: "Log a round",
        short_name: "Log round",
        url: "/?action=log",
        icons: [{ src: "/icons/icon-192.png", sizes: "192x192" }],
      },
      { name: "What's my next move?", short_name: "Next move", url: "/?action=advice" },
    ],
  };
}
