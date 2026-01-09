import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import useNavigationMonitor from "../mop/useNavigationMonitor";
import "../styles/ExperimentsPage.css";

export default function ExperimentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [experiment, setExperiment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [runs, setRuns] = useState([]);
  const [showScheduler, setShowScheduler] = useState(false);
  const [scheduleTime, setScheduleTime] = useState("");
  const [scheduleDryRun, setScheduleDryRun] = useState(false);
  useNavigationMonitor();

  useEffect(() => {
    async function fetchData() {
      try {
        const expRes = await fetch(`/api/experiments`);
        const expList = await expRes.json();
        const found = expList.find((exp) => exp.id === id);
        setExperiment(found || null);

        const runRes = await fetch(`/api/experiments/${id}/runs`);
        const runList = await runRes.json();
        setRuns(runList);
        console.log(runs);
      } catch (err) {
        console.error("Error loading experiment:", err);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [id,runs]);


  async function deleteRun(runId) {
    const confirmDelete = window.confirm(
      `Are you sure you want to delete run ${runId}?`
    );

    if (!confirmDelete) return;

    try {
      const res = await fetch(`/api/runs/${runId}`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason: "User deleted run" }),
      });

      const data = await res.json();

      if (!res.ok) {
        alert("❌ Failed to delete run: " + data.error);
        return;
      }

      alert("Run deleted successfully!");

      setRuns((prev) => prev.filter((r) => r.runId !== runId));
    } catch (err) {
      console.error("Delete run failed:", err);
    }
  }

  if (loading) {
    return (
      <div className="exp-container">
        <h2>Loading experiment...</h2>
      </div>
    );
  }

  if (!experiment) {
    return (
      <div className="exp-container">
        <h2>Experiment Not Found</h2>
        <button onClick={() => navigate("/experiments")} className="detail-btn">
          ← Back to Experiments
        </button>
      </div>
    );
  }

  async function scheduleRun() {
    if (!scheduleTime) {
      alert("Please select a valid time.");
      return;
    }

    const body = {
      when: new Date(scheduleTime).toISOString(),
      dryRun: scheduleDryRun,
    };

    try {
      const res = await fetch(`/api/experiments/${id}/runs`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        const error = await res.json();
        alert("Failed to schedule run: " + error.error);
        return;
      }

      alert("Run scheduled successfully!");
      setShowScheduler(false);
    } catch (err) {
      console.error("Schedule error:", err);
    }
  }

  return (
    <div className="exp-container">
      <div className="top-nav">
        <a className="back-link" href="/experiments">
          ← Back to Experiments
        </a>
        <span className="divider">|</span>
        <span className="back-link">{experiment.name}</span>
      </div>

      <h1 className="exp-title">Experiment Details</h1>
      <p className="exp-description">
        Full configuration for experiment <strong>{experiment.name}</strong>.
      </p>

      <div className="exp-content">
        <div className="exp-left">
          <div className="detail-card">
            <div className="details-exp">
              <div className="section-one">
                <h3>Experiment Overview</h3>

                <p>
                  <strong>ID:</strong> {experiment.id}
                </p>
                <p>
                  <strong>Name:</strong> {experiment.name}
                </p>
                <p>
                  <strong>Fault Type:</strong> {experiment.faultType}
                </p>
                <p>
                  <strong>Dry Run Allowed:</strong>{" "}
                  {experiment.dryRunAllowed ? "Yes" : "No"}
                </p>
                <p>
                  <strong>Created By:</strong> {experiment.createdBy}
                </p>
                <p>
                  <strong>SLO Count:</strong> {experiment.slos?.length || 0}
                </p>
              </div>

              <div className="section-two">
                <h3>Target System</h3>

                {experiment.target ? (
                  <>
                    <p>
                      <strong>Cluster:</strong> {experiment.target.cluster}
                    </p>
                    <p>
                      <strong>Namespace:</strong> {experiment.target.namespace}
                    </p>

                    <p>
                      <strong>Labels:</strong>
                    </p>

                    {experiment.target.labels &&
                      Object.entries(experiment.target.labels).map(([k, v]) => (
                        <div key={k} className="label-item">
                          <span>
                            • {k}: {v}
                          </span>
                        </div>
                      ))}
                  </>
                ) : (
                  <p className="empty-text">No target system defined.</p>
                )}
              </div>
            </div>
          </div>

          <div className="detail-card">
            <h3>Parameters</h3>
            {Object.keys(experiment.parameters || {}).length === 0 ? (
              <p className="empty-text">No parameters defined.</p>
            ) : (
              <table className="exp-table small-table">
                <thead>
                  <tr>
                    <th>Key</th>
                    <th>Value</th>
                  </tr>
                </thead>

                <tbody>
                  {Object.entries(experiment.parameters).map(([k, v], idx) => (
                    <tr key={idx}>
                      <td>{k}</td>
                      <td>{JSON.stringify(v)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="detail-card">
            <h3>SLO Targets</h3>
            {experiment.slos?.length ? (
              <table className="exp-table small-table">
                <thead>
                  <tr>
                    <th>Metric</th>
                    <th>Comparator</th>
                    <th>Threshold</th>
                    <th>Prom Query</th>
                  </tr>
                </thead>

                <tbody>
                  {experiment.slos.map((s, i) => (
                    <tr key={i}>
                      <td>{s.metric}</td>
                      <td>{s.comparator}</td>
                      <td>{s.threshold}</td>
                      <td>{s.promQuery}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="empty-text">No SLOs defined.</p>
            )}
          </div>
          <div className="detail-card">
            <h3>Scheduled Runs</h3>

            {runs.length === 0 ? (
              <p className="empty-text">No runs scheduled yet.</p>
            ) : (
              <table className="exp-table small-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Run ID</th>
                    <th>Scheduled At</th>
                    <th>Dry Run</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {runs.map((run, i) => (
                    <tr key={run.runId}>
                      <td>{i + 1}</td>
                      <td>{run.runId}</td>
                      <td>{new Date(run.scheduledAt).toLocaleString()}</td>
                      <td>{run.dryRun ? "Yes" : "No"}</td>
                      <td>{run.status || "SCHEDULED"}</td>
                      <td>
                        <button
                          className="delete-btn"
                          style={{
                            background: "#a30000",
                            color: "white",
                            padding: "4px 8px",
                            borderRadius: "5px",
                          }}
                          onClick={() => deleteRun(run.runId)}
                        >
                          ✖
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div className="exp-right">
          <div className="detail-card summary-card">
            <h3>Experiment Summary</h3>

            <div className="donut-chart">
              <div className="inner">{experiment.slos?.length || 0}</div>
            </div>

            <p>{experiment.slos?.length || 0} SLOs configured.</p>
          </div>

          <div className="detail-card">
            <h3>Schedule a Run</h3>

            {!showScheduler && (
              <button
                className="schedule-btn"
                onClick={() => setShowScheduler(true)}
              >
                ▶ Schedule Experiment Run
              </button>
            )}

            {showScheduler && (
              <div className="scheduler-panel">
                <label>When to run:</label>
                <input
                  type="datetime-local"
                  value={scheduleTime}
                  onChange={(e) => setScheduleTime(e.target.value)}
                />

                <label className="checkbox-row">
                  Dry Run Mode
                  <input
                    type="checkbox"
                    checked={scheduleDryRun}
                    onChange={(e) => setScheduleDryRun(e.target.checked)}
                  />
                </label>

                <div className="scheduler-buttons">
                  <button className="confirm-btn" onClick={scheduleRun}>
                    Schedule
                  </button>
                  <button
                    className="cancel-btn"
                    onClick={() => setShowScheduler(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>


          <div className="detail-card">
            <h3>Download Reports</h3>
            <p style={{ color: "#bbb", marginTop: "-5px" }}>
              Export experiment reports in multiple formats.
            </p>

            <div className="report-buttons">
              <button
                className="report-btn pdf"
                onClick={() =>
                  alert("PDF report download triggered (placeholder).")
                }
              >
                📄 Download PDF
              </button>

              <button
                className="report-btn csv"
                onClick={() =>
                  alert("CSV report download triggered (placeholder).")
                }
              >
                📊 Download CSV
              </button>

              <button
                className="report-btn json"
                onClick={() => {
                  const data = JSON.stringify(experiment, null, 2);
                  const blob = new Blob([data], { type: "application/json" });
                  const url = URL.createObjectURL(blob);

                  const a = document.createElement("a");
                  a.href = url;
                  a.download = `${experiment.name}-report.json`;
                  a.click();

                  URL.revokeObjectURL(url);
                }}
              >
                🧾 Download JSON
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
