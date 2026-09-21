import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { Dashboard } from "../api/types";

export function LmsDashboardPage() {
  const { token } = useAuth();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Dashboard>("/lms/dashboard", token)
      .then(setDashboard)
      .catch((err: Error) => setError(err instanceof ApiError ? err.message : err.message));
  }, [token]);

  if (error) {
    return (
      <div className="page">
        <p className="error">{error}</p>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="page">
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Welcome back, {dashboard.studentName}</h1>
      <p className="subtitle">
        {dashboard.course.subject} · {dashboard.course.yearRange}
      </p>

      <h2>Lessons</h2>
      <ul className="lesson-list">
        {dashboard.lessons.map((lesson) => (
          <li key={lesson.id}>
            <Link to={`/lms/lessons/${lesson.id}`} className="lesson-item">
              <span className="lesson-index">{lesson.orderIndex + 1}</span>
              <span>{lesson.title}</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
