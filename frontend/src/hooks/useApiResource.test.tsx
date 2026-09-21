import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { useApiResource } from "./useApiResource";

const schema = z.object({ id: z.number() });

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("useApiResource", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("starts in loading state, then resolves to success", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { id: 1 }));

    const { result } = renderHook(() => useApiResource("/thing", schema));

    expect(result.current.status).toBe("loading");
    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current).toEqual({ status: "success", data: { id: 1 } });
  });

  it("resolves to an error state on failure", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(404, { message: "Not found" }));

    const { result } = renderHook(() => useApiResource("/missing", schema));

    await waitFor(() => expect(result.current.status).toBe("error"));
    expect(result.current).toEqual({ status: "error", error: "Not found" });
  });

  it("does not fetch when path is null", () => {
    renderHook(() => useApiResource(null, schema));
    expect(fetch).not.toHaveBeenCalled();
  });

  it("re-fetches when the path changes", async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse(200, { id: 1 }))
      .mockResolvedValueOnce(jsonResponse(200, { id: 2 }));

    const { result, rerender } = renderHook(({ path }) => useApiResource(path, schema), {
      initialProps: { path: "/thing/1" },
    });
    await waitFor(() => expect(result.current).toEqual({ status: "success", data: { id: 1 } }));

    rerender({ path: "/thing/2" });
    expect(result.current.status).toBe("loading");
    await waitFor(() => expect(result.current).toEqual({ status: "success", data: { id: 2 } }));

    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it("does not loop when passed a fresh schema reference on every render", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, { id: 1 }));

    // Simulates a caller writing `useApiResource(path, z.object({...}))` inline instead of
    // hoisting the schema to module scope — a new ZodType instance on every render.
    const { result, rerender } = renderHook(() => useApiResource("/thing", z.object({ id: z.number() })));

    await waitFor(() => expect(result.current).toEqual({ status: "success", data: { id: 1 } }));

    rerender();
    rerender();

    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("validates against the latest schema even though schema isn't an effect dependency", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { id: "not-a-number" }));

    const { result } = renderHook(() => useApiResource("/thing", schema));

    await waitFor(() => expect(result.current.status).toBe("error"));
  });
});
