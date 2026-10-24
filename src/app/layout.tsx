import "~/styles/globals.css";
import { type Metadata, type Viewport } from "next";
import { TRPCReactProvider } from "~/trpc/react";
export const metadata: Metadata = {
  title: "History of Hands · You & her",
  description: "Two throws. One little rivalry. Log rock, paper, scissors and find your next move.",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, title: "Hands", statusBarStyle: "default" },
  icons: { icon: "/icons/icon-192.png", apple: "/icons/icon-192.png" },
};
export const viewport: Viewport = { width: "device-width", initialScale: 1, themeColor: "#f4f0e7" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <TRPCReactProvider>{children}</TRPCReactProvider>
      </body>
    </html>
  );
}
