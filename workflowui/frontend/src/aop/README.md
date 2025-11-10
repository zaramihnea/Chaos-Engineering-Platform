Aspect-Oriented Programming (AOP) in Frontend

Aspect-Oriented Programming is a programming paradigm that focuses on separating cross-cutting concerns from the main business logic and these include: Logging, Error handling, Monitoring, Security etc.

In JavaScript and React there is no native AOP support but these can be replicated.
For example:
1. Logging Aspect (Rendering Aspect)
- This aspect logs every time a page or component is rendered.
    \workflowui\src\aop\withLogging.jsx
2. Action Logging Aspect (User Interaction Aspect)
- This aspect logs each time the user does an action (in this case filters some data)
    \workflowui\chaos-engineering-platform\src\aop\withActionLogging.jsx

Other example of usage might be in case of Error Handling for API requests, button clicks, form submisions etc.
