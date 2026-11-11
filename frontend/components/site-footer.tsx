import { Logo } from "@/components/logo";

const columns = [
  {
    title: "Product",
    links: [
      { label: "Overview", href: "#product" },
      { label: "How it works", href: "#how-it-works" },
      { label: "Docs", href: "#docs" },
    ],
  },
  {
    title: "Resources",
    links: [
      { label: "Deploy logs", href: "#" },
      { label: "Status", href: "#" },
      { label: "Changelog", href: "#" },
    ],
  },
  {
    title: "Company",
    links: [
      { label: "About", href: "#" },
      { label: "Contact", href: "#" },
      { label: "Privacy", href: "#" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-line bg-panel shadow-[var(--footer-lift)]">
      <div className="mx-auto w-full max-w-6xl px-6 pb-10 pt-16 sm:pt-20">
        <div className="grid gap-12 sm:grid-cols-2 lg:grid-cols-[1.5fr_repeat(3,1fr)]">
          <div>
            <Logo />
            <p className="mt-5 max-w-[22ch] text-[15px] leading-relaxed text-muted">
              Push your code. Get a URL. A quiet place to run what you build.
            </p>
          </div>

          {columns.map((column) => (
            <nav key={column.title} aria-label={column.title}>
              <h2 className="font-mono text-[11px] uppercase tracking-[0.18em] text-muted">
                {column.title}
              </h2>
              <ul className="mt-4 space-y-2.5">
                {column.links.map((link) => (
                  <li key={link.label}>
                    <a
                      href={link.href}
                      className="text-[14.5px] text-muted transition-colors hover:text-fg"
                    >
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-4 border-t border-line pt-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-mono text-[11px] uppercase tracking-[0.16em] text-muted">
            © {new Date().getFullYear()} Harbor
          </p>

          <div className="flex items-center gap-6">
            <p className="text-[14px] text-muted">
              Built for people who&rsquo;d rather ship.
            </p>
            <a
              href="#top"
              className="group inline-flex items-center gap-1.5 font-mono text-[11px] uppercase tracking-[0.16em] text-muted transition-colors hover:text-fg"
            >
              Back to top
              <span
                aria-hidden="true"
                className="transition-transform duration-200 group-hover:-translate-y-0.5"
              >
                ↑
              </span>
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
