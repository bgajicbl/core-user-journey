import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { LessonDetail } from "../api/types";

export function LessonPage() {
  const { lessonId } = useParams<{ lessonId: string }>();
  const { token } = useAuth();
  const [lesson, setLesson] = useState<LessonDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!lessonId) return;
    api
      .get<LessonDetail>(`/lms/lessons/${lessonId}`, token)
      .then(setLesson)
      .catch((err: Error) => setError(err.message));
  }, [lessonId, token]);

  if (error) {
    return (
      <div className="page">
        <p className="error">{error}</p>
        <Link to="/lms">Back to dashboard</Link>
      </div>
    );
  }

  if (!lesson) {
    return (
      <div className="page">
        <p>Loading lesson...</p>
      </div>
    );
  }

  return (
    <div className="page page-narrow">
      <Link to="/lms" className="back-link">
        ← Back to dashboard
      </Link>
      <h1>{lesson.title}</h1>
      <p>{lesson.content}</p>
    </div>
  );
}
