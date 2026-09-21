import { render, screen, waitFor } from "@testing-library/react";
import { useEffect } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { api } from "../api/client";
import { AuthProvider, useAuth } from "./AuthContext";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** Stands in for an LMS page that fires an authenticated request on mount. */
function AuthedPage() {
  const { token } = useAuth();
  useEffect(() => {
    api.get("/lms/dashboard", z.unknown(), { token }).catch(() => {
      // handled globally via the unauthorized handler under test
    });
  }, [token]);
  return <p>Dashboard</p>;
}

describe("AuthProvider unauthorized handling", () => {
  beforeEach(() => {
    localStorage.setItem("cuj.token", "expired-token");
    localStorage.setItem("cuj.studentName", "Ada");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(401, { message: "Invalid or expired token" })));
  });

  afterEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("logs out and redirects to /login when an authenticated request comes back 401", async () => {
    render(
      <MemoryRouter initialEntries={["/lms"]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<p>Login page</p>} />
            <Route path="/lms" element={<AuthedPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("Login page")).toBeInTheDocument());
    expect(localStorage.getItem("cuj.token")).toBeNull();
    expect(localStorage.getItem("cuj.studentName")).toBeNull();
  });
});
