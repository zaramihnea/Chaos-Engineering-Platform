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

  useFormMonitor(
    {
      experimentalName,
      faultType,
      cluster,
      namespace,
    },
    {
      experimentalName: {
        check: (v) => v.length > 0,
        message: "Experiment must have a name",
      },
      faultType: {
        check: (v) => v !== null,
        message: "Fault type must be selected",
      },
      cluster: {
        check: (v) => v.length > 0,
        message: "Cluster field cannot be empty",
      },
      namespace: {
        check: (v) => v.length > 0,
        message: "Namespace cannot be empty",
      },
    }
  );

  const targets = [
    "POD_KILL",
    "CPU_STRESS",
    "MEMORY_STRESS",
    "NETWORK_DELAY",
    "NETWORK_PARTITION",
  ];

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
      metric: sloMetric,
      comparator: sloComparator,
      threshold: Number(sloThreshold),
      promQuery: sloPromQuery,
    };

    setSlos([...slos, newSlo]);

    setSloThreshold("");
    setSloPromQuery("");
  };

  const buildApiPayload = () => {
    return {
      id: crypto.randomUUID(),
      name: experimentalName,
      faultType: faultType,
      parameters: Object.fromEntries(parameters.map((p) => [p.key, {}])),
      target: {
        cluster,
        namespace,
        labels,
      },
      timeout: `PT${timeoutSeconds}S`,
      slos: slos,
      dryRunAllowed,
      createdBy: "ui-user",
    };
  };

  const submitExperiment = async () => {
    const payload = buildApiPayload();

    try {
      const response = await fetch("/api/experiments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error("Failed to submit experiment");

      alert("Experiment successfully submitted!");
      console.log("API RESPONSE:", await response.json());
    } catch (err) {
      console.error(err);
      alert("Error submitting experiment.");
    }
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

        <label>Cluster</label>
        <input
          value={cluster}
          onChange={(e) => setCluster(e.target.value)}
          placeholder="Cluster name"
        />

        <label>Namespace</label>
        <input
          value={namespace}
          onChange={(e) => setNamespace(e.target.value)}
          placeholder="Namespace"
        />

        <h3>Labels</h3>
        <div className="param-entry-row">
          <input
            placeholder="Key"
            value={labelKey}
            onChange={(e) => setLabelKey(e.target.value)}
          />
          <input
            placeholder="Value"
            value={labelValue}
            onChange={(e) => setLabelValue(e.target.value)}
          />
          <button onClick={addLabel}>Add Label</button>
        </div>

        <ul>
          {Object.entries(labels).map(([k, v]) => (
            <li key={k}>
              {k}: {v}
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2>Experiment Info</h2>

        <label>Name</label>
        <input
          value={experimentalName}
          onChange={(e) => setExperimentName(e.target.value)}
        />

        <label>Timeout (seconds)</label>
        <input
          type="number"
          value={timeoutSeconds}
          onChange={(e) => setTimeoutSeconds(e.target.value)}
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

        <ul>
          {parameters.map((p, i) => (
            <li key={i}>
              {p.key}: {p.value}
            </li>
          ))}
        </ul>

        <h3>SLO Targets</h3>

        <label>Metric</label>
        <select
          value={sloMetric}
          onChange={(e) => setSloMetric(e.target.value)}
        >
          <option value="LATENCY_P95">Latency P95</option>
          <option value="LATENCY_P99">Latency P99</option>
          <option value="ERROR_RATE">Error Rate</option>
          <option value="THROUGHPUT">Throughput</option>
          <option value="AVAILABILITY">Availability</option>
        </select>

        <label>Comparator</label>
        <select
          value={sloComparator}
          onChange={(e) => setSloComparator(e.target.value)}
        >
          <option value="<">Less Than</option>
          <option value=">">Greater Than</option>
        </select>

        <label>Threshold</label>
        <input
          type="number"
          value={sloThreshold}
          onChange={(e) => setSloThreshold(e.target.value)}
        />

        <label>PromQL Query</label>
        <input
          value={sloPromQuery}
          onChange={(e) => setSloPromQuery(e.target.value)}
        />

        <button onClick={addSlo}>Add SLO</button>

        <ul>
          {slos.map((s, i) => (
            <li key={i}>
              {s.metric} {s.comparator} {s.threshold} — {s.promQuery}
            </li>
          ))}
        </ul>
      </section>

      <button className="submit-btn" onClick={submitExperiment}>
        Submit Experiment
      </button>
    </div>
  );
}
