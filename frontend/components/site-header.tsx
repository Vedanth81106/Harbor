import Link from "next/link";
import { Logo } from "@/components/logo";
import { Button } from "@/components/ui/button";

const links = [
  { label: "Product", href: "#product" },
  { label: "How it works", href: "#how-it-works" },
  { label: "Docs", href: "#docs" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-teal-lift/15 bg-ink/70 backdrop-blur-md">
      <div className="mx-auto flex h-[72px] w-full max-w-6xl items-center gap-8 px-6">
        <Logo />

        <nav className="ml-auto hidden items-center gap-7 md:flex">
          {links.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="text-[13.5px] text-mist/70 transition-colors hover:text-teal-lift"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2 md:ml-0">
          <Link
            href="#login"
            className="hidden px-3 py-2 text-[13.5px] text-mist/70 transition-colors hover:text-teal-lift sm:block"
          >
            Log in
          </Link>
          <Button href="#signup">Sign up</Button>
        </div>
      </div>
    </header>
  );
}
