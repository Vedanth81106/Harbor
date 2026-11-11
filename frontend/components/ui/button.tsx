import Link from "next/link";

type Variant = "primary" | "secondary";
type Size = "md" | "lg";

const base =
  "inline-flex items-center justify-center gap-2 rounded-full font-medium transition-colors duration-200";

const variants: Record<Variant, string> = {
  primary:
    "bg-crimson text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.12)] hover:bg-crimson-deep",
  secondary:
    "border border-teal-lift/45 bg-teal/[0.10] text-teal-lift hover:border-teal-lift hover:bg-teal/25 hover:text-white",
};

const sizes: Record<Size, string> = {
  md: "px-5 py-2.5 text-sm",
  lg: "px-7 py-3.5 text-[15px]",
};

export function Button({
  href,
  variant = "primary",
  size = "md",
  className = "",
  children,
}: {
  href: string;
  variant?: Variant;
  size?: Size;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
    >
      {children}
    </Link>
  );
}
