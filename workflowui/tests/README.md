Test-Driven Development (TDD) — Iteration 1

Registering Workflows in WorkflowManager

The system must allow the WorkflowManager (a Singleton) to store multiple Workflow objects.
This is essential because users can create and manage multiple workflows in the application.

RED – Write a Failing Test
- src/test/java/com/example/workflowui/WorkflowManagerTest.java
The issue: workflows list is null and adding a workflow throws a NullPointerException.

GREEN – Write Minimal Code to Pass the Test
- src/main/java/com/example/workflowui/WorkflowManager.java
The test now passes successfully. 
- No NullPointerException and registered workflows are stored.