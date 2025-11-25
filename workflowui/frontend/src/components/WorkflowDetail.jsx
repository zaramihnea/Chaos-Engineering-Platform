import React from "react";
import { useParams } from "react-router-dom";
import useNavigationMonitor from "../mop/useNavigationMonitor";
import "../styles/ExperimentsPage.css";

export default function WorkflowDetail() {
  const { id } = useParams();

  useNavigationMonitor();

  const workflow = {
    id,
    name: "Black Friday Outage Simulation",
    status: "Finished",
    createdAt: "2024/10/29 12:35:11",
    steps: [
      {
        id: 1,
        name: "CPU Stress",
        status: "Finished",
        startedAt: "12:35:11",
        endedAt: "12:36:00",
        parameters: "80% for 60s",
      },
      {
        id: 2,
        name: "Network Delay",
        status: "Finished",
        startedAt: "12:36:00",
        endedAt: "12:37:30",
        parameters: "400ms delay",
      },
      {
        id: 3,
        name: "Pod Delete",
        status: "Finished",
        startedAt: "12:37:30",
        endedAt: "12:37:32",
        parameters: "target=frontend-pod",
      },
      {
        id: 4,
        name: "Memory Hog",
        status: "Finished",
        startedAt: "12:37:32",
        endedAt: "12:38:17",
        parameters: "90% RAM for 45s",
      },
    ],
    events: [
      "Workflow started",
      "Step 1: CPU Stress executed",
      "Step 2: Network Delay executed",
      "Step 3: Pod Delete executed",
      "Step 4: Memory Hog executed",
      "Workflow completed successfully",
    ],
  };

  return (
    <div className="exp-container">
      <div className="top-nav">
        <a className="back-link" href="/workflows">
          ← Back to Workflows
        </a>
        <span className="divider">|</span>
        <span className="back-link">{workflow.name}</span>
      </div>

      <h1 className="exp-title">Workflow Details</h1>
      <p className="exp-description">
        Detailed execution history for workflow <strong>{workflow.name}</strong>
        .
      </p>

      <div className="exp-content">
        <div className="exp-left">
          <div className="workflow-meta">
            <h3>Workflow Overview</h3>
            <p>
              <strong>Name:</strong> {workflow.name}
            </p>
            <p>
              <strong>Status:</strong> {workflow.status}
            </p>
            <p>
              <strong>Created At:</strong> {workflow.createdAt}
            </p>
            <p>
              <strong>Steps:</strong> {workflow.steps.length}
            </p>
          </div>

          <h3 style={{ marginTop: "20px" }}>Workflow Steps</h3>

          <table className="exp-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Step Name</th>
                <th>Status</th>
                <th>Parameters</th>
                <th>Start</th>
                <th>End</th>
              </tr>
            </thead>
            <tbody>
              {workflow.steps.map((step) => (
                <tr key={step.id}>
                  <td>{step.id}</td>
                  <td>{step.name}</td>
                  <td>
                    <span className="finished-badge">{step.status}</span>
                  </td>
                  <td>{step.parameters}</td>
                  <td>{step.startedAt}</td>
                  <td>{step.endedAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="exp-right">
          <div className="exp-summary">
            <h3>Workflow Summary</h3>
            <div className="donut-chart">
              <div className="inner">{workflow.steps.length}</div>
            </div>
            <p>All steps completed successfully.</p>
          </div>

          <div className="exp-events">
            <h3>Execution Timeline</h3>
            <ul>
              {workflow.events.map((ev, idx) => (
                <li key={idx}>{ev}</li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
