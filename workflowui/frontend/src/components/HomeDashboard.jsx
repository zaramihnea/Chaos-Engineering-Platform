import React from "react";
import { useNavigate } from "react-router-dom";
import "../styles/HomeDashboard.css";

export default function HomeDashboard() {
  const navigate = useNavigate();

  return (
    <div className="home-wrapper">
      {/* Background decoration for 'Chaos' feel */}
      <div className="chaos-grid-bg"></div>
      <div className="fault-line"></div>
      
      <div className="home-container">
        <h1 className="home-title" data-text="Chaos Engineering Platform">
          Chaos Engineering Platform
        </h1>
        <p className="home-subtitle">
          Welcome! Ready for some Experiments?
        </p>

        <div className="home-grid">
          <div className="home-card">
            <div className="card-pulse"></div>
            <h2>Experiments</h2>
            <button className="glow-button" onClick={() => navigate("/experiments")}>
              View →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}