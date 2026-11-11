import { Button } from "@/components/ui/button";

export function Hero() {
  return (
    <section className="relative isolate flex min-h-[calc(100svh-72px)] items-center overflow-hidden">
      {/* cool light from above, warm light from below */}
      <div aria-hidden className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute left-1/2 top-[-24rem] h-[52rem] w-[64rem] -translate-x-1/2 rounded-full bg-[radial-gradient(closest-side,rgba(18,151,181,0.28),transparent)] blur-2xl" />
        <div className="absolute bottom-[-10rem] left-1/2 h-[32rem] w-[64rem] -translate-x-1/2 rounded-full bg-[radial-gradient(closest-side,rgba(128,11,31,0.34),transparent)] blur-3xl" />
        <div className="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-b from-transparent to-ink" />
      </div>

      <div className="mx-auto w-full max-w-4xl px-6 pb-32 pt-20 text-center sm:pt-24">
        <h1 className="rise font-display text-[clamp(3.25rem,8.4vw,6.5rem)] font-medium leading-[0.92] tracking-[-0.045em] text-white">
          <span className="block">Push your code.</span>
          <span className="block text-crimson-lift">Get a URL.</span>
        </h1>

        <p className="rise mx-auto mt-8 max-w-[36rem] text-[18px] leading-relaxed text-mist/80 [animation-delay:180ms] sm:text-[19px]">
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
