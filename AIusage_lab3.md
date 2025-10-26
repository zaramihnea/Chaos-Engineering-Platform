We provided OpenAI's [ChatGPT](https://chat.openai.com) with the list of [requirements](https://github.com/zaramihnea/Chaos-Engineering-Platform/blob/main/team_responsability.txt) we have thought about with our coordinator (Olariu FLorin). We then asked it to suggest any changes it would make and recommend components for implementation that we did not think about.

---

### The prompts we used for this laboratory:

## problem 1
- uploaded team-responsability.txt for context
- "given the responsabilities i sent you for each team member, solve this problem: ..." **we gave it problem 1's text**. The result was an early draft of the requirement analysis. We noted that it didn't change much about the responsibilities we gave it, it more or less gave us a more neatly formatted version.
- "what would you add to this list of requirements that you think we've missed or haven't touched upon?". The LLM suggested we take a closer look at making the app work in a CI/CD pipeline while also integrating some guardrails (role based access) and additional fault types if we have the time.
- we consulted our coordinator and agreed on the .md file it created. It mostly resembles what we had in mind.

## problem 2
In regard to the diagrams we have found that it would hallucinate a tad bit in regards to use case diagrams. It seems as it couldn't differentiate between a use-case and a sequence diagram.
However it's good at generating class diagrams using Mermaid.
Example prompts:
- "I am building the test app module for an application similar to Chaos Monkey and Chaos mesh. To be more particular I am trying to develop a simple microservices app which should use design pattern such as circuit breaker which are used to help enhance system resilience and against fault injection. I will also integrate prometheus, grafana and k6 for SLO's and load testing. Can you help me design a UML class diagram with these requirements in mind?"
- The result is [this](https://github.com/zaramihnea/Chaos-Engineering-Platform/blob/main/Diagrams/Iulian_uml.png)
