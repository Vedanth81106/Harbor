"use client";

import { useEffect, useRef } from "react";

/**
 * The anchor is a function of scroll position, not a one-way animation:
 * scrolling down pays out the chain, scrolling up reels it back in.
 */
export function DriftingAnchor() {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    let frame = 0;

    const update = () => {
      frame = 0;
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const p = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;

      const travel = Math.max(0, window.innerHeight - 180);

      el.style.transform = `translate3d(0, ${p * travel}px, 0)`;
      el.style.opacity = String(Math.min(0.55, p * 5));
    };

    const onScroll = () => {
      if (!frame) frame = requestAnimationFrame(update);
    };

    update();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);

    return () => {
      if (frame) cancelAnimationFrame(frame);
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
    };
  }, []);

  return (
    <div
      ref={ref}
      aria-hidden="true"
      className="pointer-events-none fixed right-6 top-24 z-40 hidden opacity-0 xl:block"
    >
      <div className="drift-bob relative">
        <span className="absolute bottom-full left-1/2 h-24 w-px -translate-x-1/2 bg-[linear-gradient(to_top,var(--muted),transparent)]" />
        <svg
          width="30"
          height="36"
          viewBox="0 0 32 38"
          fill="none"
          stroke="var(--muted)"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="16" cy="6" r="3" />
          <path d="M16 9v22" />
          <path d="M10 13h12" />
          <path d="M8 20a8 8 0 0 0 16 0" />
        </svg>
      </div>
    </div>
  );
}
