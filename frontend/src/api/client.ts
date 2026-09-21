import type { ZodType } from "zod";

const API_BASE = "/api";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * Called when an authenticated request (one that sent a token) comes back 401, meaning the
 * token itself was rejected as missing/invalid/expired — as opposed to a 401 from an
 * unauthenticated endpoint like /auth/login, which just means "wrong password".
 */
type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler;
}

interface RequestOptions {
  token?: string | null;
  signal?: AbortSignal;
}

async function request<T>(
  path: string,
  schema: ZodType<T>,
  options: { method?: string; body?: unknown } & RequestOptions = {},
): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (options.token) {
    headers["Authorization"] = `Bearer ${options.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const data = await response.json();
      if (data?.message) message = data.message;
    } catch {
      // response had no JSON body; fall back to the generic message
    }

    if (response.status === 401 && options.token) {
      onUnauthorized?.();
    }

    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return schema.parse(undefined);
  }

  const data: unknown = await response.json();
  const parsed = schema.safeParse(data);
  if (!parsed.success) {
    throw new ApiError(response.status, "Server response did not match the expected shape.");
  }
  return parsed.data;
}

export const api = {
  get: <T>(path: string, schema: ZodType<T>, options?: RequestOptions) => request(path, schema, options),
  post: <T>(path: string, schema: ZodType<T>, body?: unknown, options?: RequestOptions) =>
    request(path, schema, { ...options, method: "POST", body }),
};
