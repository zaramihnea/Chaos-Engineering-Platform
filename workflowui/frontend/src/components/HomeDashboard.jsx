import React from "react";
import { useNavigate } from "react-router-dom";
import "../styles/HomeDashboard.css";

export default function HomeDashboard() {
  const navigate = useNavigate();

  return (
    <div className="home-wrapper">
      <div className="home-container">
        <h1 className="home-title">Chaos Engineering Platform</h1>
        <p className="home-subtitle">
          Welcome! This is the overview of your activity on this platform:
        </p>

        <div className="home-grid">
          <div className="home-card">
            <h2>Experiments</h2>
            <button onClick={() => navigate("/experiments")}>View →</button>
          </div>
          <div className="home-card">
            <h2>Workflows</h2>
            <button onClick={() => navigate("/workflows")}>View →</button>
          </div>
        </div>
      </div>
    </div>
  );
}
