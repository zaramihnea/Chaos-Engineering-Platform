Monitoring-Oriented Programming (MOP) in frontend

Monitoring-Oriented Programming (MOP) is a software engineering approach where the system continuously observes and checks its own behavior at runtime using predefined rules called properties. If the running system violates one of these rules, the monitor detects it and can log a warning, trigger an alert, or enforce corrective actions.

Two monitoring mechanisms are implemented:
1. Form Constraint Monitor 
The Form Constraint Monitor observes user input in forms and checks whether the current values respect the defined rules.
Whenever a rule is violated, the monitor logs a MOP violation to the console.
Use case: 
• Login page: email and password required
• Experiment creation: fault type and other parameters needs to be selected 
  \workflowui\src\mop\useFormMonitor.jsx
2. Navigation Monitor
The Navigation Monitor ensures that a user cannot access certain pages without satisfying required conditions.
This prevents invalid states such as:
• Accessing a workflow details page without selecting a workflow
• Jumping into protected areas through direct URL typing
  \workflowui\src\mop\useNavigationMonitor.jsx
