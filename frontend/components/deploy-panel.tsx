const logLines = [
  { label: "Cloning repository", time: "0.9s" },
  { label: "Building image", time: "18.4s" },
  { label: "Starting container", time: "1.2s" },
  { label: "Ready", time: "0.4s" },
];

/**
 * A still of the deployment screen. It is an illustration, so it is exposed
 * to assistive tech as one labelled image rather than a pile of loose text.
 */
export function DeployPanel() {
  return (
    <div
      role="img"
      aria-label="A Harbor deployment for the storefront project. The build finished and the app is live at storefront.harbor.app."
      className="relative"
    >
      {/* light spilled on the surface behind the card */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -inset-x-10 -inset-y-12 -z-10 rounded-[4rem] bg-[radial-gradient(closest-side,var(--panel-glow),transparent)] blur-2xl"
      />

      <div className="overflow-hidden rounded-2xl border border-panel-line bg-panel shadow-[var(--panel-shadow)] transition-colors duration-500">
        <div className="flex items-center justify-between gap-4 border-b border-panel-line px-5 py-4">
          <div className="flex min-w-0 items-center gap-3">
            <span
              aria-hidden="true"
              className="grid h-9 w-9 shrink-0 place-items-center rounded-lg border border-panel-line bg-panel-2 font-mono text-[13px] text-secondary"
            >
              s
            </span>
            <div className="min-w-0">
              <p className="truncate text-[14px] font-medium tracking-[-0.01em] text-fg">
                storefront
              </p>
              <p className="truncate font-mono text-[11px] tracking-[0.02em] text-muted">
                main · a91f3c2
              </p>
            </div>
          </div>

          <span className="inline-flex shrink-0 items-center gap-2 rounded-full border border-secondary-border bg-secondary-wash px-2.5 py-1 font-mono text-[9.5px] uppercase tracking-[0.16em] text-secondary">
            <span
              aria-hidden="true"
              className="live-dot relative grid h-1.5 w-1.5 place-items-center"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-current" />
            </span>
            Live
          </span>
        </div>

        {/* finished progress rail */}
        <div aria-hidden="true" className="h-px w-full bg-secondary/40" />

        <div className="space-y-0.5 px-5 py-4 font-mono text-[12px]">
          {logLines.map((line) => (
            <div key={line.label} className="flex items-center gap-3 py-1">
              <svg
                aria-hidden="true"
                width="12"
                height="12"
                viewBox="0 0 16 16"
                fill="none"
                className="shrink-0 text-secondary"
              >
                <path
                  d="M3.2 8.4 6.3 11.5 12.8 4.8"
                  stroke="currentColor"
                  strokeWidth="1.6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              <span className="truncate text-fg">{line.label}</span>
              <span className="ml-auto shrink-0 tabular-nums text-muted">
                {line.time}
              </span>
            </div>
          ))}
        </div>

        <div className="flex items-center gap-3 border-t border-panel-line bg-panel-2 px-4 py-3">
          <span className="min-w-0 flex-1 truncate font-mono text-[12px] text-fg">
            <span className="text-secondary">https://</span>
            storefront.harbor.app
          </span>
          <span className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-accent px-3 py-1.5 text-[11.5px] font-medium text-on-accent">
            Open
            <svg
              aria-hidden="true"
              width="11"
              height="11"
              viewBox="0 0 16 16"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M5 11 11 5M6 5h5v5" />
            </svg>
          </span>
        </div>
      </div>
    </div>
  );
}
