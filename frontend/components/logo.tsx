import Image from "next/image";
import Link from "next/link";

export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link
      href="/"
      className={`inline-flex items-center gap-2.5 ${className}`}
      aria-label="Harbor home"
    >
      <Image
        src="/harbor-logo.svg"
        alt=""
        width={38}
        height={44}
        priority
        unoptimized
        className="h-[38px] w-auto"
      />
      <span className="font-display text-[19px] font-semibold tracking-[-0.02em] text-white">
        Harbor
      </span>
    </Link>
  );
}
