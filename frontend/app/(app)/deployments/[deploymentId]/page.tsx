"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/auth-provider";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/app/modal";
import { StatusPill } from "@/components/app/status-pill";
import { useApiError } from "@/components/app/use-api-error";
import {
  ErrorBanner,
  PageHeader,
  PageLoading,
  cardClass,
} from "@/components/app/ui";
import {
  ApiError,
  deleteDeployment,
  deploymentUrl,
  executeDeployment,
  getDeployment,
  getProject,
  isAbortError,
  isDeploymentActive,
  streamDeploymentLogs,
  type Deployment,
  type DeploymentStatus,
  type Project,
} from "@/lib/api";

const statusCopy: Record<DeploymentStatus, string> = {
  QUEUED: "Queued. Run it to start the clone → build → start pipeline.",
  CLONING: "Cloning the repository into the deployment workspace.",
  BUILDING: "Building the Docker image from the repository's Dockerfile.",
  STARTING: "Starting the container and connecting it to Traefik.",
  READY: "The container is running and routed to its public hostname.",
  FAILED:
    "The deployment failed. Its container, image, and workspace were cleaned up.",
  STOPPED: "The container has stopped.",
};

export default function DeploymentPage() {
  const params = useParams<{ deploymentId: string }>();
  const deploymentId = params.deploymentId;
  const router = useRouter();
  const { accessToken } = useAuth();
  const reportError = useApiError();

  const [deployment, setDeployment] = useState<Deployment | null>(null);
  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [running, setRunning] = useState(false);

  const [logText, setLogText] = useState("");
  const [logError, setLogError] = useState<string | null>(null);
  const [streaming, setStreaming] = useState(false);
  const logAbort = useRef<AbortController | null>(null);
  const logPane = useRef<HTMLPreElement>(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const active = running || (deployment ? isDeploymentActive(deployment.status) : false);
  const publicUrl = deployment ? deploymentUrl(deployment.deploymentId) : null;

  const fetchDeployment = useCallback(
    async (signal?: AbortSignal, quiet = false) => {
      if (!accessToken) return null;

      try {
        const next = await getDeployment(accessToken, deploymentId, signal);
        setDeployment(next);
        setLoadError(null);
        return next;
      } catch (err) {
        if (isAbortError(err)) return null;

        // Background polls should not replace the page with an error unless
        // the session has actually expired.
        if (quiet && !(err instanceof ApiError && err.status === 401)) {
          return null;
        }

        const message = reportError(err);
        if (message) setLoadError(message);
        return null;
      }
    },
    [accessToken, deploymentId, reportError],
  );

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    void fetchDeployment(controller.signal).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [fetchDeployment]);

  const projectId = deployment?.projectId;

  useEffect(() => {
    if (!accessToken || !projectId) return;

    let cancelled = false;
    getProject(accessToken, projectId)
      .then((loaded) => {
        if (!cancelled) setProject(loaded);
      })
      .catch(() => {
        // The breadcrumb falls back to the project id.
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, projectId]);

  useEffect(() => {
    if (!active) return;
    const timer = window.setInterval(() => {
      void fetchDeployment(undefined, true);
    }, 2500);
    return () => window.clearInterval(timer);
  }, [active, fetchDeployment]);

  useEffect(() => {
    const pane = logPane.current;
    if (pane) pane.scrollTop = pane.scrollHeight;
  }, [logText]);

  useEffect(() => {
    return () => logAbort.current?.abort();
  }, []);

  async function handleRun() {
    if (!accessToken || running) return;

    setRunning(true);
    setActionError(null);

    try {
      await executeDeployment(accessToken, deploymentId);
    } catch (err) {
      const message = reportError(err);
      if (message) {
        setActionError(`${message} The deployment's final status is below.`);
      }
    } finally {
      setRunning(false);
      await fetchDeployment(undefined, true);
    }
  }

  async function startLogs() {
    if (!accessToken || streaming) return;

    setLogError(null);
    setStreaming(true);

    const controller = new AbortController();
    logAbort.current = controller;

    try {
      await streamDeploymentLogs(accessToken, deploymentId, {
        signal: controller.signal,
        onLog: (chunk) => {
          setLogText((current) => {
            const next = current + chunk;
            return next.length > 200_000 ? next.slice(-200_000) : next;
          });
        },
      });
    } catch (err) {
      if (!isAbortError(err)) {
        const message = reportError(err);
        if (message) setLogError(message);
      }
    } finally {
      if (logAbort.current === controller) logAbort.current = null;
      setStreaming(false);
    }
  }

  function stopLogs() {
    logAbort.current?.abort();
    logAbort.current = null;
    setStreaming(false);
  }

  async function handleDelete() {
    if (!accessToken || !deployment || deleting) return;

    setDeleting(true);
    setDeleteError(null);

    try {
      await deleteDeployment(accessToken, deployment.deploymentId);
      router.push(`/projects/${deployment.projectId}`);
    } catch (err) {
      const message = reportError(err);
      if (message) setDeleteError(message);
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return <PageLoading label="Loading deployment" />;
  }

  if (!deployment) {
    return (
      <div className="space-y-5">
        <Link
          href="/dashboard"
          className="text-[13.5px] text-muted transition-colors hover:text-fg"
        >
          ← Back to projects
        </Link>
        <ErrorBanner
          message={loadError ?? "This deployment could not be loaded."}
        />
      </div>
    );
  }

  const shortId = deployment.commitHash?.slice(0, 8) ?? deployment.deploymentId.slice(0, 8);
  const logsAvailable = deployment.status === "READY";
  const liveUrl = deployment.status === "READY" ? publicUrl : null;

  return (
    <div className="space-y-8">
      <Link
        href={`/projects/${deployment.projectId}`}
        className="inline-flex text-[13.5px] text-muted transition-colors hover:text-fg"
      >
        ← {project ? project.projectName : "Back to project"}
      </Link>

      <PageHeader
        eyebrow="Deployment"
        title={shortId}
        description={`Deployment ${deployment.deploymentId}`}
        actions={
          <>
            {deployment.status === "QUEUED" ? (
              <Button onClick={() => void handleRun()} disabled={running}>
                {running ? "Running…" : "Run deployment"}
              </Button>
            ) : null}
            {liveUrl ? (
              <Button href={liveUrl} target="_blank" rel="noreferrer">
                Open app
              </Button>
            ) : null}
            <Button
              variant="ghost"
              onClick={() => {
                setDeleteError(null);
                setDeleteOpen(true);
              }}
              disabled={active || deleting}
            >
              Delete
            </Button>
          </>
        }
      />

      {actionError ? <ErrorBanner message={actionError} /> : null}

      {deployment.status === "FAILED" && !actionError ? (
        <ErrorBanner
          message="This deployment failed and its resources were cleaned up. Build logs are not stored yet, so check the backend logs for the underlying error."
        />
      ) : null}

      <section className={`${cardClass} p-6`}>
        <div className="flex flex-wrap items-center gap-3">
          <StatusPill status={deployment.status} />
          <p className="text-[13.5px] leading-relaxed text-muted">
            {statusCopy[deployment.status]}
          </p>
        </div>

        <dl className="mt-6 grid gap-x-8 gap-y-5 sm:grid-cols-2">
          <Detail label="Commit">
            {deployment.commitHash ? (
              <span className="font-mono text-[12.5px]" title={deployment.commitHash}>
                {deployment.commitHash.slice(0, 12)}
              </span>
            ) : (
              <span className="text-muted">Not built yet</span>
            )}
          </Detail>

          <Detail label="Image">
            {deployment.imageName ? (
              <span className="break-all font-mono text-[12.5px]">
                {deployment.imageName}
              </span>
            ) : (
              <span className="text-muted">Not built yet</span>
            )}
          </Detail>

          <Detail label="Public URL">
            {liveUrl ? (
              <a
                href={liveUrl}
                target="_blank"
                rel="noreferrer"
                className="break-all text-[13px] text-secondary transition-colors hover:text-fg"
              >
                {liveUrl}
              </a>
            ) : (
              <span className="text-[13px] text-muted">
                {deployment.status === "READY"
                  ? "Set NEXT_PUBLIC_DEPLOY_DOMAIN to build a link."
                  : "Available once the container is running."}
              </span>
            )}
          </Detail>

          <Detail label="Deployment ID">
            <span className="break-all font-mono text-[12.5px]">
              {deployment.deploymentId}
            </span>
          </Detail>
        </dl>
      </section>

      <section className={cardClass}>
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-panel-line px-5 py-4">
          <div>
            <h2 className="text-[16px] font-medium text-fg">Container logs</h2>
            <p className="mt-1 text-[13px] text-muted">
              Live stdout and stderr from the running container.
            </p>
          </div>

          <div className="flex gap-2">
            {logText ? (
              <Button variant="ghost" onClick={() => setLogText("")}>
                Clear
              </Button>
            ) : null}
            {streaming ? (
              <Button variant="ghost" onClick={stopLogs}>
                Stop
              </Button>
            ) : (
              <Button
                variant="ghost"
                onClick={() => void startLogs()}
                disabled={!logsAvailable}
              >
                Connect
              </Button>
            )}
          </div>
        </div>

        {logError ? (
          <div className="px-5 pt-4">
            <ErrorBanner message={logError} />
          </div>
        ) : null}

        {logsAvailable ? (
          <pre
            ref={logPane}
            className="max-h-[420px] overflow-auto whitespace-pre-wrap break-words px-5 py-4 font-mono text-[11.5px] leading-relaxed text-muted"
          >
            {logText || "Waiting for output…"}
          </pre>
        ) : (
          <p className="px-5 py-8 text-[13.5px] text-muted">
            Logs become available once the container is running. Clone and
            build output is not streamed yet.
          </p>
        )}
      </section>

      {deleteOpen ? (
        <Modal
          title="Delete this deployment?"
          description="This stops and removes its container, image, network, and workspace. Deployment history for it is deleted too."
          onClose={() => {
            if (!deleting) setDeleteOpen(false);
          }}
        >
          {deleteError ? <ErrorBanner message={deleteError} /> : null}
          <div className="flex justify-end gap-3">
            <Button
              variant="ghost"
              onClick={() => setDeleteOpen(false)}
              disabled={deleting}
            >
              Cancel
            </Button>
            <Button onClick={() => void handleDelete()} disabled={deleting}>
              {deleting ? "Deleting…" : "Delete deployment"}
            </Button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}

function Detail({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div>
      <dt className="text-[11px] uppercase tracking-[0.14em] text-muted">
        {label}
      </dt>
      <dd className="mt-1.5 text-fg">{children}</dd>
    </div>
  );
}
