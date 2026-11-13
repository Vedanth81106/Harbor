"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import {
  ApiError,
  login,
  register,
  type AuthResult,
  type AuthUser,
} from "@/lib/api";

type AuthMode = "login" | "signup" | null;

type AuthContextValue = {
  open: AuthMode;
  openLogin: (nextPath?: string) => void;
  openSignup: () => void;
  close: () => void;
  ready: boolean;
  user: AuthUser | null;
  accessToken: string | null;
  setSession: (session: AuthResult) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const SESSION_KEY = "harbor.auth.session";

const inputClass =
  "mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary disabled:cursor-not-allowed disabled:opacity-60";

function readStoredSession(): AuthResult | null {
  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    if (!raw) return null;

    const parsed = JSON.parse(raw) as Partial<AuthResult>;
    if (
      typeof parsed.accessToken !== "string" ||
      typeof parsed.username !== "string" ||
      typeof parsed.email !== "string"
    ) {
      return null;
    }

    return {
      accessToken: parsed.accessToken,
      username: parsed.username,
      email: parsed.email,
    };
  } catch {
    return null;
  }
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

function AuthDialog({
  mode,
  onClose,
}: {
  mode: Exclude<AuthMode, null>;
  onClose: () => void;
}) {
  const { setSession } = useAuth();
  const isLogin = mode === "login";

  const [identifier, setIdentifier] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) return;

    setError(null);
    setPending(true);

    try {
      const result = isLogin
        ? await login({ identifier: identifier.trim(), password })
        : await register({
            username: username.trim(),
            email: email.trim(),
            password,
          });

      setSession(result);
      onClose();
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Something went wrong. Please try again.",
      );
    } finally {
      setPending(false);
    }
  }

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

          <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
            {!isLogin && (
              <div>
                <label htmlFor="auth-username" className="block text-[13px] font-medium text-fg">
                  Username
                </label>
                <input
                  id="auth-username"
                  name="username"
                  type="text"
                  autoComplete="username"
                  placeholder="your-handle"
                  minLength={3}
                  maxLength={100}
                  required
                  disabled={pending}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className={inputClass}
                />
                <p className="mt-1.5 text-[12px] text-muted">At least 3 characters.</p>
              </div>
            )}

            {isLogin ? (
              <div>
                <label htmlFor="auth-identifier" className="block text-[13px] font-medium text-fg">
                  Email or username
                </label>
                <input
                  id="auth-identifier"
                  name="identifier"
                  type="text"
                  autoComplete="username"
                  placeholder="you@example.com"
                  required
                  disabled={pending}
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  className={inputClass}
                />
              </div>
            ) : (
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
                  maxLength={254}
                  required
                  disabled={pending}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={inputClass}
                />
              </div>
            )}

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
                placeholder={isLogin ? "••••••••" : "At least 8 characters"}
                minLength={isLogin ? undefined : 8}
                maxLength={100}
                required
                disabled={pending}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClass}
              />
            </div>

            {error ? (
              <p
                role="alert"
                aria-live="polite"
                className="rounded-xl border border-accent-soft/40 bg-accent/10 px-4 py-3 text-[13.5px] leading-relaxed text-accent-soft"
              >
                {error}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={pending}
              className="mt-2 inline-flex w-full items-center justify-center rounded-full bg-accent px-7 py-3.5 text-[15.5px] font-medium text-on-accent shadow-[inset_0_1px_0_rgba(255,255,255,0.12)] transition-colors duration-200 hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-70"
            >
              {pending
                ? isLogin
                  ? "Logging in…"
                  : "Creating account…"
                : isLogin
                  ? "Log in"
                  : "Create account"}
            </button>
          </form>

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
      onClick={target === "login" ? () => openLogin() : openSignup}
      className="font-medium text-secondary transition-colors hover:text-fg"
    >
      {children}
    </button>
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [open, setOpen] = useState<AuthMode>(null);
  const [ready, setReady] = useState(false);
  const [session, setSessionState] = useState<AuthResult | null>(null);
  const pendingRedirect = useRef<string | null>(null);

  // Read the persisted session after mount so server and client markup match.
  // The URL can also ask for the dialog to open and carry a post-login target.
  useEffect(() => {
    setSessionState(readStoredSession());

    const params = new URLSearchParams(window.location.search);
    const auth = params.get("auth");

    if (auth === "login" || auth === "signup") {
      setOpen(auth);
    }

    const next = params.get("next");
    if (next && next.startsWith("/") && !next.startsWith("//")) {
      pendingRedirect.current = next;
    }

    if (params.has("auth") || params.has("next")) {
      params.delete("auth");
      params.delete("next");
      const query = params.toString();
      window.history.replaceState(
        null,
        "",
        `${window.location.pathname}${query ? `?${query}` : ""}${window.location.hash}`,
      );
    }

    setReady(true);
  }, []);

  // Keep signed-in tabs in sync when one of them logs in or out.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === SESSION_KEY) {
        setSessionState(readStoredSession());
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const openLogin = useCallback((nextPath?: string) => {
    if (nextPath && nextPath.startsWith("/") && !nextPath.startsWith("//")) {
      pendingRedirect.current = nextPath;
    }
    setOpen("login");
  }, []);
  const openSignup = useCallback(() => setOpen("signup"), []);
  const close = useCallback(() => {
    pendingRedirect.current = null;
    setOpen(null);
  }, []);

  const setSession = useCallback(
    (next: AuthResult) => {
      setSessionState(next);
      try {
        window.localStorage.setItem(SESSION_KEY, JSON.stringify(next));
      } catch {
        // A blocked localStorage should not stop the in-memory session.
      }

      const target = pendingRedirect.current;
      pendingRedirect.current = null;
      if (target) {
        router.replace(target);
      }
    },
    [router],
  );

  const logout = useCallback(() => {
    setSessionState(null);
    try {
      window.localStorage.removeItem(SESSION_KEY);
    } catch {
      // Nothing else to clean up when storage is unavailable.
    }
  }, []);

  const value = useMemo(
    () => ({
      open,
      openLogin,
      openSignup,
      close,
      ready,
      user: session ? { username: session.username, email: session.email } : null,
      accessToken: session?.accessToken ?? null,
      setSession,
      logout,
    }),
    [open, openLogin, openSignup, close, ready, session, setSession, logout],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {open ? <AuthDialog key={open} mode={open} onClose={close} /> : null}
    </AuthContext.Provider>
  );
}
