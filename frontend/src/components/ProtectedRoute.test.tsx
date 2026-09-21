import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import { AuthProvider } from "../auth/AuthContext";
import { ProtectedRoute } from "./ProtectedRoute";

function renderProtected(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route
            path="/lms"
            element={
              <ProtectedRoute>
                <p>Secret dashboard</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("redirects to /login when there is no token", () => {
    renderProtected("/lms");
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("renders the protected content when a token is present", () => {
    localStorage.setItem("cuj.token", "a-token");
    localStorage.setItem("cuj.studentName", "Ada");

    renderProtected("/lms");
    expect(screen.getByText("Secret dashboard")).toBeInTheDocument();
  });
});
