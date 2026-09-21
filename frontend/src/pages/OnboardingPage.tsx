import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { authResponseSchema, onboardingInfoSchema } from "../api/schemas";

export function OnboardingPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { login } = useAuth();

  const infoState = useApiResource(token ? `/onboarding/${token}` : null, onboardingInfoSchema);

  const [studentName, setStudentName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const auth = await api.post(`/onboarding/${token}`, authResponseSchema, {
        studentName,
        email,
        password,
      });
      login(auth.token, auth.studentName);
      navigate("/lms");
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : "Could not complete onboarding.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page page-narrow">
      <AsyncBoundary state={infoState} loadingText="Loading invitation..." errorTitle="Invitation not found">
        {(info) =>
          info.status === "COMPLETED" ? (
            <>
              <h1>Already onboarded</h1>
              <p>This invitation has already been used. Try logging in instead.</p>
            </>
          ) : (
            <>
              <h1>Welcome!</h1>
              <p>
                Complete onboarding for your <strong>{info.course.subject}</strong> ({info.course.yearRange})
                course, purchased by {info.parentEmail}.
              </p>

              <form onSubmit={handleSubmit} className="form">
                <label htmlFor="studentName">Your name</label>
                <input
                  id="studentName"
                  required
                  value={studentName}
                  onChange={(e) => setStudentName(e.target.value)}
                />

                <label htmlFor="email">Your email</label>
                <input
                  id="email"
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />

                <label htmlFor="password">Choose a password</label>
                <input
                  id="password"
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />

                {submitError && (
                  <p className="error" role="alert">
                    {submitError}
                  </p>
                )}

                <button type="submit" disabled={submitting}>
                  {submitting ? "Activating..." : "Activate my account"}
                </button>
              </form>
            </>
          )
        }
      </AsyncBoundary>
    </div>
  );
}
