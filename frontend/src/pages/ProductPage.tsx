import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import type { Course } from "../api/types";

export function ProductPage() {
  const [courses, setCourses] = useState<Course[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get<Course[]>("/courses")
      .then(setCourses)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <div className="page">
      <h1>Choose a course</h1>
      <p className="subtitle">Pick a course to purchase access for your child.</p>

      {error && <p className="error">{error}</p>}
      {!courses && !error && <p>Loading courses...</p>}

      <div className="card-grid">
        {courses?.map((course) => (
          <div className="card" key={course.id}>
            <h2>{course.subject}</h2>
            <p>{course.yearRange}</p>
            <p className="price">£{course.price.toFixed(2)}</p>
            <button onClick={() => navigate(`/checkout/${course.id}`)}>Buy access</button>
          </div>
        ))}
      </div>
    </div>
  );
}
