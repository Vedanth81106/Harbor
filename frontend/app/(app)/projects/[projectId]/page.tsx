"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
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
  inputClass,
  labelClass,
} from "@/components/app/ui";
import {
  ApiError,
  createDeployment,
  deleteProjectDeployments,
  executeDeployment,
  getProject,
  isAbortError,
  isDeploymentActive,
  listDeployments,
  updateProjectRepository,
  type Deployment,
  type Project,
} from "@/lib/api";

export default function ProjectPage() {
  const params = useParams<{ projectId: string }>();
  const projectId = params.projectId;
  const { accessToken } = useAuth();
  const reportError = useApiError();

  const [project, setProject] = useState<Project | null>(null);
  const [deployments, setDeployments] = useState<Deployment[]>([]);

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [repoDraft, setRepoDraft] = useState("");
  const [repoPending, setRepoPending] = useState(false);
  const [repoSaved, setRepoSaved] = useState(false);
  const [repoError, setRepoError] = useState<string | null>(null);

  const [creating, setCreating] = useState(false);
  const [runningId, setRunningId] = useState<string | null>(null);
  const [cleanupOpen, setCleanupOpen] = useState(false);
  const [cleaning, setCleaning] = useState(false);
  const [cleanupError, setCleanupError] = useState<string | null>(null);

  const hasActive = deployments.some((deployment) =>
    isDeploymentActive(deployment.status),
  );
  const busy = creating || runningId !== null || hasActive;

  const load = useCallback(
    async (signal?: AbortSignal) => {
      if (!accessToken) return;

      setLoading(true);
      setLoadError(null);

      try {
        const [loadedProject, loadedDeployments] = await Promise.all([
          getProject(accessToken, projectId, signal),
          listDeployments(accessToken, projectId, signal),
        ]);
        setProject(loadedProject);
        setDeployments(loadedDeployments);
        setRepoDraft(loadedProject.repositoryUrl ?? "");
      } catch (err) {
        if (isAbortError(err)) return;
        const message = reportError(err);
        if (message) setLoadError(message);
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [accessToken, projectId, reportError],
  );

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const refreshDeployments = useCallback(async () => {
    if (!accessToken) return;
    try {
      const next = await listDeployments(accessToken, projectId);
      setDeployments(next);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        const message = reportError(err);
        if (message) setActionError(message);
      }
    }
  }, [accessToken, projectId, reportError]);

  // While a deployment is moving through the pipeline, keep the history fresh.
  useEffect(() => {
    if (!hasActive && runningId === null) return;
    const timer = window.setInterval(() => {
      void refreshDeployments();
    }, 2500);
    return () => window.clearInterval(timer);
  }, [hasActive, runningId, refreshDeployments]);

  async function saveRepository(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || !project || repoPending) return;

    const repositoryUrl = repoDraft.trim();
    if (!/^https:\/\/\S+$/i.test(repositoryUrl)) {
      setRepoError(
        "Use an HTTPS repository URL, for example https://github.com/you/repo.git.",
      );
      return;
    }

    setRepoError(null);
    setRepoSaved(false);
    setRepoPending(true);

    try {
      const updated = await updateProjectRepository(
        accessToken,
        project.projectId,
        repositoryUrl,
      );
      setProject(updated);
      setRepoDraft(updated.repositoryUrl ?? repositoryUrl);
      setRepoSaved(true);
    } catch (err) {
      const message = reportError(err);
      if (message) setRepoError(message);
    } finally {
      setRepoPending(false);
    }
  }

  async function runDeployment(deploymentId: string) {
    if (!accessToken) return;

    setRunningId(deploymentId);
    setActionError(null);

    try {
      await executeDeployment(accessToken, deploymentId);
    } catch (err) {
      const message = reportError(err);
      if (message) {
        setActionError(
          `The deployment did not finish: ${message} Refresh to see its final status.`,
        );
      }
    } finally {
      setRunningId(null);
      await refreshDeployments();
    }
  }

  async function handleDeploy() {
    if (!accessToken || !project || busy) return;
    if (!project.repositoryUrl) {
      setActionError("Connect a repository before deploying this project.");
      return;
    }

    setCreating(true);
    setActionError(null);

    try {
      const deployment = await createDeployment(
        accessToken,
        project.projectId,
      );
      setDeployments((current) => [deployment, ...current]);
      await runDeployment(deployment.deploymentId);
    } catch (err) {
      const message = reportError(err);
      if (message) setActionError(message);
    } finally {
      setCreating(false);
    }
  }

  async function handleCleanup() {
    if (!accessToken || !project || cleaning) return;

    setCleaning(true);
    setCleanupError(null);

    try {
      await deleteProjectDeployments(accessToken, project.projectId);
      setDeployments([]);
      setCleanupOpen(false);
    } catch (err) {
      const message = reportError(err);
      if (message) setCleanupError(message);
    } finally {
      setCleaning(false);
    }
  }

  if (loading) {
    return <PageLoading label="Loading project" />;
  }

  if (!project) {
    return (
      <div className="space-y-5">
        <Link
          href="/dashboard"
          className="text-[13.5px] text-muted transition-colors hover:text-fg"
        >
          ← Back to projects
        </Link>
        <ErrorBanner message={loadError ?? "This project could not be loaded."} />
        <Button variant="ghost" onClick={() => void load()}>
          Try again
        </Button>
      </div>
    );
  }

  const repoChanged =
    repoDraft.trim().length > 0 &&
    repoDraft.trim() !== (project.repositoryUrl ?? "");

  return (
    <div className="space-y-8">
      <Link
        href="/dashboard"
        className="inline-flex text-[13.5px] text-muted transition-colors hover:text-fg"
      >
        ← Back to projects
      </Link>

      <PageHeader
        eyebrow="Project"
        title={project.projectName}
        description={
          project.repositoryUrl ?? "No repository connected yet."
        }
        actions={
          <Button
            onClick={() => void handleDeploy()}
            disabled={busy || !project.repositoryUrl}
            title={
              project.repositoryUrl
                ? undefined
                : "Connect a repository before deploying"
            }
          >
            {runningId
              ? "Deploying…"
              : creating
                ? "Starting…"
                : "Deploy"}
          </Button>
        }
      />

      {actionError ? <ErrorBanner message={actionError} /> : null}

      <section className={`${cardClass} p-6`}>
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-[16px] font-medium text-fg">Repository</h2>
            <p className="mt-1 text-[13.5px] text-muted">
              Harbor clones this repository and builds its Dockerfile.
            </p>
          </div>
          {repoSaved ? (
            <span className="shrink-0 text-[12.5px] text-secondary">Saved</span>
          ) : null}
        </div>

        <form className="mt-5" onSubmit={saveRepository}>
          <label htmlFor="repository-url" className={labelClass}>
            Repository URL
          </label>
          <input
            id="repository-url"
            type="url"
            autoComplete="off"
            placeholder="https://github.com/you/repo.git"
            required
            disabled={repoPending}
            value={repoDraft}
            onChange={(event) => {
              setRepoDraft(event.target.value);
              setRepoSaved(false);
            }}
            className={inputClass}
          />
          <p className="mt-1.5 text-[12px] text-muted">
            Public HTTPS Git repository with a Dockerfile at its root.
          </p>

          {repoError ? (
            <ErrorBanner message={repoError} className="mt-4" />
          ) : null}

          <div className="mt-4 flex justify-end">
            <Button
              type="submit"
              variant="ghost"
              disabled={repoPending || !repoChanged}
            >
              {repoPending ? "Saving…" : "Save repository"}
            </Button>
          </div>
        </form>
      </section>

      <section className={cardClass}>
        <div className="flex items-center justify-between gap-4 border-b border-panel-line px-5 py-4">
          <div>
            <h2 className="text-[16px] font-medium text-fg">Deployments</h2>
            <p className="mt-1 text-[13px] text-muted">
              {deployments.length > 0
                ? `${deployments.length} deployment${deployments.length === 1 ? "" : "s"}`
                : "No deployments yet"}
            </p>
          </div>
          {deployments.length > 0 ? (
            <Button
              variant="ghost"
              onClick={() => {
                setCleanupError(null);
                setCleanupOpen(true);
              }}
              disabled={busy}
            >
              Clean up all
            </Button>
          ) : null}
        </div>

        {deployments.length === 0 ? (
          <div className="px-5 py-12 text-center text-[14px] text-muted">
            Deploy this project to see its history and logs here.
          </div>
        ) : (
          <ul>
            {deployments.map((deployment) => (
              <li
                key={deployment.deploymentId}
                className="flex items-center gap-4 border-b border-panel-line px-5 py-4 last:border-b-0"
              >
                <StatusPill status={deployment.status} />

                <Link
                  href={`/deployments/${deployment.deploymentId}`}
                  className="group min-w-0 flex-1"
                >
                  <p className="truncate font-mono text-[12px] text-fg group-hover:text-secondary">
                    {deployment.commitHash?.slice(0, 12) ??
                      deployment.deploymentId.slice(0, 8)}
                  </p>
                  <p className="mt-1 truncate text-[12px] text-muted">
                    {deployment.imageName ?? "Waiting to build"}
                  </p>
                </Link>

                {deployment.status === "QUEUED" ? (
                  <Button
                    variant="ghost"
                    onClick={() => void runDeployment(deployment.deploymentId)}
                    disabled={busy}
                  >
                    {runningId === deployment.deploymentId ? "Running…" : "Run"}
                  </Button>
                ) : null}

                <Link
                  href={`/deployments/${deployment.deploymentId}`}
                  className="shrink-0 text-[13px] text-muted transition-colors hover:text-secondary"
                >
                  View
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {actionsHint(project, busy)}

      {cleanupOpen ? (
        <Modal
          title="Delete all deployments?"
          description={`This stops and removes every container and image owned by ${project.projectName}. The project and its repository URL stay.`}
          onClose={() => {
            if (!cleaning) setCleanupOpen(false);
          }}
        >
          {cleanupError ? <ErrorBanner message={cleanupError} /> : null}
          <div className="flex justify-end gap-3">
            <Button
              variant="ghost"
              onClick={() => setCleanupOpen(false)}
              disabled={cleaning}
            >
              Cancel
            </Button>
            <Button onClick={() => void handleCleanup()} disabled={cleaning}>
              {cleaning ? "Deleting…" : "Delete deployments"}
            </Button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}

function actionsHint(project: Project, busy: boolean) {
  if (project.repositoryUrl) {
    return (
      <p className="text-[12.5px] text-muted">
        Deploying runs clone → build → start. Keep this page open while it
        finishes; the status updates live.{" "}
        {busy ? "A deployment is currently running." : null}
      </p>
    );
  }

  return (
    <div className="rounded-xl border border-secondary-border bg-secondary-wash px-4 py-3 text-[13.5px] leading-relaxed text-secondary">
      Connect a repository above to enable deployments.
    </div>
  );
}
