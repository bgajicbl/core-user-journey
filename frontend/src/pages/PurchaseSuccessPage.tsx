import { Link, useParams } from "react-router-dom";
import { AsyncBoundary } from "../components/AsyncBoundary";
import { useApiResource } from "../hooks/useApiResource";
import { onboardingInfoSchema } from "../api/schemas";

export function PurchaseSuccessPage() {
  const { token } = useParams<{ token: string }>();
  const infoState = useApiResource(token ? `/onboarding/${token}` : null, onboardingInfoSchema);

  const invitationUrl = `${window.location.origin}/onboarding/${token}`;

  return (
    <div className="page page-narrow">
      <h1>Purchase complete</h1>
      <AsyncBoundary state={infoState} loadingText="Confirming your purchase...">
        {(info) => <p>Access for {info.course.subject} has been purchased.</p>}
      </AsyncBoundary>
      <p>Share this invitation link with your student so they can onboard and access the LMS:</p>

      <div className="invite-box">
        <a href={invitationUrl}>{invitationUrl}</a>
      </div>

      <Link to={`/onboarding/${token}`} className="button-link">
        Continue to student onboarding
      </Link>
    </div>
  );
}
