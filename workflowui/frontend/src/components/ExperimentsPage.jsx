import React, { useState } from "react";
import withLogging from "../aop/withLogging";
import withActionLogging from "../aop/withActionLogging";
import "../styles/ExperimentsPage.css";

function ExperimentsPage() {
  const [nameFilter, setNameFilter] = useState("");
  const [kindFilter, setKindFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  const experiments = [
    {
      id: 1,
      kind: "STRESS TEST",
      name: "memory-load-4workers-80p",
      status: "Paused",
      createdAt: "2021/10/29 16:02:55",
    },
    {
      id: 2,
      kind: "STRESS TEST",
      name: "cpu-load-4workers-80p",
      status: "Paused",
      createdAt: "2021/10/29 15:52:15",
    },
    {
      id: 3,
      kind: "STRESS TEST",
      name: "cpu-load-4workers",
      status: "Paused",
      createdAt: "2021/10/29 15:50:38",
    },
    {
      id: 4,
      kind: "NETWORK ATTACK",
      name: "network-bandwidth",
      status: "Paused",
      createdAt: "2021/10/29 14:53:37",
    },
    {
      id: 5,
      kind: "NETWORK ATTACK",
      name: "network-corrupt",
      status: "Paused",
      createdAt: "2021/10/29 14:47:45",
    },
  ];

  const queryExperiments = withActionLogging("Query Experiments", () => {
    console.log("Real Query Logic Running...");
  });

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
              <option value="STRESS TEST">STRESS TEST</option>
              <option value="NETWORK ATTACK">NETWORK ATTACK</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">Status</option>
              <option value="Paused">Paused</option>
              <option value="Running">Running</option>
              <option value="Finished">Finished</option>
            </select>

            <button className="query-btn" onClick={queryExperiments}>
              Query
            </button>
            <button className="new-exp-btn">+ New Experiment</button>
          </div>

          <table className="exp-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Kind</th>
                <th>Name</th>
                <th>Status</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {experiments.map((exp) => (
                <tr key={exp.id}>
                  <td>{exp.id}</td>
                  <td>{exp.kind}</td>
                  <td>{exp.name}</td>
                  <td>
                    <span className="paused-badge">|| Paused</span>
                  </td>
                  <td>{exp.createdAt}</td>
                  <td>
                    <button className="detail-btn">Detail</button>
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
              <div className="inner">19</div>
            </div>
            <p>Finished: 7 • Paused: 12</p>
          </div>

          <div className="exp-events">
            <h3>Events</h3>
            <ul>
              <li>
                <strong>memory-load-4workers-80p</strong> — Successfully updated
                phase
              </li>
              <li>
                <strong>network-bandwidth</strong> — Experiment paused
              </li>
              <li>
                <strong>cpu-load-4workers</strong> — Successfully recovered
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

const ExperimentsPageWithLogging = withLogging(
  ExperimentsPage,
  "ExperimentsPage"
);

export default ExperimentsPageWithLogging;
