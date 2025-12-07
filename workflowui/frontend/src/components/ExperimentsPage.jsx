import React, { useState, useEffect } from "react";
import withActionLogging from "../aop/withActionLogging";
import { useNavigate } from "react-router-dom";
import "../styles/ExperimentsPage.css";

export default function ExperimentsPage() {
  const navigate = useNavigate();

  const [experiments, setExperiments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [nameFilter, setNameFilter] = useState("");
  const [kindFilter, setKindFilter] = useState("");

  const loadExperiments = async () => {
    try {
      const response = await fetch("/api/experiments");

      if (!response.ok) {
        console.error("Failed to fetch experiments", response.status);
        return;
      }

      const data = await response.json();
      setExperiments(data);
    } catch (err) {
      console.error("Error fetching experiments:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadExperiments();
  }, []);

  const queryExperiments = withActionLogging(
    "Query Experiments",
    loadExperiments
  );

  async function deleteExperiment(id) {
    const confirmDelete = window.confirm(
      "Are you sure you want to delete this experiment?"
    );

    if (!confirmDelete) return;

    try {
      const res = await fetch(`/api/experiments/${id}`, {
        method: "DELETE",
      });

      const data = await res.json();

      if (!res.ok) {
        alert("❌ Error: " + data.error);
        return;
      }

      alert("Experiment deleted successfully!");

      setExperiments((prev) => prev.filter((exp) => exp.id !== id));
    } catch (err) {
      console.error("Delete failed:", err);
      alert("Failed to delete experiment.");
    }
  }

  if (loading) {
    return (
      <div className="exp-container">
        <h2>Loading experiments...</h2>
      </div>
    );
  }

  return (
    <div className="exp-container">
      <h1 className="exp-title">Experiments</h1>
      <p className="exp-description">
        The experiment list shows the experiments and statistics that the
        service has currently executed.
      </p>

      <div className="exp-content">
        <div className="exp-left">
          <div className="exp-filters">
            <input
              placeholder="Name"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
            />

            <select
              value={kindFilter}
              onChange={(e) => setKindFilter(e.target.value)}
            >
              <option value="">Kind</option>
              <option value="POD_KILL">POD_KILL</option>
              <option value="CPU_STRESS">CPU TEST</option>
              <option value="MEMORY_STRESS">MEMORY TEST</option>
              <option value="NETWORK_DELAY">NETWORK DELAY</option>
              <option value="NETWORK_PARTITION">NETWORK PARTITION</option>
            </select>

            <button className="query-btn" onClick={queryExperiments}>
              Query
            </button>

            <button
              className="new-exp-btn"
              onClick={() => navigate("/new-experiment")}
            >
              + New Experiment
            </button>
          </div>

          <table className="exp-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Fault Type</th>
                <th>Name</th>
                <th>Target Namespace</th>
                <th>SLO Count</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {experiments.map((exp, index) => (
                <tr key={exp.id}>
                  <td>{index + 1}</td>
                  <td>{exp.faultType}</td>
                  <td>{exp.name}</td>
                  <td>{exp.target?.namespace}</td>
                  <td>{exp.slos?.length || 0}</td>

                  <td>
                    <button
                      className="detail-btn"
                      onClick={() => {
                        sessionStorage.setItem("workflow-nav-token", "true");
                        navigate(`/experiment/${exp.id}`);
                      }}
                    >
                      Details
                    </button>

                    <button
                      className="delete-btn"
                      onClick={() => deleteExperiment(exp.id)}
                      style={{
                        background: "#b71c1c",
                        marginLeft: "10px",
                        color: "white",
                        padding: "6px 10px",
                        borderRadius: "6px",
                        cursor: "pointer",
                      }}
                    >
                      ✖
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="exp-right">
          <div className="exp-summary">
            <h3>Experiments Summary</h3>
            <div className="donut-chart">
              <div className="inner">{experiments.length}</div>
            </div>
            <p>Total Experiments: {experiments.length}</p>
          </div>

          <div className="exp-events">
            <h3>Events</h3>
            <ul>
              <li>
                <strong>System</strong> — Experiment list loaded
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

