"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/components/auth/auth-provider";
import { CreateProjectDialog } from "@/components/app/create-project-dialog";
import { Button } from "@/components/ui/button";
import { useApiError } from "@/components/app/use-api-error";
import {
  EmptyState,
  ErrorBanner,
  PageHeader,
  cardClass,
} from "@/components/app/ui";
import { isAbortError, listProjects, type Project } from "@/lib/api";

export default function DashboardPage() {
  const { accessToken } = useAuth();
  const reportError = useApiError();

  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const loadProjects = useCallback(
    async (signal?: AbortSignal) => {
      if (!accessToken) return;

      setLoading(true);
      setError(null);

      try {
        const data = await listProjects(accessToken, signal);
        setProjects(data);
      } catch (err) {
        if (isAbortError(err)) return;
        const message = reportError(err);
        if (message) setError(message);
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [accessToken, reportError],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadProjects(controller.signal);
    return () => controller.abort();
  }, [loadProjects]);

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Console"
        title="Projects"
        description={
          projects.length > 0
            ? `${projects.length} project${projects.length === 1 ? "" : "s"} connected to this Harbor instance.`
            : "Create a project, point it at a repository, and deploy it."
        }
        actions={
          <Button onClick={() => setCreating(true)}>
            <span aria-hidden="true" className="text-[17px] leading-none">
              +
            </span>
            New project
          </Button>
        }
      />

      {error ? (
        <div className="space-y-3">
          <ErrorBanner message={error} />
          <Button variant="ghost" size="md" onClick={() => void loadProjects()}>
            Try again
          </Button>
        </div>
      ) : null}

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((key) => (
            <div
              key={key}
              className="h-[148px] animate-pulse rounded-2xl border border-panel-line bg-panel-2"
            />
          ))}
        </div>
      ) : null}

      {!loading && !error && projects.length === 0 ? (
        <EmptyState
          title="No projects yet"
          description="A project links a Git repository to a running container. Create your first one and Harbor will build it, start it, and give it a URL."
          action={
            <Button onClick={() => setCreating(true)}>Create a project</Button>
          }
        />
      ) : null}

      {!loading && projects.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <Link
              key={project.projectId}
              href={`/projects/${project.projectId}`}
              className={`${cardClass} group flex flex-col justify-between gap-6 p-6 transition-colors hover:border-secondary-border`}
            >
              <div className="flex items-start justify-between gap-4">
                <span
                  aria-hidden="true"
                  className="grid h-10 w-10 place-items-center rounded-lg border border-panel-line bg-panel-2 font-mono text-[14px] text-secondary"
                >
                  {project.projectName.slice(0, 1).toLowerCase()}
                </span>
                <span className="text-[13px] text-muted transition-colors group-hover:text-secondary">
                  Open
                </span>
              </div>

              <div className="min-w-0">
                <h2 className="truncate text-[18px] font-medium tracking-[-0.01em] text-fg">
                  {project.projectName}
                </h2>
                <p className="mt-1.5 truncate font-mono text-[11.5px] tracking-[0.01em] text-muted">
                  {project.repositoryUrl ?? "No repository connected"}
                </p>
              </div>
            </Link>
          ))}
        </div>
      ) : null}

      {creating ? (
        <CreateProjectDialog
          onClose={() => setCreating(false)}
          onCreated={(project) => setProjects((current) => [project, ...current])}
        />
      ) : null}
    </div>
  );
}
