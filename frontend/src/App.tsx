import { Navigate, Route, Routes } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ProductPage } from "./pages/ProductPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { PurchaseSuccessPage } from "./pages/PurchaseSuccessPage";
import { OnboardingPage } from "./pages/OnboardingPage";
import { LoginPage } from "./pages/LoginPage";
import { LmsDashboardPage } from "./pages/LmsDashboardPage";
import { LessonPage } from "./pages/LessonPage";

function App() {
  return (
    <>
      <NavBar />
      <main>
        <ErrorBoundary>
          <Routes>
            <Route path="/" element={<ProductPage />} />
            <Route path="/checkout/:courseId" element={<CheckoutPage />} />
            <Route path="/checkout/success/:token" element={<PurchaseSuccessPage />} />
            <Route path="/onboarding/:token" element={<OnboardingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/lms"
              element={
                <ProtectedRoute>
                  <LmsDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/lms/lessons/:lessonId"
              element={
                <ProtectedRoute>
                  <LessonPage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ErrorBoundary>
      </main>
    </>
  );
}

export default App;
