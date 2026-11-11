import { Logo } from "@/components/logo";

const productLinks = [
  { label: "Overview", href: "#product" },
  { label: "How it works", href: "#how-it-works" },
  {
    label: "Docs",
    href: "https://github.com/Vedanth81106/Harbor",
    external: true,
  },
];

const contactLinks = [
  { label: "GitHub", href: "https://github.com/Vedanth81106" },
  {
    label: "LinkedIn",
    href: "https://www.linkedin.com/in/vedanth-m-990356363/",
  },
  { label: "Instagram", href: "https://instagram.com/vedanth8116" },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-line bg-panel shadow-[var(--footer-lift)]">
      <div className="mx-auto w-full max-w-6xl px-6 pb-10 pt-16 sm:pt-20">
        <div className="grid gap-12 sm:grid-cols-2 lg:grid-cols-[1.5fr_1fr_1fr]">
          <div>
            <Logo />
            <p className="mt-5 max-w-[22ch] text-[15px] leading-relaxed text-muted">
              Push your code. Get a URL. A quiet place to run what you build.
            </p>
          </div>

          <nav aria-label="Product">
            <h2 className="text-[11px] uppercase tracking-[0.18em] text-muted">
              Product
            </h2>
            <ul className="mt-4 space-y-2.5">
              {productLinks.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    className="text-[14.5px] text-muted transition-colors hover:text-fg"
                    {...(link.external
                      ? { target: "_blank", rel: "noreferrer" }
                      : {})}
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <nav aria-label="Contact">
            <h2 className="text-[11px] uppercase tracking-[0.18em] text-muted">
              Contact
            </h2>
            <ul className="mt-4 space-y-2.5">
              {contactLinks.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    target="_blank"
                    rel="noreferrer"
                    className="text-[14.5px] text-muted transition-colors hover:text-fg"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="mt-14 flex flex-col gap-4 border-t border-line pt-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-[11px] uppercase tracking-[0.16em] text-muted">
            © {new Date().getFullYear()} Harbor
          </p>

          <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
            <a
              href="/privacy"
              className="text-[13.5px] text-muted transition-colors hover:text-fg"
            >
              Privacy
            </a>
            <a
              href="/terms"
              className="text-[13.5px] text-muted transition-colors hover:text-fg"
            >
              Terms
            </a>
            <a
              href="#top"
              className="group inline-flex items-center gap-1.5 text-[11px] uppercase tracking-[0.16em] text-muted transition-colors hover:text-fg"
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
