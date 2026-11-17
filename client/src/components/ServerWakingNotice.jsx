import { useSyncExternalStore } from 'react';
import { isRequestSlow, subscribeToSlowRequests } from '../services/slowRequests';

export default function ServerWakingNotice() {
  const slow = useSyncExternalStore(subscribeToSlowRequests, isRequestSlow);

  // The status region stays in the page so that screen readers announce the message when it appears.
  return (
    <div role="status">
      {slow && (
        <p className="wake-notice">
          The server is starting up. It sleeps when idle on free hosting, so this can take 2 to 3 minutes.
        </p>
      )}
    </div>
  );
}