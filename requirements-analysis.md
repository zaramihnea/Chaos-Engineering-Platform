# Requirements Analysis Document

### 1. Purpose and Scope
The **Chaos Engineering Platform** aims to provide an automated environment for executing controlled failure experiments on distributed microservices deployed in Kubernetes clusters.  
Its main objective is to validate system resilience and ensure that services remain stable under stress. The platform includes modules for experiment definition, execution, monitoring,
and automated reporting, integrating with observability tools such as Prometheus and Grafana. The platform allows users to inject faults like pod termination, network delay, or CPU stress
and analyze how systems recover according to predefined SLOs.

---

### 2. Actors
| Actor | Description | Main Interactions |
|--------|--------------|-------------------|
| **User / Developer** | Engineers or DevOps staff defining and running chaos experiments. | Create, launch, and review experiments. |
| **Coordinator (Control Plane)** | Central API service managing experiments, workflows, and metrics. | Stores experiment definitions, communicates with agents, queries Prometheus. |
| **Agent** | Lightweight service deployed in the Kubernetes cluster executing faults. | Receives RunPlans from the Control Plane, runs chaos actions, reports results. |
| **Prometheus / Grafana** | External observability stack. | Provides metric data and dashboards for steady-state and post-chaos analysis. |

---

### 3. Major Components

#### a. Control Plane (Backend API) – Zară Mihnea-Tudor
- Central API and database that store experiment configurations and run states.  
- Integrates with Prometheus for SLO evaluation and triggers aborts if thresholds are exceeded.  
- Provides REST endpoints to manage experiments, workflows, and reports.  
- Includes CI/CD pipelines for testing, building, and deploying the system.

#### b. Agent (Fault Executor) – Biliuți Andrei
- Kubernetes DaemonSet responsible for executing fault injections such as:
  - Pod kill, CPU/memory stress, network delay, and partition.  
- Supports “dry-run” and “blast-radius preview” for safety.  
- Communicates securely with the Control Plane using mTLS.  
- Packaged as a Helm chart for easy deployment.

#### c. Test Application & Observability – Roman Iulian
- Simple microservices application (Gateway, Catalog, Cart, Payment) for experiment validation.  
- Includes Prometheus metrics and Grafana dashboards to visualize performance and resilience.  
- Contains synthetic load tests (k6 or hey) to simulate real traffic.

#### d. Workflow & UI – Mihoc Roxana
- Component that allows sequential or parallel execution of multiple experiments.  
- Web UI or CLI interface to define workflows and visualize run results.  
- Generates JSON/Markdown reports with SLO deltas and conclusions.

---

### 4. Use Case Scenarios

**1. Create and Schedule a Chaos Experiment**  
The developer defines an experiment (e.g., kill one pod for two minutes) through the UI.  
The Control Plane validates parameters, stores the definition, and waits for execution.  

**2. Execute Chaos Experiment**  
The Control Plane sends a RunPlan to the Agent.  
The Agent injects the fault, Prometheus collects metrics, and the Control Plane monitors SLOs.  
If performance breaches thresholds, the Control Plane aborts the experiment.  

**3. View Experiment Results**  
The developer opens the UI to review experiment results, steady-state metrics, and generated reports.  

**4. Automated CI/CD Testing**  
On each commit, GitHub Actions runs a pipeline deploying the platform to a test cluster, running one chaos experiment, and asserting recovery through SLO queries.

**5. Define Service-Level Objectives (SLOs)**
The **Service Owner** defines key metrics such as latency, error rate, and availability within the **Control Plane**.
These SLOs are continuously monitored during experiments, with data pulled from **Prometheus** to determine system health and pass/fail conditions.

**6. Dry-Run and Blast-Radius Validation**
Before running a live experiment, the **Developer** or **QA Engineer** performs a *dry-run*.
The **Agent** queries the Kubernetes API and reports which pods or services would be affected.
If the calculated blast radius exceeds policy limits, the **Control Plane** blocks the experiment and requests approval.

**7. Policy Enforcement and Approval Workflow**
The **System Administrator** defines operational rules such as allowed namespaces, fault durations, and business-hour limits.
When a **Developer** submits an experiment, the **Control Plane** validates it against these rules.
Experiments outside policy boundaries are denied or routed to the **Service Owner** for approval.

**8. Real-Time Experiment Monitoring**
During a running experiment, the **Developer** or **Observer** monitors real-time logs and metrics.
The **Control Plane** streams status updates, while **Grafana** displays visual dashboards.
If performance degrades, the system issues alerts and triggers an auto-abort event.

**9. Multi-Step Workflow Execution**
The **Workflow Engine** executes a series of experiments defined in a workflow — for instance,

1. Kill a pod → 2. Wait 60 seconds → 3. Add network latency → 4. Stress CPU.
   Each step validates SLOs before continuing, enabling simulation of cascading failures.


**10. Generate and Review Experiment Reports**
Once an experiment completes, the **Control Plane** compiles metrics, timestamps, and outcomes into structured **JSON** and **Markdown** reports.
The **Service Owner** and **Observer** can download or view them directly in the UI.
The reports highlight whether SLOs were maintained and include recommendations for resilience improvements.

**11. Automated Regression Testing**
Following a new deployment, the **CI/CD Pipeline** automatically runs predefined chaos experiments.
Results are compared with historical data, and if resilience has declined, the pipeline flags the build for investigation.

**12. Security and Access Control**
The **System Administrator** manages user roles and namespace-level permissions.
When unauthorized users attempt to run experiments, the **Control Plane** enforces policies and records an audit log entry for compliance tracking.

**13. Incident Replay**
After a real incident or outage, the **Service Owner** replays similar failure conditions using stored experiment templates.
The **Agent** reproduces the scenario, allowing teams to validate whether fixes improved system recovery.

**14. Resilience Scoring and Reporting**
At the end of each testing cycle, the **Control Plane** aggregates experiment data to compute a **Resilience Score** per service.
The **Observer** or **Manager** uses these scores to track progress over time and prioritize reliability improvements.

---

### 5. Components Implemented This Semester
- **Control Plane:** core API, DB, Prometheus integration, abort logic.  
- **Agent:** pod kill, CPU hog, and network delay faults.  
- **Test Application:** Prometheus metrics and Grafana dashboards.  
- **Workflow Engine:** basic sequential logic and report generation.  
- **CI/CD Pipeline:** automated deployment and chaos validation.

#### If Time Allows (Stretch Goals)
- Add multi-step workflow orchestration (parallel and conditional flows).  
- Implement additional fault types (DNS errors, storage I/O latency, region outage simulation).  
- Create a self-service web UI for experiment creation and visualization.  
- Integrate security guardrails (role-based access, namespace whitelisting).  
- Extend CI/CD tests to include automated resilience scoring and regression tracking.  
- Develop auto-remediation logic, where failed SLOs trigger recovery scripts.  
- Package the platform with Infrastructure as Code (Terraform + Helm) for reproducible environments.

---

### 6. Complexity and Evaluation
The project integrates backend, Kubernetes, DevOps, and observability technologies.  
Complexity lies in ensuring secure, automated communication between distributed components, real-time monitoring of system health, and dynamic reaction to failures.  

Evaluation will consider:  
- Correct identification of actors and workflows.  
- Realistic fault simulation and SLO validation.  
- Automation level in CI/CD testing.  
- Documentation clarity and presentation.
