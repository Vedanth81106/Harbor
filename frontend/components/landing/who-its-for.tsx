const audiences = [
  {
    title: "Side projects",
    body: "Ship experiments and personal apps without spending a weekend on setup.",
  },
  {
    title: "Hackathons & demos",
    body: "Turn a repo into a shareable live URL fast, right when it matters.",
  },
  {
    title: "Small teams",
    body: "Keep a simple path from code to a running app everyone can open.",
  },
];

export function WhoItsFor() {
  return (
    <section className="relative scroll-mt-24 border-t border-line bg-bg">
      <div className="mx-auto w-full max-w-6xl px-6 pb-24 pt-24 sm:pb-32 sm:pt-32">
        <p className="reveal text-[11px] uppercase tracking-[0.18em] text-secondary">
          Who it&rsquo;s for
        </p>

        <div className="reveal mt-6 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <h2 className="max-w-[18ch] text-balance text-[clamp(2.4rem,5vw,3.8rem)] leading-[1.02] tracking-[-0.02em] text-fg">
            Made for people who want to <span className="text-accent-soft">ship</span>
          </h2>
          <p className="max-w-sm text-pretty text-[16.5px] leading-relaxed text-muted lg:pb-2">
            A straightforward way to get from a repository to something people can actually use.
          </p>
        </div>

        <div className="mt-14 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {audiences.map((item, i) => (
            <article
              key={item.title}
              className="reveal rounded-2xl border border-panel-line bg-panel p-6 shadow-[var(--panel-shadow)] sm:p-7"
              style={{ transitionDelay: `${i * 90}ms` }}
            >
              <h3 className="text-[18px] font-medium tracking-[-0.01em] text-fg">
                {item.title}
              </h3>
              <p className="mt-3 text-[15px] leading-relaxed text-muted">
                {item.body}
              </p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
