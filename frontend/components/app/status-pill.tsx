import type { DeploymentStatus } from "@/lib/api";

const styles: Record<DeploymentStatus, string> = {
  QUEUED: "border-line bg-panel-2 text-muted",
  CLONING: "border-secondary-border bg-secondary-wash text-secondary",
  BUILDING: "border-secondary-border bg-secondary-wash text-secondary",
  STARTING: "border-secondary-border bg-secondary-wash text-secondary",
  READY: "border-secondary-border bg-secondary-wash text-secondary",
  FAILED: "border-accent-soft/40 bg-accent/10 text-accent-soft",
  STOPPED: "border-line bg-panel-2 text-muted",
};

const labels: Record<DeploymentStatus, string> = {
  QUEUED: "Queued",
  CLONING: "Cloning",
  BUILDING: "Building",
  STARTING: "Starting",
  READY: "Live",
  FAILED: "Failed",
  STOPPED: "Stopped",
};

export function StatusPill({ status }: { status: DeploymentStatus }) {
  const active =
    status === "CLONING" || status === "BUILDING" || status === "STARTING";

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full border px-2.5 py-1 font-mono text-[9.5px] uppercase tracking-[0.16em] ${styles[status]}`}
    >
      <span aria-hidden="true" className="relative grid h-1.5 w-1.5 place-items-center">
        <span
          className={`h-1.5 w-1.5 rounded-full bg-current ${active ? "animate-pulse" : ""}`}
        />
      </span>
      {labels[status]}
    </span>
  );
}
