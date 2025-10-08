# Introduction

Ballerina Workflow Support is a proof-of-concept (POC) for a workflow engine implemented in Ballerina. It aims to provide a framework for defining, executing, and managing workflows in a structured manner.

## Key Features

The core features this POC aims to support are:
-   **Persistence**: The ability to save the state of a running workflow and resume it later.
-   **Interruptibility**: The ability to pause a workflow to wait for an external event or trigger.
-   **Correlation**: The ability to map an external event to a specific, running workflow instance.

> **Note**: Human tasks are not in the scope of this POC. However, they can be modeled as interruptible and correlated activities.

## Definitions

-   **Workflow Model**: Defines the structure and behavior of a workflow. This is typically graph-based (i.e., composed of nodes, edges, and gates).
-   **Workflow**: An executing instance of a workflow model. It represents the actual execution path taken, including the current state of all nodes and edges.
-   **Workflow Client (API)**: The public interface that external applications or services use to interact with the workflow engine or particular workflow model. It provides the necessary functions to manage the lifecycle of a workflow instance. Key responsibilities include:
    * Starting a new workflow instance from a specific model.
    * Sending events or messages to an active workflow to trigger interruptions or provide data.
    * Querying the current status and state of a workflow instance.
    * Terminating, pausing, or resuming a workflow.
-   **Node**: Represents a specific activity or task within a workflow. *(Note: A more suitable name that aligns with BI terminology should be considered.)*
-   **Edge**: Represents a transition or connection between two nodes in a workflow.
-   **Gate**: Represents a decision point in a workflow where the execution path can branch based on certain conditions. A gate could be modeled as part of an edge. However, gates are more complex and might be out of scope for this POC.
-   **Workflow Engine**: A software component that executes and manages workflows based on their defined models. It handles the execution of nodes, transitions between them, and the overall state of the workflow.
-   **Memory**: The **persistence store** used to save a workflow's state. This could be an in-memory store, an RDBMS, a cloud database, or a SaaS platform.

## Modeling a Workflow in Ballerina

One of the key problems to solve is how to define a workflow model in Ballerina. There are a few options:

1.  Define a workflow model abstraction using **existing Ballerina constructs** (e.g., records, objects, functions, services, listeners, etc.).
2.  Define a **DSL (Domain-Specific Language)** that can be converted to Ballerina constructs. This could be an external DSL (e.g., YAML, JSON) or an internal DSL (using Ballerina syntax).
3.  Create **new language constructs** to represent workflow models. This is the most complex option, as it requires changes to the Ballerina language itself.

Based on these options, we can take several approaches to define a workflow model in Ballerina.

---

### 1. Listener, Service, and Client Model

[Read more about this approach here.](./models/1_service_listener_model.md)

This approach models a workflow using familiar Ballerina abstractions.
-   **Listener**: Represents the Workflow Engine.
-   **Service and Remote Methods**: Represent the Workflow Model and its Nodes.
-   **Client**: Represents the Workflow Client

**Feature Support:**
-   **Persistence Boundary**: At the remote method level.
-   **Interruptibility Boundary**: Achieved through client calls.
-   **Correlation**: Via client calls with correlation IDs.

---

### 2. Functional Graph Model

[Read more about this approach here.](./models/2_functional_grahp_model.md)

This approach defines the workflow as a graph of functions.
-   **Node**: A function.
-   **Edge**: A record containing source and target nodes, with optional conditions.
-   **Client**: A workflow client to start and interact with a workflow.


> Implementation Note: This approach is inspired by the `ballerina/persist` tool. A new Ballerina tool could be created to generate the workflow's boilerplate code from a defined model. The developer would only need to implement the custom business logic for each node (function) and provide a Memory store when creating the workflow client from the model. Additionally, we can model the Workflow model as objects with functions as methods.

**Feature Support:**
-   **Persistence Boundary**: At the function boundary level.
-   **Interruptibility Boundary**: Achieved through client calls.
-   **Correlation**: Via client calls with correlation IDs.

---

### 3. Worker-Based Model

This approach uses Ballerina workers to model workflow components.
-   **Node**: A worker.
-   **Edge**: The interaction between workers.

**Feature Support:**
-   **Persistence Boundary**: At the worker level.
-   **Interruptibility Boundary**: Only at the function boundary. Internal interruption of a worker is undefined. Since workers run inside a function, we cannot interrupt a running worker without a dedicated language-level construct.
-   **Correlation**: Static only. External correlation is undefined because worker interactions cannot be interrupted from the outside.

> **Note**: This model is similar to the service and remote method-based approach. In Option 1, interruptibility and correlation occur at the function boundary, not at the individual worker boundary.

---

### 4. Workflow as a Function

This approach models the entire workflow as a single Ballerina function.
-   **Node**: A statement within the function.
-   **Edge**: Implicit, via the control flow of the function (e.g., `if/else`, loops).
-   **Worker**: Represents parallel execution paths within the function.

**Feature Support:**
-   **Persistence Boundary**: At the statement level (conceptually difficult to implement).
-   **Interruptibility Boundary**: Only at the function boundary. We cannot interrupt running statements unless a language-level construct is introduced to support this.
-   **Correlation**: Static only. External correlation is undefined because individual statements cannot be interrupted.

Got it. Here are the updated sections of the `readme.md` incorporating your new points.

---

### 5. Workflow as a Library

This approach provides a library that developers can use to programmatically construct and execute a workflow.
-   **Concept**: Similar to the Functional Graph Model, workflow steps are represented as functions or lambda functions. However, instead of using a code generator, the developer uses the library's API to wire these functions together into a coherent flow.
-   **Challenge**: A significant challenge is designing a proper API. Due to Ballerina's current limitations with generics, especially concerning function types, the resulting API could become complex and less intuitive for the developer.

---

## Pros and Cons of Each Approach

| Approach | Pros | Cons |
| :--- | :--- | :--- |
| **1. Listener, Service, and Client Model** | Leverages existing, well-understood Ballerina constructs. Good support for key features like persistence and interruptibility. | The overall workflow logic is distributed across multiple components, which can make the complete flow difficult to visualize directly from the code. |
| **2. Functional Graph Model** | Highly flexible and decoupled. The graph structure is explicit and can be supported by code generation tools. Uses standard functions as the execution units. | The workflow model is separate from the implementation. This is a common and accepted trade-off, also seen in tools like OpenAPI, gRPC, and the Ballerina `persist` tool. |
| **3. Worker-Based Model** | Provides an intuitive mapping for parallel activities using a core Ballerina concurrency feature. | Lacks support for internal interruption and external correlation with current language features. This leads to complex state graphs and requires a difficult implementation. |
| **4. Workflow as a Function Model** | Simple and straightforward for linear, non-interruptible workflows. The entire logic is contained in one place. | Requires a complex implementation to support workflow features. Lacks support for internal interruption and external correlation without significant language changes. |
| **5. Workflow as a Library** | Gives the developer full programmatic control over the workflow definition. High degree of flexibility. | Designing a clean and simple API is difficult due to Ballerina's limitations with generic function types, potentially leading to a complex developer experience. |

---

## The Pro-Code vs. Low-Code Balance

Workflows are traditionally geared towards a low-code or no-code experience. The primary goal is to **model the flow of logic visually**, making it accessible to business analysts and integration specialists, not just hardcore programmers.

However, the power of Ballerina lies in its robust pro-code capabilities. Therefore, finding the right balance between a declarative, low-code modeling experience and a powerful, pro-code implementation experience is **essential** for a successful Ballerina workflow programming model. The ideal solution should allow the **flow** to be defined simply (low-code) while the complex **logic** of each step is implemented with the full power of Ballerina (pro-code).

---

### Comparison of Modeling Approaches

Here’s how each of the five options stacks up in achieving this balance.

| Approach | Pro-Code Experience | Low-Code Experience | Verdict and Notes |
| :--- | :--- | :--- | :--- |
| **1. Listener & Service Model** | **Strong**. Developers write services and remote methods, with the compiler assisting in client generation. Interactions are syntactically visible in the code. But This requires writing the program in a specific way to be recognized as a workflow, a pattern common in service architectures.| **Good**. A visual flow can be derived from the code's structure and syntactic elements. | A **balanced approach** that would rely heavily on the compiler and on-the-fly code generation tools to create the visual representation. |
| **2. Functional Graph Model** | **Strong**. Developers use standard Ballerina to define the model in a declarative way and write standard functions to implement the node logic. | **Strong**. The workflow is an explicit, declarative graph. Individual functions can also be visualized using control flow diagrams, a feature already supported in WSO2 Integrator: BI. | ✅ **Excellent Balance**. This approach cleanly separates the *what* (the visual flow) from the *how* (the function's code). |
| **3. Worker-Based Model** | **Difficult**. This model requires a very specific and rigid coding pattern. The flow logic and implementation are tightly coupled. | **Potentially Strong**. It uses worker interactions. With future language features (e.g., channels), it might achieve "code parity" with a visual model. | **Omitted**. This approach is dependent on future language features to be viable. |
| **4. Workflow as a Function** | **Strong**. The entire workflow is the control flow (if/else, loops) of a single Ballerina function. | **Potentially Strong**. The function's internal logic can be visualized with existing control flow diagrams in BI. | **Omitted**. Dependent on significant new language features for persistence and interruption. |
| **5. Workflow as a Library** | **Strong**. Developers use a programmatic API to imperatively build the workflow. The entire model is constructed via code. | **Weak**. There is no declarative model for a low-code tool to easily interpret or modify. | **Omitted**. The API design is severely hampered by current language limitations. |

---

## Recommendation: The Functional Graph Model

Based on the comparison, I am strongly leaning towards **Option 2: The Functional Graph Model**.

This approach provides the clearest and most effective balance between the pro-code and low-code worlds.