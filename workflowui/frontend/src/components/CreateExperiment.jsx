import React, { useState } from "react";
import useFormMonitor from "../mop/useFormMonitor";
import "../styles/CreateExperiment.css";

export default function CreateExperiment() {
  const [experimentalName, setExperimentName] = useState("");
  const [faultType, setFaultType] = useState(null);
  const [dryRunAllowed, setDryRunAllowed] = useState(false);

  const [cluster, setCluster] = useState("");
  const [namespace, setNamespace] = useState("");
  const [labels, setLabels] = useState({});
  const [labelKey, setLabelKey] = useState("");
  const [labelValue, setLabelValue] = useState("");

  const [timeoutSeconds, setTimeoutSeconds] = useState("");

  const [paramKey, setParamKey] = useState("");
  const [paramValue, setParamValue] = useState("");
  const [parameters, setParameters] = useState([]);

  const [sloMetric, setSloMetric] = useState("LATENCY_P95");
  const [sloComparator, setSloComparator] = useState("<");
  const [sloThreshold, setSloThreshold] = useState("");
  const [sloPromQuery, setSloPromQuery] = useState("");
  const [slos, setSlos] = useState([]);
  const [statusMessage, setStatusMessage] = useState({ text: "", type: "" });

  useFormMonitor(
    { experimentalName, faultType, cluster, namespace },
    {
      experimentalName: { check: (v) => v.length > 0, message: "Experiment must have a name" },
      faultType: { check: (v) => v !== null, message: "Fault type must be selected" },
      cluster: { check: (v) => v.length > 0, message: "Cluster field cannot be empty" },
      namespace: { check: (v) => v.length > 0, message: "Namespace cannot be empty" },
    }
  );

  const targets = ["POD_KILL", "CPU_STRESS", "MEMORY_STRESS", "NETWORK_DELAY", "NETWORK_PARTITION"];

  // --- Handlers pentru Adăugare ---
  const addParameter = () => {
    if (!paramKey || !paramValue) return;
    setParameters([...parameters, { key: paramKey, value: paramValue }]);
    setParamKey("");
    setParamValue("");
  };

  const addLabel = () => {
    if (!labelKey || !labelValue) return;
    setLabels({ ...labels, [labelKey]: labelValue });
    setLabelKey("");
    setLabelValue("");
  };

  const addSlo = () => {
    if (!sloThreshold || !sloPromQuery) return;
    const newSlo = {
      id: crypto.randomUUID(),
      metric: sloMetric,
      comparator: sloComparator,
      threshold: Number(sloThreshold),
      promQuery: sloPromQuery,
    };
    console.log(sloComparator)
    setSlos([...slos, newSlo]);
    setSloThreshold("");
    setSloPromQuery("");
  };

  const removeLabel = (keyToRemove) => {
    const newLabels = { ...labels };
    delete newLabels[keyToRemove];
    setLabels(newLabels);
  };

  const removeParameter = (index) => {
    setParameters(parameters.filter((_, i) => i !== index));
  };

  const removeSlo = (id) => {
    setSlos(slos.filter((slo) => slo.id !== id));
  };

const buildApiPayload = () => {
  // Ensure we have a valid number
  const seconds = parseInt(timeoutSeconds, 10) || 30;

  return {
    id: crypto.randomUUID(),
    name: experimentalName,
    faultType: faultType,
    parameters: Object.fromEntries(parameters.map((p) => [p.key, p.value])),
    target: { 
      cluster: cluster, 
      namespace: namespace, 
      labels: labels 
    },
    // Send a clean ISO-8601 String
    timeout: `PT${seconds}S`, 
    slos: slos.map(slo => ({
      metric: slo.metric,
      promQuery: slo.promQuery,
      threshold: Number(slo.threshold),
      comparator: slo.comparator
    })),
    dryRunAllowed: dryRunAllowed,
    createdBy: "ui-user",
  };
};

  const showStatus = (text, type) => {
    setStatusMessage({ text, type });
    setTimeout(() => setStatusMessage({ text: "", type: "" }), 5000);
  };

  const submitExperiment = async () => {
    if (!experimentalName || !faultType) {
        showStatus("Please fill in the required fields (Name and Fault Type).", "error");
        return;
    }

    const payload = buildApiPayload();
    try {
      console.log(payload)
      const response = await fetch("/api/experiments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        showStatus("Experiment created successfully!", "success");
      } else {
        const errorData = await response.json().catch(() => ({}));
        showStatus(`Failed to submit: ${errorData.message || 'Server error'}`, "error");
      }
    } catch (err) {
      console.error(err);
      showStatus("Connection error. Could not reach the server.", "error");
    }
  };

  return (
    <div className="exp-create-container">
      <div className="top-nav">
        <a className="back-link" href="/experiments">← Back to Experiments</a>
        <span className="divider">|</span>
        <span className="current-page">New Experiment</span>
      </div>

      <section>
        <h2>Fault Type</h2>
        <div className="target-grid">
          {targets.map((t) => (
            <div
              key={t}
              className={`target-card ${faultType === t ? "selected" : ""}`}
              onClick={() => setFaultType(t)}
            >
              {t}
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2>Target Configuration</h2>
        <div className="form-group">
          <label>Cluster</label>
          <input value={cluster} onChange={(e) => setCluster(e.target.value)} placeholder="Cluster name" />
        </div>
        <div className="form-group">
          <label>Namespace</label>
          <input value={namespace} onChange={(e) => setNamespace(e.target.value)} placeholder="Namespace" />
        </div>

        <h3>Labels</h3>
        <div className="param-entry-row">
          <input placeholder="Key" value={labelKey} onChange={(e) => setLabelKey(e.target.value)} />
          <input placeholder="Value" value={labelValue} onChange={(e) => setLabelValue(e.target.value)} />
          <button onClick={addLabel}>Add Label</button>
        </div>
        <ul className="data-list">
          {Object.entries(labels).map(([k, v]) => (
            <li key={k} className="data-item">
              <span><strong>{k}:</strong> {v}</span>
              <button className="remove-btn" onClick={() => removeLabel(k)}>×</button>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2>Experiment Info</h2>
        <label>Name</label>
        <input value={experimentalName} onChange={(e) => setExperimentName(e.target.value)} placeholder="Name" />

        <label>Timeout (seconds)</label>
        <input type="number" value={timeoutSeconds} onChange={(e) => setTimeoutSeconds(e.target.value)} />

        <div className="row-switch">
          <label>Dry Run Allowed</label>
          <input type="checkbox" checked={dryRunAllowed} onChange={() => setDryRunAllowed(!dryRunAllowed)} />
        </div>

        <h3>Parameters</h3>
        <div className="param-entry-row">
          <input placeholder="Key" value={paramKey} onChange={(e) => setParamKey(e.target.value)} />
          <input placeholder="Value" value={paramValue} onChange={(e) => setParamValue(e.target.value)} />
          <button onClick={addParameter}>Add</button>
        </div>
        <ul className="data-list">
          {parameters.map((p, i) => (
            <li key={i} className="data-item">
              <span><strong>{p.key}:</strong> {p.value}</span>
              <button className="remove-btn" onClick={() => removeParameter(i)}>×</button>
            </li>
          ))}
        </ul>

        <h3>SLO Targets</h3>
        <div className="param-entry-row">
          <div className="select-stack">
            <label>Metric</label>
            <select value={sloMetric} onChange={(e) => setSloMetric(e.target.value)}>
              <option value="LATENCY_P95">Latency P95</option>
              <option value="ERROR_RATE">Error Rate</option>
              <option value="AVAILABILITY">Availability</option>
            </select>
          </div>
          <div className="select-stack">
            <label>Comparator</label>
            <select value={sloComparator} onChange={(e) => setSloComparator(e.target.value)}>
              <option value="<">Less Than</option>
              <option value="<=">Less & Equal Than</option>
              <option value=">">Greater Than</option>
              <option value=">=">Greater & Equal Than</option>
            </select>
          </div>
          <div className="select-stack">
            <label>Threshold</label>
            <input type="number" value={sloThreshold} onChange={(e) => setSloThreshold(e.target.value)} />
          </div>
        </div>
        <label>PromQL Query</label>
        <div className="param-entry-row">
          <input value={sloPromQuery} onChange={(e) => setSloPromQuery(e.target.value)} placeholder="rate(http_requests_total...)" />
          <button onClick={addSlo}>Add SLO</button>
        </div>
        <ul className="data-list">
          {slos.map((s) => (
            <li key={s.id} className="data-item">
              <span>{s.metric} {s.comparator} {s.threshold} — <small>{s.promQuery}</small></span>
              <button className="remove-btn" onClick={() => removeSlo(s.id)}>×</button>
            </li>
          ))}
        </ul>
      </section>

      {statusMessage.text && (
        <div className={`status-info ${statusMessage.type}`}>
          {statusMessage.text}
        </div>
      )}

      <button className="submit-btn" onClick={submitExperiment}>Submit Experiment</button>
    </div>
  );
}