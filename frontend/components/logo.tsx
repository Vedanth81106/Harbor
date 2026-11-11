import Link from "next/link";

export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link
      href="/"
      className={`inline-flex items-center gap-2.5 ${className}`}
      aria-label="Harbor home"
    >
      {/* swapped by theme in globals.css, so only the needed file is fetched */}
      <span aria-hidden="true" className="logo-mark" />
      <span className="font-display text-[21px] font-semibold tracking-[-0.02em] text-fg">
        Harbor
      </span>
    </Link>
  );
}
