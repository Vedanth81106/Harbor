import { Button } from "@/components/ui/button";
import { HarborScene } from "@/components/harbor-scene";

export function Hero() {
  return (
    <section className="relative isolate flex min-h-[calc(100svh-80px)] items-center overflow-hidden">
      <HarborScene />

      <div className="mx-auto w-full max-w-4xl px-6 pb-32 pt-20 text-center sm:pt-24">
        <h1 className="rise font-display text-[clamp(3.25rem,8.4vw,6.5rem)] font-medium leading-[0.92] tracking-[-0.045em] text-fg">
          <span className="block">Push your code.</span>
          <span className="block text-accent-soft">Get a URL.</span>
        </h1>

        <p className="rise mx-auto mt-8 max-w-[36rem] text-[18px] leading-relaxed text-muted [animation-delay:180ms] sm:text-[19px]">
          Connect a Git repository and Harbor builds it, runs it, and gives it a
          live address. No servers to configure, nothing to babysit.
        </p>

        <div className="rise mt-10 flex flex-col items-center justify-center gap-3 [animation-delay:300ms] sm:flex-row">
          <Button href="#signup" size="lg" className="w-full sm:w-auto">
            Create an account
          </Button>
          <Button
            href="#how-it-works"
            variant="secondary"
            size="lg"
            className="w-full sm:w-auto"
          >
            See how it works
          </Button>
        </div>
      </div>
    </section>
  );
}
