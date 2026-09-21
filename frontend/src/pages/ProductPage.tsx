import { useNavigate } from "react-router-dom";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { courseListSchema } from "../api/schemas";

export function ProductPage() {
  const coursesState = useApiResource("/courses", courseListSchema);
  const navigate = useNavigate();

  return (
    <div className="page">
      <h1>Choose a course</h1>
      <p className="subtitle">Pick a course to purchase access for your child.</p>

      <AsyncBoundary state={coursesState} loadingText="Loading courses...">
        {(courses) => (
          <div className="card-grid">
            {courses.map((course) => (
              <div className="card" key={course.id}>
                <h2>{course.subject}</h2>
                <p>{course.yearRange}</p>
                <p className="price">£{course.price.toFixed(2)}</p>
                <button onClick={() => navigate(`/checkout/${course.id}`)}>Buy access</button>
              </div>
            ))}
          </div>
        )}
      </AsyncBoundary>
    </div>
  );
}
