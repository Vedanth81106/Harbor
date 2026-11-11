import type { Metadata } from "next";
import "@fontsource-variable/inter";
import "@fontsource-variable/space-grotesk";
import "@fontsource-variable/jetbrains-mono";
import "@fontsource/instrument-serif/400.css";
import "@fontsource/instrument-serif/400-italic.css";
import { ScrollFx } from "@/components/scroll-fx";
import { DriftingAnchor } from "@/components/drifting-anchor";
import "./globals.css";

export const metadata: Metadata = {
  title: "Harbor — Push your code. Get a URL.",
  description:
    "Connect a Git repository and Harbor builds it, runs it, and gives it a live address. No servers to configure, nothing to babysit.",
};

/* Runs before the body paints, so the first frame is already the right theme. */
const bootScript = `(function(){var d=document.documentElement;d.classList.add("js");try{var t=localStorage.getItem("theme");if(t!=="light"&&t!=="dark"){t=window.matchMedia("(prefers-color-scheme: light)").matches?"light":"dark"}d.dataset.theme=t}catch(e){d.dataset.theme="dark"}})();`;

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="grain min-h-svh">
        <script dangerouslySetInnerHTML={{ __html: bootScript }} />
        {children}
        <ScrollFx />
        <DriftingAnchor />
      </body>
    </html>
  );
}
