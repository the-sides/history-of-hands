import "~/styles/globals.css";

import { GeistSans } from "geist/font/sans";
import { type Metadata } from "next";

import { TRPCReactProvider } from "~/trpc/react";
import Nav from "./_components/Nav";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "History of Hands",
  description: "Rock-paper-scissors Tracker",
  icons: [{ rel: "icon", url: "/favicon.ico" }],
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${GeistSans.variable}`}>
      <body className="min-h-screen flex flex-col bg-gradient-to-br from-[#532A73] to-[#081B59] text-white">
        <TRPCReactProvider>
          <Nav />
          <Toaster />
          {children}</TRPCReactProvider>
      </body>
    </html>
  );
}
