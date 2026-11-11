import { DeployPanel } from "@/components/deploy-panel";

const points = [
  {
    title: "A URL for every deploy",
    body: "Each deployment gets its own address, live the moment it's ready.",
  },
  {
    title: "Watch it happen",
    body: "Output streams into the page as it runs, so nothing is a black box.",
  },
  {
    title: "Nothing to configure",
    body: "No servers, no pipelines, no files to babysit before the first deploy.",
  },
];

export function Product() {
  return (
    <section id="product" className="relative scroll-mt-24 overflow-hidden bg-bg">
      <div className="mx-auto grid w-full max-w-6xl items-center gap-14 px-6 pb-24 pt-24 sm:pb-32 sm:pt-32 lg:grid-cols-[0.92fr_1.08fr] lg:items-start lg:gap-20">
        <div>
          <p className="reveal font-mono text-[11px] uppercase tracking-[0.18em] text-secondary">
            The product
          </p>

          <h2 className="reveal mt-6 max-w-[15ch] text-balance font-serif text-[clamp(2.6rem,5vw,3.9rem)] leading-[1.02] tracking-[-0.015em] text-fg">
            Everything between your code and{" "}
            <em className="italic text-accent-soft">your users.</em>
          </h2>

          <p className="reveal mt-6 max-w-[38ch] text-pretty text-[16.5px] leading-relaxed text-muted">
            Point Harbor at a repository and it takes over from there: the
            build, the run, and the address people visit.
          </p>

          <ul className="reveal mt-10 space-y-6">
            {points.map((point) => (
              <li key={point.title} className="flex gap-4">
                <span
                  aria-hidden="true"
                  className="mt-[0.7rem] h-px w-6 shrink-0 bg-accent-soft"
                />
                <div>
                  <h3 className="font-display text-[16.5px] font-medium tracking-[-0.01em] text-fg">
                    {point.title}
                  </h3>
                  <p className="mt-1 max-w-[36ch] text-[15px] leading-relaxed text-muted">
                    {point.body}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="reveal">
          <DeployPanel />
        </div>
      </div>
    </section>
  );
}
