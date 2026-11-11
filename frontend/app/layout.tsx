import type { Metadata, Viewport } from "next";
import "@fontsource-variable/inter";
import { ScrollFx } from "@/components/scroll-fx";
import { DriftingAnchor } from "@/components/drifting-anchor";
import { AuthProvider } from "@/components/auth/auth-provider";
import "./globals.css";

const description =
  "Connect a Git repository and Harbor builds it, runs it, and gives it a live address. No servers to configure, nothing to babysit.";

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000",
  ),
  title: "Harbor",
  description,
  applicationName: "Harbor",
  openGraph: {
    type: "website",
    siteName: "Harbor",
    title: "Harbor — Push your code. Get a URL.",
    description,
    url: "/",
  },
  twitter: {
    card: "summary_large_image",
    title: "Harbor — Push your code. Get a URL.",
    description,
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#eff2f5" },
    { media: "(prefers-color-scheme: dark)", color: "#07101b" },
  ],
};

/* Runs before the body paints, so the first frame is already the right theme. */
const bootScript = `(function(){var d=document.documentElement;d.classList.add("js");try{var t=localStorage.getItem("theme");if(t!=="light"&&t!=="dark"){t=window.matchMedia("(prefers-color-scheme: light)").matches?"light":"dark"}d.dataset.theme=t}catch(e){d.dataset.theme="dark"}})();`;

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="grain min-h-svh">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[100] focus:rounded-full focus:bg-accent focus:px-4 focus:py-2 focus:text-[14px] focus:font-medium focus:text-on-accent"
        >
          Skip to content
        </a>
        <script dangerouslySetInnerHTML={{ __html: bootScript }} />
        <AuthProvider>
          {children}
          <ScrollFx />
          <DriftingAnchor />
        </AuthProvider>
      </body>
    </html>
  );
}
