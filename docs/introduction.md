Oct 13, 2025 Proposal

# Introduction

Ballerina Workflow Support is a proof of concept (POC) for a workflow engine implemented in Ballerina. It aims to provide a framework for defining, executing, and managing workflows in a structured manner.

## Key Features

The core features this POC aims to support are:

- **Persistence**: The ability to save the state of a running workflow and resume it later.  
- **Interruptibility**: The ability to pause a workflow to wait for an external event or trigger.  
- **Correlation**: The ability to map an external event to a specific, running workflow instance.

**Note**: Human tasks are not in the scope of this POC. However, they can be modeled as interruptible and correlated activities.

## Definitions

- **Workflow Model**: Defines the structure and behavior of a workflow. This is typically graph-based (i.e., composed of nodes, edges, and gates).  
- **Workflow**: An executing instance of a workflow model. It represents the actual execution paths (including subgraphs) taken, including the current state of all nodes and edges.  
- **Workflow Client (API)**: The public interface that external applications or services use to interact with the workflow engine or particular workflow model. It provides the necessary functions to manage the lifecycle of a workflow instance. Key responsibilities include:  
  * Starting a new workflow instance from a specific model.  
  * Sending events or messages to an active workflow to trigger interruptions or provide data.  
  * Querying the current status and state of a workflow instance.  
  * Terminating, pausing, or resuming a workflow.  
- **Workflow Engine**: A software component that executes and manages workflows based on their defined models. It handles the execution of nodes, transitions between them, and the overall state of the workflow.  
- **Memory**: The **persistence store** used to save a workflow's state. This could be an in-memory store, an RDBMS, a cloud database, or a SaaS platform.

(Note: The Following definitions are used only to explain the DX of some approaches.)

- **Node (Activity)**: Represents a specific activity or task within a workflow. *(Note: A more suitable name that aligns with BI terminology should be considered.)*  
- **Edge**: Represents a  ~~transition or connection~~ control flow dependency between two nodes in a workflow. This can be implicit or explicit.  
- **Gate**: Represents a decision point in a workflow where the execution path can branch based on certain conditions. A gate could be modeled as part of an edge. However, gates are more complex and might be out of scope for this POC.

## Design Rationale

### Why Workflows Are Separate from Integration Services

A key design goal is to allow workflow integrations to co-exist with normal integration services and remain independent of specific protocols or event sources within the Ballerina ecosystem. 

This design deliberately keeps workflows separate from regular Ballerina integration services (like HTTP services) for several reasons:

**Protocol-Specific Semantics Conflict**: Regular integration services have their own inherent semantics that conflict with workflow requirements. For example:

- In HTTP resource functions, a `return` statement implies sending a response back to the client  
- Receiving interrupting events from other protocols (like message queues) while inside an HTTP resource function would create overly complicated design patterns  
- Each protocol has its own lifecycle and error handling semantics that don't align with workflow state management, because replay/restore should be able to occur without any side effects. 

**Interruptibility Requirements**: Workflows need the ability to pause execution and wait for external events i.e. the path that contains the receiving activity. Navigation along other paths of the flow continues.  This requires:

- Channels or queue-like mechanisms for event delivery  
- These mechanisms inherently cause Ballerina workers to suspend  
- Modeling this properly would require new language features and an interruptible execution runtime (potentially built on Java Loom)

**Leveraging Existing Language Features**: Instead of introducing new language constructs, this design:

- Uses existing Ballerina values and functions as the foundation  
- Implements workflow features as a platform capability on top of the existing Java runtime  
- Maintains compatibility with the current Ballerina ecosystem  
- Allows workflows to integrate seamlessly with existing integration services without modifying their semantics

**Visual Modeling as a Core Requirement**: Workflows must support visual modeling alongside the three key technical features (persistence, interruptibility, and correlation). This requires:

- A declarative structure that can be easily visualized  
- Clear separation between the visual model and the implementation logic  
- The ability to generate tooling and designers from the same model definition

## 

## Modeling a Workflow in Ballerina

There are three main approaches. 

1. Implement it as a pure library.  
2. Utilize existing language features.  
   1. Introduce DSL using existing language features (i.e. Persist tool)  
   2. Fit into existing language concepts. (i.e., Listeners/Services)  
3. Introduce new language features.

We will take a **hybrid approach**, combining a pure library with existing language features. **We are avoiding new language features (option 3\)** as this would require significant compiler effort.

### Modeling

Following the hybrid approach, the next key problem is workflow modeling. We have two main options:

* M1: Implicit Model (Code-First)  
  * The model is the code, and the code is the model. The workflow is defined implicitly using standard Ballerina code.  
  * The model is implicit in the code. We can then use techniques (like algorithms used in WSO2 Integrator: BI) to parse this code and generate a low-code diagram from it.  
  * This approach is more natural because it directly leverages Ballerina's native control flow semantics to define the workflow.  
* M2: Explicit Model (Model-First)  
  * You first define a "model skeleton" using explicit concepts like Edges and Nodes. Then, you implement the logic inside these stubs.  
  * This simplify the modeling.   
  * But, this introduces a new modeling concept to Ballerina, even though Ballerina's own control flow semantics are already natural for workflows.

### Achieving Persistence, Interruptibility, and Correlation  

### **Achieving Persistence, Interruptibility, and Correlation**

We are considering two primary methods for persistence:

* **P1: Snapshot**: Persist the complete workflow state or a selected scope as a snapshot. Then, reconstruct the state from the snapshot.  
* **P2: Event Replay**: Save all events as they occur. Reconstruct the workflow's state by replaying this sequence of events.

Both snapshotting (other than a full dump) and event replay work **if and only if** the core workflow logic is deterministic. This means the workflow logic itself cannot contain random number generation, currentTime() calls, external API calls, or system config lookups. These operations can still happen within activities (nodes), but **not inside** the core workflow logic.

To manage long-running, interruptible processes, we considered two concepts:

* **I1:** Utilize Ballerina **Client Semantics**  
* **I2:** Use a new **"Channels"** like concept

Since we have already decided not to introduce new language features, option **I2 (Channels) has been ruled out**. We are planning to utilize **I1 (Client Semantics)** to handle this.

Let's see how we can handle events on both sides:

* **Inside the workflow Logic:**  
  * e.g., The workflow logic waits for events, such as timer events.  
  * We can model this as a generic workflow functionality (like a library function) or as part of the language construct design (will be discussed in more detail later).  
* **Sender:**  
  * An external construct that interacts with the workflow. Typically, this is a Ballerina client (either generic or generated).  
  * This requires correlation.  
  * We have multiple implementation options for correlation:  
    1. Take the **correlation ID** as a parameter in the client's remote/resource action signature.  
    2. Provide the **correlation ID** to a factory or service to get the relevant client instance.  
  * We are leaning towards the **first option**, as it is more direct and avoids an additional lookup step.

## 

## **Possible DX Options (Re-Ordered)**

Here, Options 1-4 are **"Code-first,"** and Option 5 is **"Model-first."**

### **1\. Workflow as a Library**

**Idea**

* The user defines workflow activities as functions (or lambda expressions). The execution flow is represented using a graph notation (nodes and edges), i.e., a JSON value (Agent graph-like model).  
* This model is given to the workflow library for execution.  
* **Persistence**: Snapshot  
* **Events**:  
  * **Sender**: We use a generic/generated client to send signals/events to the workflow.  
  * **Workflow logic**: Use a workflow engine function.  
* Additional variables will be shared using a property bag.

**Development Effort**

* The challenge is designing a proper API.  
* Snapshotting is simple to implement because the persistence scope is well-defined at the function level.

**Low-Code Experience**

* Requires significant effort to build a low-code experience.

**Pros**:

* Gives the developer full programmatic control over the workflow definition.  
* It can be used anywhere within Ballerina constructs.

**Cons**:

* Designing a clean and simple API is difficult due to Ballerina's limitations with generic function types, potentially leading to a complex developer experience with casts, etc.  
* Building a low-code visualization is difficult.

### **2\. Workflow as a Function**

**Idea**

* The workflow is a single Ballerina function. It may be annotated or use special function syntax.  
* An activity will be a statement within the function.  
* Control flow is defined using if/else and loops.  
* Parallel execution paths are implemented via **Workers**.  
* **Variables**: In-scope  
* **Persistence**:  
  * **Option 1 (Preferred)**: Persist events and reconstruct by replaying events.  
  * **Option 2**: Persist at the statement level. This is conceptually difficult to implement, as it requires a runtime change.  
* **Events**:  
  * **Sender**: We use a generic/generated client to send signals/events to the workflow.  
  * **Workflow logic**: Use a workflow engine function.

**Development Effort**

* The event-based persistence model and replay require moderate effort.  
* Statement-level persistence is complex to implement.

**Low-Code Experience**

* Works with the existing **WSO2 Integrator: BI** flow control diagram, with additional workflow-related nodes in the RHS.

**Pros**:

* Simple and easy to visualize using existing WSO2 Integrator: BI control flow semantics.  
* The entire logic is contained in one place.

**Cons**:

* Events are second-class citizens and require dynamic typing.  
* Query and Update (forward recovery) semantics are not supported.  
* Can we achieve deterministic code?  
* Attaching to workflow engine semantics.

### **2.b. Activities as Functions**

To solve the determinism problem, we can externalize activities to functions, similar to Temporal. This means we need to use replay as the persistence model.

However, the other disadvantages are not yet solved.

### **3\. Worker-Based Model**

The idea is to solve the persistence problem from Option 2\.

**Idea**

* Similar to **WSO2 Integrator: BI V1**. Define workflow activities using **Workers** (not at the statement level).  
* Worker interactions define the workflow edges.  
* **Variables**: In-scope  
* **Persistence**: Replay  
* **Events**:  
  * **Sender**: We use a generic/generated client to send signals/events to the workflow.  
  * **Workflow logic**: Use a workflow engine function.

**Development Effort**

* The event-based persistence model and replay require moderate effort.

**Low-Code Experience**

* Requires significant effort to build a low-code experience, as this deviates from the WSO2 Integrator: BI flow.

**Pros**:

* Fits well with Ballerina's concurrency features.

**Cons**:

* Static worker semantics break with the workflow client for receiving events.  
* Leads to complex code, because to achieve workflow functions, you need to program the code in a specific way.  
* Events are still second-class citizens and require dynamic typing.  
* Query and Update (forward recovery) semantics are not supported.

### **4\. Workflow as a (Service) Object**

Another approach to solve the problems in Option 2.b (Similar to Temporal).

**Idea**:

* The workflow is a (service) **class**, where a remote/resource method represents the workflow model. Events will be other remote/resource methods in the object. This can support:  
  * Main logic  
  * Signal (Asynchronous events)  
  * Query (Synchronous read-only)  
  * Update (Synchronous read-write)  
* The **listener** can be used as the workflow engine.  
* **Persistence**: Replay  
* **Events**:  
  * **Sender**: We use a generic/generated client to send signals/events to the workflow.  
  * **Workflow logic**: Use a workflow engine function.  
* **Variables**: Service-level and in-scope.  
* **Additional Activities**: Functions

**Development**:

* The event-based persistence model and replay require moderate effort.

**Low-Code Experience**

* Works with the existing **WSO2 Integrator: BI** flow control diagram, with additional workflow-related nodes in the RHS.  
* Additionally, the Service designer will be used to model the workflow.

**Pros**:

* Works with existing Ballerina language constructs.  
* Low-code effort is minimal.  
* Fixes Option 2's problems.

**Cons**:

### **5\. Function Graph Model**

This is an improvement on Option 1\. We use Ballerina tool support to hide the complexity of building the flow. This approach is inspired by the ballerina/persist tool. This is a **"Model-first"** approach.

**Idea**:

* The user defines a model using Ballerina constructs, specifying **nodes and edges**.  
* The user uses a **Ballerina workflow tool** to generate the skeleton (an object type) \- similar to an OpenAPI skeleton \- and a **Client** to interact with the workflow.  
* The user implements the class with the business logic.  
* **Persistence**: Replay  
* **Events**:  
  * **Sender**: We use a generic/generated client to send signals/events to the workflow.  
  * **Workflow logic**: Use a workflow engine function.

**Development**:

* The event-based persistence model and replay require moderate effort.

**Low-Code Experience**

* A new low-code view for the workflow.

**Pros**:

* The tool hides the workflow's boilerplate code from a defined model. The developer only needs to implement the custom business logic for each node (function).  
* Can be extended to support BPMN activities, Agent Activities, and code templates.

**Cons**:

* Update and Query will be second-class citizens.

## Summery

### 

| Criteria | 1\. As a Library | 2\. As a Function | 3\. Worker-Based | 4\. As a (Service) Object | 5\. Functional Graph Model |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **Core Idea** | Graph (JSON) of nodes/edges passed to a library. | Single Ballerina function with standard control flow. | worker interactions define the workflow edges. | A **service class** where remote methods are workflow actions (run, signal, query). | A **tool generates** a skeleton class from a defined model (nodes/edges). |
| **Modeling** | Code-first | Code-first | Code-first | Code-first | **Model-first** |
| **Persistence** | Snapshot of the graph. | Replay | Replay | Replay | Replay |
| **Low-Code Effort** | **High Effort** (New view) | **Low Effort** (Uses existing BI diagram) | **High Effort** (New view, deviates from BI) | **Minimal Effort** (Uses existing BI diagram \+ service designer) | **Medium Effort** (New view) |
| **Pros** | Full programmatic control. | Simple, uses existing BI visualization. | Fits Ballerina concurrency. | Uses existing constructs; fixes Option 2's issues. | Tool hides boilerplate; extensible (BPMN, Agents). |
| **Cons** | Hard to design API; hard to visualize. | Events are second-class; no Query/Update; determinism issues. | Complex code; static workers; events are second-class. |  | Update/Query are second-class. |
| **Selected** | **No** | **No** | **No** | **Yes** | **Yes** |

---

## Recommendation: 

Based on the comparison, I am strongly leaning towards, 

* Option 4: Workflow as a (Service) Object  
* Option 5: The Functional Graph Model.

This approach provides the clearest and most effective balance between the pro-code and low-code worlds.
