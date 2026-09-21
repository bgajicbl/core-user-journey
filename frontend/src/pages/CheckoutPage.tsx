import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { CheckoutResponse, Course } from "../api/types";

export function CheckoutPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const navigate = useNavigate();

  const [course, setCourse] = useState<Course | null>(null);
  const [parentEmail, setParentEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Course[]>("/courses")
      .then((courses) => {
        const match = courses.find((c) => String(c.id) === courseId);
        setCourse(match ?? null);
      })
      .catch((err: Error) => setError(err.message));
  }, [courseId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!course) return;
    setSubmitting(true);
    setError(null);
    try {
      const result = await api.post<CheckoutResponse>("/checkout", {
        courseId: course.id,
        parentEmail,
      });
      navigate(`/checkout/success/${result.invitationToken}`, { state: result });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Checkout failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!course) {
    return (
      <div className="page">
        <p>{error ?? "Loading course..."}</p>
      </div>
    );
  }

  return (
    <div className="page page-narrow">
      <h1>Checkout</h1>
      <div className="card">
        <h2>{course.subject}</h2>
        <p>{course.yearRange}</p>
        <p className="price">£{course.price.toFixed(2)}</p>
      </div>

      <form onSubmit={handleSubmit} className="form">
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

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? "Processing..." : `Pay £${course.price.toFixed(2)} & complete purchase`}
        </button>
      </form>
    </div>
  );
}
