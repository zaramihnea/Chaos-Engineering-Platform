import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/ExperimentsPage.css";

export default function WorkflowList() {
  const navigate = useNavigate();

  const [nameFilter, setNameFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  const workflows = [
    {
      id: 1,
      name: "Black Friday Outage Simulation",
      status: "Finished",
      createdAt: "2024/11/01 10:20:11",
    },
    {
      id: 2,
      name: "Network Latency Pipeline",
      status: "Paused",
      createdAt: "2024/10/28 09:55:03",
    },
    {
      id: 3,
      name: "Pod Failure Chain Reaction",
      status: "Running",
      createdAt: "2024/10/20 12:14:55",
    },
    {
      id: 4,
      name: "Endurance Chaos Workflow",
      status: "Finished",
      createdAt: "2024/10/19 11:05:22",
    },
  ];

  return (
    <div className="exp-container">
      <h1 className="exp-title">Workflows</h1>
      <p className="exp-description">
        The workflow list shows all multi-step chaos scenarios executed in the
        platform.
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
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">Status</option>
              <option value="Paused">Paused</option>
              <option value="Running">Running</option>
              <option value="Finished">Finished</option>
            </select>

            <button className="query-btn">Query</button>

            <button
              className="new-exp-btn"
              onClick={() => navigate("/workflow/create")}
            >
              + New Workflow
            </button>
          </div>

          <table className="exp-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Status</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {workflows.map((w) => (
                <tr key={w.id}>
                  <td>{w.id}</td>
                  <td>{w.name}</td>
                  <td>
                    <span
                      className={
                        w.status === "Running"
                          ? "running-badge"
                          : w.status === "Finished"
                          ? "finished-badge"
                          : "paused-badge"
                      }
                    >
                      {w.status}
                    </span>
                  </td>
                  <td>{w.createdAt}</td>
                  <td>
                    <button
                      onClick={() => {
                        // mark navigation as intentional
                        sessionStorage.setItem("workflow-nav-token", "true");

                        // store the workflow ID (optional)
                        sessionStorage.setItem("workflowId", w.id);

                        navigate(`/workflow/${w.id}`);
                      }}
                    >
                      Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="exp-right">
          <div className="exp-summary">
            <h3>Workflow Summary</h3>
            <div className="donut-chart">
              <div className="inner">11</div>
            </div>
            <p>Finished: 6 • Running: 3 • Paused: 2</p>
          </div>

          <div className="exp-events">
            <h3>Events</h3>
            <ul>
              <li>
                <strong>Black Friday Outage Simulation</strong> — Completed
                successfully
              </li>
              <li>
                <strong>Network Latency Pipeline</strong> — Workflow paused
              </li>
              <li>
                <strong>Pod Failure Chain Reaction</strong> — Step 2 triggered
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
