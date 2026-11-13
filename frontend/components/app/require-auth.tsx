"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/auth-provider";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, ready, openLogin } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!ready || user) return;
    openLogin(pathname);
    router.replace("/");
  }, [ready, user, pathname, router, openLogin]);

  if (!ready || !user) {
    return (
      <div className="grid min-h-svh place-items-center">
        <div
          role="status"
          aria-label="Checking your session"
          className="h-8 w-8 animate-spin rounded-full border-2 border-line border-t-secondary"
        />
      </div>
    );
  }

  return <>{children}</>;
}
