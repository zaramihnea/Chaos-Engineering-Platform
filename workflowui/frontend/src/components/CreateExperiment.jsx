import React, { useState } from "react";
import useFormMonitor from "../mop/useFormMonitor";
import "../styles/CreateExperiment.css";

export default function CreateExperiment() {
  const [experimentalName, setExperimentName] = useState("");
  const [faultType, setFaultType] = useState(null);
  const [parameters, setParameters] = useState([]);
  const [targetPod, setTargetPod] = useState(null);
  const [timeout, setTimeout] = useState("");
  const [slos, setSlos] = useState([]);
  const [newSlo, setNewSlo] = useState("");
  const [dryRunAllowed, setDryRunAllowed] = useState(false);

  useFormMonitor(
  {
    experimentalName,
    faultType,
    targetPod
  },
  {
    experimentalName: {
      check: (v) => v.length > 0,
      message: "Experiment must have a name"
    },
    faultType: {
      check: (v) => v !== null,
      message: "Fault type must be selected"
    },
    targetPod: {
      check: (v) => v !== null,
      message: "At least one target pod must be selected"
    }
  }
);


  const targets = [
    { id: "POD_FAULT", label: "POD FAULT" },
    { id: "NETWORK_ATTACK", label: "NETWORK ATTACK" },
    { id: "IO_INJECTION", label: "IO INJECTION" },
    { id: "STRESS_TEST", label: "STRESS TEST" },
  ];

  const podData = [
    {
      name: "ws-ek8djp9sid9seu-5d9458f6d6-lclsb",
      namespace: "ns-90f142599393456ba881cad8580af4e",
      ip: "10.60.125.171",
      status: "Running",
    },
  ];

  const [paramKey, setParamKey] = useState("");
  const [paramValue, setParamValue] = useState("");

  const addParameter = () => {
    if (!paramKey || !paramValue) return;
    setParameters([...parameters, { key: paramKey, value: paramValue }]);
    setParamKey("");
    setParamValue("");
  };

  const addSlo = () => {
    if (!newSlo.trim()) return;
    setSlos([...slos, newSlo]);
    setNewSlo("");
  };

  const submitExperiment = () => {
    const experiment = {
      name: experimentalName,
      faultType: faultType,
      parameters: Object.fromEntries(parameters.map((p) => [p.key, p.value])),
      target: targetPod,
      timeout,
      slos,
      dryRunAllowed,
    };

    console.log("Experiment sent:", experiment);
  };

  const saveExperiment = () => {
    const experiment = {
      name: experimentalName,
      faultType: faultType,
      parameters: Object.fromEntries(parameters.map((p) => [p.key, p.value])),
      target: targetPod,
      timeout,
      slos,
      dryRunAllowed,
    };

    console.log("Experiment saved:", experiment);
  };

  return (
    <div className="exp-create-container">
      <div className="top-nav">
        <a className="back-link" href="/experiments">
          ← Back to Experiments
        </a>
        <span className="divider">|</span>
        <span className="back-link">New Experiment</span>
      </div>
      <section>
        <h2>Fault Type</h2>
        <div className="target-grid">
          {targets.map((t) => (
            <div
              key={t.id}
              className={`target-card ${faultType === t.id ? "selected" : ""}`}
              onClick={() => setFaultType(t.id)}
            >
              {t.label}
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2>Select Target Pod</h2>

        <table className="pods-table">
          <thead>
            <tr>
              <th></th>
              <th>Name</th>
              <th>Namespace</th>
              <th>IP Address</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>
            {podData.map((pod, idx) => (
              <tr key={idx}>
                <td>
                  <input
                    type="checkbox"
                    checked={targetPod?.name === pod.name}
                    onChange={() => setTargetPod(pod)}
                  />
                </td>
                <td>{pod.name}</td>
                <td>{pod.namespace}</td>
                <td>{pod.ip}</td>
                <td>
                  <span className="status-running">Running</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>Experiment Info</h2>

        <label>Name</label>
        <input
          value={experimentalName}
          onChange={(e) => setExperimentName(e.target.value)}
          placeholder="Enter experiment name"
        />

        <label>Timeout (e.g. 10s, 2m, 1h)</label>
        <input
          value={timeout}
          onChange={(e) => setTimeout(e.target.value)}
          placeholder="Duration"
        />

        <div className="row-switch">
          <label>Dry Run Allowed</label>
          <input
            type="checkbox"
            checked={dryRunAllowed}
            onChange={() => setDryRunAllowed(!dryRunAllowed)}
          />
        </div>

        <h3>Parameters</h3>
        <div className="param-entry-row">
          <input
            placeholder="Key"
            value={paramKey}
            onChange={(e) => setParamKey(e.target.value)}
          />
          <input
            placeholder="Value"
            value={paramValue}
            onChange={(e) => setParamValue(e.target.value)}
          />
          <button onClick={addParameter}>Add</button>
        </div>

        <div className="param-list">
          {parameters.map((p, i) => (
            <div key={i} className="param-card">
              <span>
                <strong>• {p.key}</strong>: {p.value}
              </span>
              <button
                className="remove-param-btn"
                onClick={() =>
                  setParameters(parameters.filter((_, idx) => idx !== i))
                }
              >
                ✖
              </button>
            </div>
          ))}
        </div>

        <h3>SLO Targets</h3>
        <div className="slo-entry-row">
          <input
            placeholder="Define an SLO (e.g., Latency < 200ms)"
            value={newSlo}
            onChange={(e) => setNewSlo(e.target.value)}
          />
          <button onClick={addSlo}>Add</button>
        </div>

        <div className="slo-list">
          {slos.map((s, i) => (
            <div key={i} className="slo-card">
              <span> • {s}</span>
              <button
                className="remove-slo-btn"
                onClick={() => setSlos(slos.filter((_, idx) => idx !== i))}
              >
                ✖
              </button>
            </div>
          ))}
        </div>
      </section>

      <div className="buttons-row">
        <button className="submit-btn" onClick={submitExperiment}>
          Submit Experiment
        </button>
        <button className="submit-btn" onClick={saveExperiment}>
          Save Experiment
        </button>
      </div>
    </div>
  );
}
