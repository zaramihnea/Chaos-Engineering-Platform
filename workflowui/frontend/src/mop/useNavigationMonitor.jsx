import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export default function useNavigationMonitor() {
  const navigate = useNavigate();

  useEffect(() => {
    const token = sessionStorage.getItem("workflow-nav-token");

    if (!token) {
      console.warn(
        "MOP VIOLATION: Manual navigation to workflow page detected. Redirecting..."
      );
      navigate("/workflows");
      return;
    }

    console.log("MOP: Valid workflow navigation.");
    sessionStorage.removeItem("workflow-nav-token");
  }, [navigate]);
}
