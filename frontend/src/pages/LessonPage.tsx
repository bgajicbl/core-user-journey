import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { lessonDetailSchema } from "../api/schemas";

export function LessonPage() {
  const { lessonId } = useParams<{ lessonId: string }>();
  const { token } = useAuth();
  const lessonState = useApiResource(lessonId ? `/lms/lessons/${lessonId}` : null, lessonDetailSchema, token);

  return (
    <div className="page page-narrow">
      <Link to="/lms" className="back-link">
        ← Back to dashboard
      </Link>
      <AsyncBoundary state={lessonState} loadingText="Loading lesson...">
        {(lesson) => (
          <>
            <h1>{lesson.title}</h1>
            <p>{lesson.content}</p>
          </>
        )}
      </AsyncBoundary>
    </div>
  );
}
