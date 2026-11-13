import type { NextRequest } from "next/server";

/**
 * Same-origin gateway to the Harbor backend.
 *
 * The browser only ever talks to the Next.js server; this handler forwards
 * `/api/*` to the backend over the private Docker network (or to localhost
 * during local development). Keeping the backend off the public internet means
 * no Traefik route, no published port, no CORS surface, and no way for a client
 * to reach backend endpoints that the UI does not use.
 *
 * Environment:
 *   INTERNAL_API_URL - backend origin, defaults to http://localhost:8080
 */

export const dynamic = "force-dynamic";
export const fetchCache = "force-no-store";
export const runtime = "nodejs";

const BACKEND_URL = (
  process.env.INTERNAL_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

/** Headers that must not be forwarded in either direction. */
const HOP_BY_HOP = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
  "host",
]);

type RouteContext = { params: Promise<{ path: string[] }> };

async function proxy(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;

  const target = new URL(`${BACKEND_URL}/${path.join("/")}`);
  target.search = request.nextUrl.search;

  const headers = new Headers(request.headers);
  for (const name of HOP_BY_HOP) headers.delete(name);
  // Let the runtime negotiate compression so the response body we pass through
  // is never left encoded with headers that no longer describe it.
  headers.delete("accept-encoding");

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? request.body : undefined,
      // Node requires this when the request body is a stream.
      duplex: "half",
      redirect: "manual",
      cache: "no-store",
      signal: request.signal,
    } as RequestInit & { duplex: "half" });
  } catch {
    return Response.json(
      {
        status: 502,
        message:
          "Could not reach the Harbor API. Check that the backend is running and try again.",
      },
      { status: 502 },
    );
  }

  const responseHeaders = new Headers(upstream.headers);
  for (const name of HOP_BY_HOP) responseHeaders.delete(name);
  // The body may have been decoded or re-framed by the runtime.
  responseHeaders.delete("content-encoding");
  responseHeaders.delete("content-length");

  // Passing the stream straight through preserves SSE log streaming.
  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
export const OPTIONS = proxy;
