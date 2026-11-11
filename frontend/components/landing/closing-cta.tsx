import { Button } from "@/components/ui/button";

export function ClosingCta() {
  return (
    <section className="relative isolate overflow-hidden border-t border-line bg-bg">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 -z-10"
      >
        <div className="absolute left-1/2 top-1/2 h-[26rem] w-[52rem] max-w-[130vw] -translate-x-1/2 -translate-y-1/2 rounded-full bg-[radial-gradient(closest-side,var(--cta-glow),transparent)] blur-2xl" />
      </div>

      <div className="mx-auto w-full max-w-3xl px-6 pb-28 pt-28 text-center sm:pb-36 sm:pt-36">
        <p className="reveal font-mono text-[11px] uppercase tracking-[0.18em] text-secondary">
          Start shipping
        </p>

        <h2 className="reveal mt-6 text-balance font-serif text-[clamp(2.8rem,6.4vw,4.8rem)] leading-[0.98] tracking-[-0.02em] text-fg">
          Your next deploy is{" "}
          <span className="text-accent-soft">a click away.</span>
        </h2>

        <p className="reveal mx-auto mt-7 max-w-[34rem] text-pretty text-[17px] leading-relaxed text-muted">
          Point Harbor at a repository. It builds, runs, and hands you a live
          address.
        </p>

        <div className="reveal mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
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

        <p className="reveal mt-12 font-mono text-[10.5px] uppercase tracking-[0.2em] text-muted/80">
          No servers · No pipelines · No YAML
        </p>
      </div>
    </section>
  );
}
