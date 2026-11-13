"use client";

import { useCallback } from "react";
import { useAuth } from "@/components/auth/auth-provider";
import { ApiError, isAbortError } from "@/lib/api";

/**
 * Normalises API failures for the authenticated pages. A 401 means the stored
 * token is no longer accepted, so the session is cleared; RequireAuth then
 * sends the visitor to the landing page with the login dialog open.
 */
export function useApiError() {
  const { logout } = useAuth();

  return useCallback(
    (error: unknown): string | null => {
      if (isAbortError(error)) return null;

      if (error instanceof ApiError && error.status === 401) {
        logout();
        return "Your session expired. Please log in again.";
      }

      return error instanceof ApiError
        ? error.message
        : "Something went wrong. Please try again.";
    },
    [logout],
  );
}
