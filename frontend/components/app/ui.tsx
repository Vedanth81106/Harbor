import type { ReactNode } from "react";

export const cardClass =
  "rounded-2xl border border-panel-line bg-panel shadow-[var(--panel-shadow)]";

export const inputClass =
  "mt-2 w-full rounded-xl border border-line bg-panel-2 px-4 py-3 text-[15px] text-fg placeholder:text-muted/70 outline-none transition-colors focus:border-secondary disabled:cursor-not-allowed disabled:opacity-60";

export const labelClass = "block text-[13px] font-medium text-fg";

export function ErrorBanner({
  message,
  className = "",
}: {
  message: string;
  className?: string;
}) {
  return (
    <p
      role="alert"
      className={`rounded-xl border border-accent-soft/40 bg-accent/10 px-4 py-3 text-[13.5px] leading-relaxed text-accent-soft ${className}`}
    >
      {message}
    </p>
  );
}

export function PageLoading({ label = "Loading" }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-20 text-[14px] text-muted">
      <span
        aria-hidden="true"
        className="h-5 w-5 animate-spin rounded-full border-2 border-line border-t-secondary"
      />
      <span>{label}…</span>
    </div>
  );
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow ? (
          <p className="text-[11px] uppercase tracking-[0.18em] text-secondary">
            {eyebrow}
          </p>
        ) : null}
        <h1 className="mt-2 text-[clamp(2rem,4vw,2.8rem)] font-medium leading-[1.05] tracking-[-0.02em] text-fg">
          {title}
        </h1>
        {description ? (
          <p className="mt-3 max-w-[42rem] text-[15px] leading-relaxed text-muted">
            {description}
          </p>
        ) : null}
      </div>
      {actions ? <div className="flex shrink-0 gap-3">{actions}</div> : null}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className={`${cardClass} px-8 py-16 text-center`}>
      <span
        aria-hidden="true"
        className="mx-auto grid h-12 w-12 place-items-center rounded-xl border border-secondary-border bg-secondary-wash text-secondary"
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M12 3v18M5 8h14M7 8l-3 6a3.5 3.5 0 0 0 6 0L7 8Zm10 0-3 6a3.5 3.5 0 0 0 6 0l-3-6Z" />
        </svg>
      </span>
      <h2 className="mt-5 text-[19px] font-medium text-fg">{title}</h2>
      <p className="mx-auto mt-2 max-w-[28rem] text-[14.5px] leading-relaxed text-muted">
        {description}
      </p>
      {action ? <div className="mt-7 flex justify-center">{action}</div> : null}
    </div>
  );
}
