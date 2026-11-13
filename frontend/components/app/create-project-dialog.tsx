"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/app/modal";
import { useApiError } from "@/components/app/use-api-error";
import { ErrorBanner, inputClass, labelClass } from "@/components/app/ui";
import { useAuth } from "@/components/auth/auth-provider";
import { createProject, type Project } from "@/lib/api";

export function CreateProjectDialog({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (project: Project) => void;
}) {
  const { accessToken } = useAuth();
  const reportError = useApiError();

  const [projectName, setProjectName] = useState("");
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || pending) return;

    const name = projectName.trim();
    const repo = repositoryUrl.trim();

    if (name.length < 3) {
      setError("Project names need at least 3 characters.");
      return;
    }

    if (repo && !/^https:\/\/\S+$/i.test(repo)) {
      setError(
        "Use an HTTPS repository URL, for example https://github.com/you/repo.git.",
      );
      return;
    }

    setError(null);
    setPending(true);

    try {
      const project = await createProject(accessToken, {
        projectName: name,
        ...(repo ? { repositoryUrl: repo } : {}),
      });
      onCreated(project);
      onClose();
    } catch (err) {
      const message = reportError(err);
      if (message) setError(message);
    } finally {
      setPending(false);
    }
  }

  return (
    <Modal
      title="New project"
      description="Give it a name now; you can connect a repository here or from the project page."
      onClose={onClose}
    >
      <form className="space-y-5" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="project-name" className={labelClass}>
            Project name
          </label>
          <input
            id="project-name"
            type="text"
            autoComplete="off"
            placeholder="storefront"
            minLength={3}
            maxLength={100}
            required
            disabled={pending}
            value={projectName}
            onChange={(event) => setProjectName(event.target.value)}
            className={inputClass}
          />
          <p className="mt-1.5 text-[12px] text-muted">
            At least 3 characters. Names are unique per account.
          </p>
        </div>

        <div>
          <label htmlFor="project-repository" className={labelClass}>
            Repository URL <span className="font-normal text-muted">(optional)</span>
          </label>
          <input
            id="project-repository"
            type="url"
            autoComplete="off"
            placeholder="https://github.com/you/repo.git"
            disabled={pending}
            value={repositoryUrl}
            onChange={(event) => setRepositoryUrl(event.target.value)}
            className={inputClass}
          />
          <p className="mt-1.5 text-[12px] text-muted">
            Must be a public HTTPS Git repository with a Dockerfile at its root.
          </p>
        </div>

        {error ? <ErrorBanner message={error} /> : null}

        <div className="flex justify-end gap-3 pt-1">
          <Button variant="ghost" onClick={onClose} disabled={pending}>
            Cancel
          </Button>
          <Button type="submit" disabled={pending}>
            {pending ? "Creating…" : "Create project"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
