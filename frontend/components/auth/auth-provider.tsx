"use client";

import Link from "next/link";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

type AuthMode = "login" | "signup" | null;

type AuthContextValue = {
  open: AuthMode;
  openLogin: () => void;
  openSignup: () => void;
  close: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

function AuthDialog({ mode, onClose }: { mode: Exclude<AuthMode, null>; onClose: () => void }) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeyDown);
    return () => {
      document.body.style.overflow = prev;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [onClose]);

  const isLogin = mode === "login";

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center overflow-y-auto bg-bg/70 p-4 backdrop-blur-sm sm:p-6"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-dialog-title"
        className="relative w-full max-w-md"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="w-full rounded-2xl border border-panel-line bg-panel p-8 shadow-[var(--panel-shadow)] sm:p-10">
          <button
            type="button"
            onClick={onClose}
            className="absolute right-4 top-4 inline-flex h-9 w-9 items-center justify-center rounded-full border border-line bg-panel-2 text-muted transition-colors hover:text-fg"
            aria-label="Close"
          >
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </button>

          <p className="text-[11px] uppercase tracking-[0.18em] text-secondary">
            {isLogin ? "Welcome back" : "Get started"}
          </p>
          <h1
            id="auth-dialog-title"
            className="mt-3 pr-8 text-[clamp(1.9rem,4vw,2.4rem)] font-medium leading-[1.05] tracking-[-0.02em] text-fg"
          >
            {isLogin ? "Log in to Harbor" : "Create your account"}
          </h1>
          <p className="mt-3 text-[15px] leading-relaxed text-muted">
            {isLogin
              ? "Pick up where you left off and ship your next deploy."
              : "Connect a repository and get a live URL in minutes."}
          </p>

          <form className="mt-8 space-y-5" action="#" method="post" onSubmit={(e) => e.preventDefault()}>
            {!isLogin && (
              <div>
                <label htmlFor="auth-name" className="block text-[13px] font-medium text-fg">
                  Name
                </label>
                <input
                  id="auth-name"
                  name="name"
                  type="text"
                  autoComplete="name"
                  placeholder="Your name"
                  className="mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary"
                />
              </div>
            )}

            <div>
              <label htmlFor="auth-email" className="block text-[13px] font-medium text-fg">
                Email
              </label>
              <input
                id="auth-email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                className="mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary"
              />
            </div>

            <div>
              <div className="flex items-center justify-between gap-4">
                <label htmlFor="auth-password" className="block text-[13px] font-medium text-fg">
                  Password
                </label>
                {isLogin && (
                  <span className="text-[13px] text-muted">Forgot password?</span>
                )}
              </div>
              <input
                id="auth-password"
                name="password"
                type="password"
                autoComplete={isLogin ? "current-password" : "new-password"}
                placeholder={isLogin ? "••••••••" : "Create a password"}
                className="mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary"
              />
            </div>

            <button
              type="submit"
              className="mt-2 inline-flex w-full items-center justify-center rounded-full bg-accent px-7 py-3.5 text-[15.5px] font-medium text-on-accent shadow-[inset_0_1px_0_rgba(255,255,255,0.12)] transition-colors duration-200 hover:bg-accent-hover"
            >
              {isLogin ? "Log in" : "Create account"}
            </button>
          </form>

          <p className="mt-6 text-center text-[14px] text-muted">
            {isLogin ? (
              <>
                Don&rsquo;t have an account?{" "}
                <button
                  type="button"
                  onClick={() => {
                    // switch handled by parent via state setter exposed? easier: use links to self state
                  }}
                  className="hidden"
                />
              </>
            ) : null}
          </p>
          {isLogin ? (
            <p className="mt-6 text-center text-[14px] text-muted">
              Don&rsquo;t have an account?{" "}
              <AuthSwitch target="signup">Sign up</AuthSwitch>
            </p>
          ) : (
            <p className="mt-6 text-center text-[14px] text-muted">
              Already have an account?{" "}
              <AuthSwitch target="login">Log in</AuthSwitch>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

function AuthSwitch({ target, children }: { target: Exclude<AuthMode, null>; children: ReactNode }) {
  const { openLogin, openSignup } = useAuth();
  return (
    <button
      type="button"
      onClick={target === "login" ? openLogin : openSignup}
      className="font-medium text-secondary transition-colors hover:text-fg"
    >
      {children}
    </button>
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState<AuthMode>(null);

  const openLogin = useCallback(() => setOpen("login"), []);
  const openSignup = useCallback(() => setOpen("signup"), []);
  const close = useCallback(() => setOpen(null), []);

  const value = useMemo(
    () => ({ open, openLogin, openSignup, close }),
    [open, openLogin, openSignup, close],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {open ? <AuthDialog mode={open} onClose={close} /> : null}
    </AuthContext.Provider>
  );
}
