"use client";

import Link from "next/link";
import { Logo } from "@/components/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/components/auth/auth-provider";

const links = [
  { label: "Product", href: "#product" },
  { label: "How it works", href: "#how-it-works" },
  { label: "Docs", href: "https://github.com/Vedanth81106/Harbor" },
];

export function SiteHeader() {
  const { openLogin, openSignup, user, logout } = useAuth();
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
              {...(link.href.startsWith("http")
                ? { target: "_blank", rel: "noreferrer" }
                : {})}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2 sm:gap-3">
          <ThemeToggle />
          {user ? (
            <>
              <Button
                href="/dashboard"
                variant="ghost"
                className="hidden sm:inline-flex"
              >
                Dashboard
              </Button>
              <Link
                href="/dashboard"
                className="text-[14px] text-muted transition-colors hover:text-fg sm:hidden"
              >
                Dashboard
              </Link>
              <span
                className="hidden max-w-[16ch] truncate text-[14px] text-muted md:inline"
                title={user.username}
              >
                {user.username}
              </span>
              <Button variant="ghost" onClick={logout}>
                Log out
              </Button>
            </>
          ) : (
            <>
              <Button variant="ghost" onClick={() => openLogin()}>
                Log in
              </Button>
              <Button onClick={openSignup}>Sign up</Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
