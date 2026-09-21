import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { checkoutResponseSchema, courseSchema } from "../api/schemas";

export function CheckoutPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const navigate = useNavigate();

  const courseState = useApiResource(courseId ? `/courses/${courseId}` : null, courseSchema);

  const [parentEmail, setParentEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent, courseId: number) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = await api.post("/checkout", checkoutResponseSchema, { courseId, parentEmail });
      navigate(`/checkout/success/${result.invitationToken}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Checkout failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Checkout</h1>
      <AsyncBoundary state={courseState} loadingText="Loading course...">
        {(course) => (
          <>
            <div className="card">
              <h2>{course.subject}</h2>
              <p>{course.yearRange}</p>
              <p className="price">£{course.price.toFixed(2)}</p>
            </div>

            <form onSubmit={(e) => handleSubmit(e, course.id)} className="form">
              <label htmlFor="parentEmail">Parent email</label>
              <input
                id="parentEmail"
                type="email"
                required
                value={parentEmail}
                onChange={(e) => setParentEmail(e.target.value)}
                placeholder="you@example.com"
              />

              <p className="hint">This is a mock checkout — no payment details are collected.</p>

              {error && (
                <p className="error" role="alert">
                  {error}
                </p>
              )}

              <button type="submit" disabled={submitting}>
                {submitting ? "Processing..." : `Pay £${course.price.toFixed(2)} & complete purchase`}
              </button>
            </form>
          </>
        )}
      </AsyncBoundary>
    </div>
  );
}
