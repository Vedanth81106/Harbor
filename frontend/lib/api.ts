const DEFAULT_API_URL = "/api";

/**
 * Base URL for backend calls.
 *
 * By default requests go to this app's own `/api` route, which the Next.js
 * server proxies to the backend over the internal network. Set
 * NEXT_PUBLIC_API_URL only to point the browser at a different backend origin.
 */
export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? DEFAULT_API_URL
).replace(/\/+$/, "");

export type AuthUser = {
  username: string;
  email: string;
};

export type AuthResult = AuthUser & {
  accessToken: string;
};

export type Project = {
  projectId: string;
  projectName: string;
  repositoryUrl: string | null;
};

export type DeploymentStatus =
  | "QUEUED"
  | "CLONING"
  | "BUILDING"
  | "STARTING"
  | "READY"
  | "FAILED"
  | "STOPPED";

export type Deployment = {
  deploymentId: string;
  projectId: string;
  status: DeploymentStatus;
  commitHash: string | null;
  imageName: string | null;
  hostPort: number;
};

type ErrorPayload = {
  status?: number;
  message?: string;
};

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

async function readErrorMessage(response: Response): Promise<string> {
  let payload: ErrorPayload | null = null;
  try {
    payload = (await response.json()) as ErrorPayload;
  } catch {
    // The backend can return an empty or non-JSON body on some failures.
  }

  const backendMessage = payload?.message?.trim() || null;

  if (response.status === 401) {
    return "Your session has expired. Please log in again.";
  }

  if (response.status === 404) {
    return backendMessage ?? "The requested resource could not be found.";
  }

  if (response.status === 409) {
    return backendMessage ?? "Those details are already in use.";
  }

  return backendMessage ?? `Request failed with status ${response.status}.`;
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  token?: string | null;
  body?: unknown;
  signal?: AbortSignal;
};

async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = "GET", token, body, signal } = options;

  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
      cache: "no-store",
    });
  } catch (error) {
    if (isAbortError(error)) throw error;
    throw new ApiError(
      "Could not reach the Harbor API. Check that the backend is running and try again.",
      0,
    );
  }

  if (!response.ok) {
    throw new ApiError(await readErrorMessage(response), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export async function login(input: {
  identifier: string;
  password: string;
}): Promise<AuthResult> {
  try {
    return await request<AuthResult>("/auth/login", {
      method: "POST",
      body: {
        usernameOrEmail: input.identifier,
        password: input.password,
      },
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      throw new ApiError("Invalid email, username, or password.", 401);
    }
    throw error;
  }
}

export function register(input: {
  username: string;
  email: string;
  password: string;
}): Promise<AuthResult> {
  return request<AuthResult>("/auth/register", {
    method: "POST",
    body: input,
  });
}

export function listProjects(token: string, signal?: AbortSignal) {
  return request<Project[]>("/projects", { token, signal });
}

export function createProject(
  token: string,
  input: { projectName: string; repositoryUrl?: string },
) {
  return request<Project>("/projects", {
    method: "POST",
    token,
    body: input,
  });
}

export function getProject(
  token: string,
  projectId: string,
  signal?: AbortSignal,
) {
  return request<Project>(`/projects/${projectId}`, { token, signal });
}

export function updateProjectRepository(
  token: string,
  projectId: string,
  repositoryUrl: string,
) {
  return request<Project>(`/projects/${projectId}`, {
    method: "PUT",
    token,
    body: { repositoryUrl },
  });
}

export function listDeployments(
  token: string,
  projectId: string,
  signal?: AbortSignal,
) {
  return request<Deployment[]>(`/deployments/project/${projectId}`, {
    token,
    signal,
  });
}

export function getDeployment(
  token: string,
  deploymentId: string,
  signal?: AbortSignal,
) {
  return request<Deployment>(`/deployments/${deploymentId}`, { token, signal });
}

export function createDeployment(token: string, projectId: string) {
  return request<Deployment>(`/deployments/${projectId}`, {
    method: "POST",
    token,
  });
}

/**
 * Runs the whole clone/build/start pipeline. This request stays open for the
 * duration of the deployment, so callers should poll getDeployment() in
 * parallel if they want live status updates.
 */
export function executeDeployment(
  token: string,
  deploymentId: string,
  signal?: AbortSignal,
) {
  return request<Deployment>(`/deployments/${deploymentId}/execute`, {
    method: "POST",
    token,
    signal,
  });
}

export function deleteDeployment(token: string, deploymentId: string) {
  return request<void>(`/deployments/${deploymentId}`, {
    method: "DELETE",
    token,
  });
}

export function deleteProjectDeployments(token: string, projectId: string) {
  return request<void>(`/projects/${projectId}/deployments`, {
    method: "DELETE",
    token,
  });
}

function parseSseEvent(rawEvent: string): string | null {
  const dataLines: string[] = [];

  for (const line of rawEvent.split("\n")) {
    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).replace(/^ /, ""));
      continue;
    }

    // Comments and other SSE fields carry no log content.
    if (
      line === "" ||
      line.startsWith(":") ||
      line.startsWith("event:") ||
      line.startsWith("id:") ||
      line.startsWith("retry:")
    ) {
      continue;
    }

    // Spring writes multi-line log payloads without a data: prefix on
    // continuation lines.
    dataLines.push(line);
  }

  if (dataLines.length === 0) return null;
  return dataLines.join("\n");
}

/**
 * Streams a deployment's container logs. EventSource cannot send bearer
 * headers, so this reads the SSE response body from a normal fetch().
 */
export async function streamDeploymentLogs(
  token: string,
  deploymentId: string,
  options: { signal?: AbortSignal; onLog: (chunk: string) => void },
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/deployments/${deploymentId}/logs`, {
      headers: {
        Accept: "text/event-stream",
        Authorization: `Bearer ${token}`,
      },
      signal: options.signal,
      cache: "no-store",
    });
  } catch (error) {
    if (isAbortError(error)) throw error;
    throw new ApiError("Could not connect to the deployment log stream.", 0);
  }

  if (!response.ok) {
    throw new ApiError(await readErrorMessage(response), response.status);
  }

  if (!response.body) {
    throw new ApiError("The deployment log stream returned no data.", 0);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      buffer = buffer.replace(/\r\n/g, "\n");

      let boundary = buffer.indexOf("\n\n");
      while (boundary !== -1) {
        const rawEvent = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);

        const text = parseSseEvent(rawEvent);
        if (text) {
          options.onLog(text.endsWith("\n") ? text : `${text}\n`);
        }

        boundary = buffer.indexOf("\n\n");
      }
    }

    // Flush an event that arrived without its trailing blank line.
    buffer += decoder.decode();
    buffer = buffer.replace(/\r\n/g, "\n");
    const tail = parseSseEvent(buffer);
    if (tail) {
      options.onLog(tail.endsWith("\n") ? tail : `${tail}\n`);
    }
  } finally {
    reader.releaseLock();
  }
}

const ACTIVE_STATUSES: DeploymentStatus[] = [
  "CLONING",
  "BUILDING",
  "STARTING",
];

export function isDeploymentActive(status: DeploymentStatus) {
  return ACTIVE_STATUSES.includes(status);
}

/**
 * The backend routes each READY container at
 * harbor-<first 8 chars of deployment id>.<DOMAIN_NAME>. The frontend has to
 * be told the same base domain to build a link. The value may include a
 * scheme; HTTPS is assumed when it does not.
 */
export function deploymentUrl(deploymentId: string): string | null {
  const domain = process.env.NEXT_PUBLIC_DEPLOY_DOMAIN?.trim();
  if (!domain) return null;

  const scheme = /^http:\/\//i.test(domain) ? "http" : "https";
  const host = domain.replace(/^https?:\/\//i, "").replace(/\/+$/, "");
  if (!host) return null;

  return `${scheme}://harbor-${deploymentId.slice(0, 8)}.${host}`;
}
