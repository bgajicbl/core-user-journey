import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { api, ApiError, setUnauthorizedHandler } from "./client";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("api client", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    setUnauthorizedHandler(null);
    vi.unstubAllGlobals();
  });

  it("returns validated data on success", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { id: 1, name: "Ada" }));
    const schema = z.object({ id: z.number(), name: z.string() });

    await expect(api.get("/thing", schema)).resolves.toEqual({ id: 1, name: "Ada" });
  });

  it("throws ApiError with the server message on a non-ok response", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(404, { message: "Not found" }));

    await expect(api.get("/missing", z.unknown())).rejects.toMatchObject({
      message: "Not found",
      status: 404,
    });
  });

  it("throws ApiError when the response doesn't match the schema", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { unexpected: true }));
    const schema = z.object({ id: z.number() });

    await expect(api.get("/thing", schema)).rejects.toBeInstanceOf(ApiError);
  });

  it("fires the unauthorized handler on a 401 from an authenticated request", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { message: "Invalid or expired token" }));
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.get("/lms/dashboard", z.unknown(), { token: "expired-token" })).rejects.toBeInstanceOf(
      ApiError,
    );
    expect(handler).toHaveBeenCalledOnce();
  });

  it("does not fire the unauthorized handler on a 401 with no token (e.g. bad login credentials)", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { message: "Invalid email or password" }));
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.post("/auth/login", z.unknown(), { email: "a@b.com", password: "wrong" })).rejects.toBeInstanceOf(
      ApiError,
    );
    expect(handler).not.toHaveBeenCalled();
  });
});
