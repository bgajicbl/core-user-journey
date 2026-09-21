import type { ReactNode } from "react";
import type { AsyncState } from "../hooks/useApiResource";

interface AsyncBoundaryProps<T> {
  state: AsyncState<T>;
  loadingText?: string;
  /** Optional heading shown above the error message, for pages that need a distinct failure title. */
  errorTitle?: string;
  children: (data: T) => ReactNode;
}

export function AsyncBoundary<T>({ state, loadingText = "Loading...", errorTitle, children }: AsyncBoundaryProps<T>) {
  if (state.status === "loading") {
    return (
      <p aria-live="polite" className="loading">
        {loadingText}
      </p>
    );
  }

  if (state.status === "error") {
    return (
      <>
        {errorTitle && <h1>{errorTitle}</h1>}
        <p className="error" role="alert">
          {state.error}
        </p>
      </>
    );
  }

  return <>{children(state.data)}</>;
}
