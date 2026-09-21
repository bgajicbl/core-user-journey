import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AsyncBoundary } from "./AsyncBoundary";

describe("AsyncBoundary", () => {
  it("renders loadingText while loading", () => {
    render(
      <AsyncBoundary state={{ status: "loading" }} loadingText="Loading invitation...">
        {() => null}
      </AsyncBoundary>,
    );
    expect(screen.getByText("Loading invitation...")).toBeInTheDocument();
  });

  it("renders an errorTitle heading above the error message when provided", () => {
    render(
      <AsyncBoundary state={{ status: "error", error: "Invitation not found" }} errorTitle="Invitation not found">
        {() => null}
      </AsyncBoundary>,
    );
    expect(screen.getByRole("heading", { name: "Invitation not found" })).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Invitation not found");
  });

  it("renders no heading when errorTitle is omitted", () => {
    render(
      <AsyncBoundary state={{ status: "error", error: "Something broke" }}>{() => null}</AsyncBoundary>,
    );
    expect(screen.queryByRole("heading")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Something broke");
  });

  it("renders children with the resolved data on success", () => {
    render(
      <AsyncBoundary state={{ status: "success", data: { name: "Ada" } }}>
        {(data) => <p>Hello {data.name}</p>}
      </AsyncBoundary>,
    );
    expect(screen.getByText("Hello Ada")).toBeInTheDocument();
  });
});
