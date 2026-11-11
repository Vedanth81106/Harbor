import type { Metadata } from "next";
import Link from "next/link";
import { HarborScene } from "@/components/harbor-scene";
import { Logo } from "@/components/logo";
import { AuthModal } from "@/components/auth-modal";

export const metadata: Metadata = {
  title: "Log in",
};

export default function LoginPage() {
  return (
    <div className="relative isolate min-h-svh overflow-hidden">
      <HarborScene />
      <div className="absolute inset-0 bg-bg/60 backdrop-blur-[2px]" aria-hidden="true" />

      <div className="relative z-10 flex min-h-svh flex-col px-4 py-6 sm:px-8">
        <div className="flex items-center justify-between">
          <Logo />
          <Link
            href="/"
            className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-line bg-panel/80 text-fg transition-colors hover:bg-panel-2"
            aria-label="Close"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-center">
          <AuthModal labelledBy="login-title">
            <div className="w-full rounded-2xl border border-panel-line bg-panel p-8 shadow-[var(--panel-shadow)] sm:p-10">
              <p className="text-[11px] uppercase tracking-[0.18em] text-secondary">
                Welcome back
              </p>
              <h1
                id="login-title"
                className="mt-3 text-[clamp(1.9rem,4vw,2.4rem)] font-medium leading-[1.05] tracking-[-0.02em] text-fg"
              >
                Log in to Harbor
              </h1>
              <p className="mt-3 text-[15px] leading-relaxed text-muted">
                Pick up where you left off and ship your next deploy.
              </p>

              <form className="mt-8 space-y-5" action="#" method="post">
                <div>
                  <label htmlFor="email" className="block text-[13px] font-medium text-fg">
                    Email
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@example.com"
                    className="mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between gap-4">
                    <label htmlFor="password" className="block text-[13px] font-medium text-fg">
                      Password
                    </label>
                    <a href="#" className="text-[13px] text-secondary transition-colors hover:text-fg">
                      Forgot password?
                    </a>
                  </div>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    className="mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary"
                  />
                </div>

                <button
                  type="submit"
                  className="mt-2 inline-flex w-full items-center justify-center rounded-full bg-accent px-7 py-3.5 text-[15.5px] font-medium text-on-accent shadow-[inset_0_1px_0_rgba(255,255,255,0.12)] transition-colors duration-200 hover:bg-accent-hover"
                >
                  Log in
                </button>
              </form>

              <p className="mt-6 text-center text-[14px] text-muted">
                Don&rsquo;t have an account?{" "}
                <Link href="/signup" className="font-medium text-secondary transition-colors hover:text-fg">
                  Sign up
                </Link>
              </p>
            </div>
          </AuthModal>
        </div>
      </div>
    </div>
  );
}
