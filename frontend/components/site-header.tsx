import Link from "next/link";
import { Logo } from "@/components/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";

const links = [
  { label: "Product", href: "#product" },
  { label: "How it works", href: "#how-it-works" },
  { label: "Docs", href: "#docs" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-line bg-veil backdrop-blur-md">
      <div className="relative flex h-[80px] w-full items-center justify-between gap-2 px-4 sm:px-8">
        <div className="flex items-center">
          <Logo />
        </div>

        {/* absolutely centred so it stays on the viewport axis whatever flanks it */}
        <nav className="absolute left-1/2 hidden -translate-x-1/2 items-center gap-9 lg:flex">
          {links.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="text-[15px] text-muted transition-colors hover:text-secondary"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-1.5 sm:gap-3">
          <ThemeToggle />
          <Link
            href="#login"
            className="whitespace-nowrap px-1 py-2 text-[15px] text-muted transition-colors hover:text-secondary sm:px-2"
          >
            Log in
          </Link>
          <Button href="#signup">Sign up</Button>
        </div>
      </div>
    </header>
  );
}
