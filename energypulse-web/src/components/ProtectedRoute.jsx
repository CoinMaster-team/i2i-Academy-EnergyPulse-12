import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import {
  AUTH_EXPIRED_EVENT,
  clearAuthSession,
  getAuthToken,
  getCurrentUser,
  saveStoredUser,
} from "../services/authService";

function ProtectedRoute({ children }) {
  const [sessionStatus, setSessionStatus] = useState(
    getAuthToken() ? "checking" : "anonymous"
  );

  useEffect(() => {
    let active = true;

    const expireSession = () => {
      clearAuthSession();
      if (active) {
        setSessionStatus("anonymous");
      }
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, expireSession);

    if (getAuthToken()) {
      getCurrentUser()
        .then((user) => {
          if (active) {
            saveStoredUser(user);
            setSessionStatus("authenticated");
          }
        })
        .catch(expireSession);
    }

    return () => {
      active = false;
      window.removeEventListener(AUTH_EXPIRED_EVENT, expireSession);
    };
  }, []);

  if (sessionStatus === "checking") {
    return (
      <main className="route-loading" aria-live="polite">
        Checking your session...
      </main>
    );
  }

  if (sessionStatus !== "authenticated") {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default ProtectedRoute;
