import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { AuthResponse, OnboardingInfo } from "../api/types";

export function OnboardingPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { login } = useAuth();

  const [info, setInfo] = useState<OnboardingInfo | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [studentName, setStudentName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    api
      .get<OnboardingInfo>(`/onboarding/${token}`)
      .then(setInfo)
      .catch((err: Error) => setLoadError(err.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const auth = await api.post<AuthResponse>(`/onboarding/${token}`, {
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

  if (loadError) {
    return (
      <div className="page page-narrow">
        <h1>Invitation not found</h1>
        <p className="error">{loadError}</p>
      </div>
    );
  }

  if (!info) {
    return (
      <div className="page page-narrow">
        <p>Loading invitation...</p>
      </div>
    );
  }

  if (info.status === "COMPLETED") {
    return (
      <div className="page page-narrow">
        <h1>Already onboarded</h1>
        <p>This invitation has already been used. Try logging in instead.</p>
      </div>
    );
  }

  return (
    <div className="page page-narrow">
      <h1>Welcome!</h1>
      <p>
        Complete onboarding for your <strong>{info.course.subject}</strong> ({info.course.yearRange}) course,
        purchased by {info.parentEmail}.
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
        <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />

        <label htmlFor="password">Choose a password</label>
        <input
          id="password"
          type="password"
          required
          minLength={6}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {submitError && <p className="error">{submitError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? "Activating..." : "Activate my account"}
        </button>
      </form>
    </div>
  );
}
