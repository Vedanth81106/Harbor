"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "@/components/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/components/auth/auth-provider";

export function AppHeader() {
  const { user, logout, openLogin } = useAuth();
  const pathname = usePathname();
  const inConsole =
    pathname === "/dashboard" ||
    pathname.startsWith("/projects") ||
    pathname.startsWith("/deployments");

  return (
    <header className="sticky top-0 z-50 border-b border-line bg-veil backdrop-blur-md">
      <div className="flex h-[72px] w-full items-center justify-between gap-3 px-4 sm:px-8">
        <div className="flex items-center gap-7">
          <Logo />
          {user ? (
            <nav className="hidden items-center gap-6 sm:flex">
              <Link
                href="/dashboard"
                aria-current={inConsole ? "page" : undefined}
                className={
                  inConsole
                    ? "text-[14.5px] font-medium text-fg"
                    : "text-[14.5px] text-muted transition-colors hover:text-fg"
                }
              >
                Dashboard
              </Link>
            </nav>
          ) : null}
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          {user ? (
            <Link
              href="/dashboard"
              className="text-[14px] text-muted transition-colors hover:text-fg sm:hidden"
            >
              Dashboard
            </Link>
          ) : null}
          <ThemeToggle />
          {user ? (
            <>
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
            <Button variant="ghost" onClick={() => openLogin()}>
              Log in
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
