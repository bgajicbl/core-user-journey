import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { dashboardSchema } from "../api/schemas";

export function LmsDashboardPage() {
  const { token } = useAuth();
  const dashboardState = useApiResource("/lms/dashboard", dashboardSchema, token);

  return (
    <div className="page">
      <AsyncBoundary state={dashboardState} loadingText="Loading dashboard...">
        {(dashboard) => (
          <>
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
          </>
        )}
      </AsyncBoundary>
    </div>
  );
}
