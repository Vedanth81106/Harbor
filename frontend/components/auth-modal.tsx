"use client";

import { useRouter } from "next/navigation";
import { type ReactNode, useCallback, useEffect } from "react";

export function AuthModal({
  children,
  labelledBy,
}: {
  children: ReactNode;
  labelledBy: string;
}) {
  const router = useRouter();

  const close = useCallback(() => {
    router.push("/");
  }, [router]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") close();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [close]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto px-4 py-10 sm:px-6"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) close();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        className="relative w-full max-w-md"
      >
        {children}
      </div>
    </div>
  );
}
