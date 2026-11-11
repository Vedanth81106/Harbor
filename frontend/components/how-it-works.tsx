const steps = [
  {
    number: "01",
    title: "Connect your repository",
    body: "Paste a Git URL. Harbor picks up the latest commit on its own, no setup on your side.",
  },
  {
    number: "02",
    title: "Press deploy",
    body: "It builds and starts your app. You can watch the whole thing happen, live.",
  },
  {
    number: "03",
    title: "Open your URL",
    body: "Every deploy gets its own address, live the moment it's ready.",
  },
];

export function HowItWorks() {
  return (
    <section
      id="how-it-works"
      className="relative scroll-mt-24 bg-panel text-fg"
    >
      <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-24 sm:pb-32 sm:pt-32">
        <p className="reveal font-mono text-[11px] uppercase tracking-[0.18em] text-secondary">
          How it works
        </p>

        <div className="reveal mt-6 flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <h2 className="max-w-[19ch] text-balance font-serif text-[clamp(2.6rem,5.4vw,4.2rem)] leading-[1.02] tracking-[-0.015em] text-fg">
            From repository to live address,{" "}
            <em className="italic text-accent-soft">in three steps.</em>
          </h2>
          <p className="max-w-sm text-pretty text-[16.5px] leading-relaxed text-muted lg:pb-2">
            Nothing to configure. Point Harbor at your code, press deploy, and
            open the link it hands back.
          </p>
        </div>

        <ol className="mt-16 grid gap-10 sm:mt-20 sm:grid-cols-3 sm:gap-8">
          {steps.map((step, i) => (
            <li
              key={step.number}
              className="reveal border-t border-line-strong pt-6"
              style={{ transitionDelay: `${i * 90}ms` }}
            >
              <span className="font-mono text-[11px] tracking-[0.18em] text-secondary">
                {step.number}
              </span>
              <h3 className="mt-4 font-display text-[19px] font-medium tracking-[-0.01em] text-fg">
                {step.title}
              </h3>
              <p className="mt-2 text-[15px] leading-relaxed text-muted">
                {step.body}
              </p>
            </li>
          ))}
        </ol>

        <a
          href="#docs"
          className="reveal mt-16 inline-flex items-center gap-2 border-b border-secondary-border pb-1 font-mono text-[11px] uppercase tracking-[0.18em] text-secondary transition-colors hover:border-secondary hover:text-fg"
        >
          Read the docs
          <span aria-hidden="true">→</span>
        </a>
      </div>
    </section>
  );
}
