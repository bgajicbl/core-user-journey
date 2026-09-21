import { useEffect, useRef, useState } from "react";
import type { ZodType } from "zod";
import { api, ApiError } from "../api/client";

export type AsyncState<T> =
  | { status: "loading" }
  | { status: "error"; error: string }
  | { status: "success"; data: T };

/**
 * Fetches `path` with GET and re-fetches whenever `path` or `token` changes. Cancels the
 * in-flight request on unmount or before starting the next one, so a stale response from a
 * superseded request can never overwrite newer state.
 *
 * Pass `path: null` to skip fetching (e.g. while a required route param is still unknown).
 *
 * `schema` is not a fetch trigger — it describes the shape of whatever `path` returns, so it
 * intentionally isn't in the effect's dependency array. It's read through a ref that's kept
 * current every render instead: passing a fresh inline schema (e.g. `z.object({...})` written
 * directly in a render) still always validates against the latest schema, but can never itself
 * cause a re-fetch loop the way including it in the deps array would.
 */
export function useApiResource<T>(
  path: string | null,
  schema: ZodType<T>,
  token?: string | null,
): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({ status: "loading" });

  const schemaRef = useRef(schema);
  // "Latest ref" pattern: written every render, never read during render, so it can't cause a
  // missed update the way reading a ref during render can.
  // oxlint-disable-next-line react/refs
  schemaRef.current = schema;

  useEffect(() => {
    if (path === null) return;

    const controller = new AbortController();
    // Reset to loading whenever `path`/`token` change so a stale success/error from the
    // previous resource doesn't flash before the new fetch resolves.
    // oxlint-disable-next-line react/set-state-in-effect
    setState({ status: "loading" });

    api
      .get(path, schemaRef.current, { token, signal: controller.signal })
      .then((data) => setState({ status: "success", data }))
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === "AbortError") return;
        setState({
          status: "error",
          error: err instanceof ApiError ? err.message : "Something went wrong. Please try again.",
        });
      });

    return () => controller.abort();
  }, [path, token]);

  return state;
}
