"use client";

import { Button } from "@/components/ui/button";
import { HarborScene } from "@/components/harbor-scene";
import { useAuth } from "@/components/auth/auth-provider";

export function Hero() {
  const { openSignup, user } = useAuth();

  return (
    <section className="relative isolate flex min-h-[calc(100svh-80px)] items-center overflow-hidden">
      <HarborScene />

      <div className="mx-auto w-full max-w-4xl px-6 pb-32 pt-20 text-center sm:pt-24">
        <h1 className="rise text-[clamp(3.25rem,8.4vw,6.5rem)] font-medium leading-[0.92] tracking-[-0.045em] text-fg">
          <span className="block">HARBOR</span>
          <span className="block text-accent-soft">A self-hosted PaaS</span>
        </h1>

        <p className="rise mx-auto mt-8 max-w-[36rem] text-[18px] leading-relaxed text-muted [animation-delay:180ms] sm:text-[19px]">
          A self-hosted PaaS for my homelab. Point it at a Git repo and Harbor
          builds it with Docker and gives it a live URL on your own server.
        </p>

        <div className="rise mt-10 flex flex-col items-center justify-center gap-3 [animation-delay:300ms] sm:flex-row">
          {user ? (
            <Button
              href="/dashboard"
              size="lg"
              className="btn-ship w-full sm:w-auto"
            >
              Open dashboard
            </Button>
          ) : (
            <Button
              size="lg"
              className="btn-ship w-full sm:w-auto"
              onClick={openSignup}
            >
              Get started
            </Button>
          )}
          <Button
            href="#how-it-works"
            variant="secondary"
            size="lg"
            className="btn-ship w-full sm:w-auto"
          >
            See how it works
          </Button>
        </div>
      </div>
    </section>
  );
}
