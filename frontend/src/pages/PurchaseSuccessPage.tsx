import { Link, useLocation, useParams } from "react-router-dom";
import type { CheckoutResponse } from "../api/types";

export function PurchaseSuccessPage() {
  const { token } = useParams<{ token: string }>();
  const location = useLocation();
  const result = location.state as CheckoutResponse | undefined;

  const invitationUrl = `${window.location.origin}/onboarding/${token}`;

  return (
    <div className="page page-narrow">
      <h1>Purchase complete</h1>
      <p>
        {result
          ? `Access for ${result.course.subject} has been purchased.`
          : "Your purchase was completed."}
      </p>
      <p>Share this invitation link with your student so they can onboard and access the LMS:</p>

      <div className="invite-box">
        <a href={invitationUrl}>{invitationUrl}</a>
      </div>

      <Link to={invitationUrl.replace(window.location.origin, "")} className="button-link">
        Continue to student onboarding
      </Link>
    </div>
  );
}
