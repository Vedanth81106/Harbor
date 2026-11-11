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
      <div className="grid h-[80px] w-full grid-cols-[1fr_auto_1fr] items-center gap-2 px-5 sm:px-8">
        <div className="justify-self-start">
          <Logo />
        </div>

        <nav className="hidden items-center gap-9 justify-self-center md:flex">
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

        <div className="flex items-center gap-2 justify-self-end sm:gap-3">
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
