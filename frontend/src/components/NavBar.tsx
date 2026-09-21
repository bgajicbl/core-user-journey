import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function NavBar() {
  const { token, studentName, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">
        MyEdSpace
      </Link>
      <nav className="navbar-links">
        {token ? (
          <>
            <Link to="/lms">Dashboard</Link>
            <span className="navbar-user">Hi, {studentName}</span>
            <button
              className="link-button"
              onClick={() => {
                logout();
                navigate("/");
              }}
            >
              Log out
            </button>
          </>
        ) : (
          <Link to="/login">Student login</Link>
        )}
      </nav>
    </header>
  );
}
