import type { Metadata } from "next";
import "@fontsource-variable/inter";
import "@fontsource-variable/space-grotesk";
import "@fontsource-variable/jetbrains-mono";
import "@fontsource/instrument-serif/400.css";
import "@fontsource/instrument-serif/400-italic.css";
import { ScrollFx } from "@/components/scroll-fx";
import "./globals.css";

export const metadata: Metadata = {
  title: "Harbor — Push your code. Get a URL.",
  description:
    "Connect a Git repository and Harbor builds it, runs it, and gives it a live address. No servers to configure, nothing to babysit.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="grain min-h-svh bg-ink">
        <script
          dangerouslySetInnerHTML={{
            __html: "document.documentElement.classList.add('js')",
          }}
        />
        {children}
        <ScrollFx />
      </body>
    </html>
  );
}
