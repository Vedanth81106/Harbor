import type { Metadata } from "next";
import { HarborScene } from "@/components/harbor-scene";
import { Logo } from "@/components/logo";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = {
  title: "Lost at sea — Harbor",
};

export default function NotFound() {
  return (
    <div className="relative isolate flex min-h-svh flex-col overflow-hidden">
      <HarborScene />

      <div className="px-4 pt-4 sm:px-8 sm:pt-6">
        <Logo />
      </div>

      <div className="flex flex-1 flex-col items-center justify-center px-6 pb-24 text-center">
        <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-secondary">
          404 · Not found
        </p>

        <h1 className="mt-6 text-balance font-serif text-[clamp(3rem,8vw,5.5rem)] leading-[0.98] tracking-[-0.02em] text-fg">
          Lost at <em className="italic text-accent-soft">sea.</em>
        </h1>

        <p className="mt-6 max-w-md text-pretty text-[17px] leading-relaxed text-muted">
          This page drifted off. The harbor is still exactly where you left it.
        </p>

        <div className="mt-9">
          <Button href="/" size="lg">
            Back to harbor
          </Button>
        </div>
      </div>
    </div>
  );
}
