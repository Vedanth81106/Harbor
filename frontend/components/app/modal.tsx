"use client";

import { useEffect, useId, type ReactNode } from "react";

export function Modal({
  title,
  description,
  onClose,
  children,
}: {
  title: string;
  description?: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const titleId = useId();

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeyDown);
    return () => {
      document.body.style.overflow = previous;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[90] flex items-center justify-center overflow-y-auto bg-bg/70 p-4 backdrop-blur-sm sm:p-6"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative w-full max-w-md"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="w-full rounded-2xl border border-panel-line bg-panel p-8 shadow-[var(--panel-shadow)]">
          <button
            type="button"
            onClick={onClose}
            className="absolute right-4 top-4 inline-flex h-9 w-9 items-center justify-center rounded-full border border-line bg-panel-2 text-muted transition-colors hover:text-fg"
            aria-label="Close"
          >
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path
                d="M4 4l8 8M12 4l-8 8"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
            </svg>
          </button>

          <h2
            id={titleId}
            className="pr-8 text-[clamp(1.5rem,4vw,1.9rem)] font-medium leading-[1.1] tracking-[-0.02em] text-fg"
          >
            {title}
          </h2>
          {description ? (
            <p className="mt-2 text-[14.5px] leading-relaxed text-muted">
              {description}
            </p>
          ) : null}

          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
