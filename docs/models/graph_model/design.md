# Design Deep Dive: The Functional Graph Model

This model is designed to achieve the ideal balance between low-code visual modeling and high-control pro-code development, while successfully implementing key features like:

- **Persistence**
- **Interruptibility**
- **Correlation**
- **Visual Modeling**

The workflow itself is defined as a declarative data structure (a graph), which is then interpreted by the workflow engine. This allows for a clear separation between the workflow model's definition and the user-defined functions that implement the business logic.

A key design goal is to allow workflow integrations to co-exist with normal integration services and remain independent of specific protocols or event sources within the Ballerina ecosystem.

## Components of the Design

The design consists of five core components:

1. **Workflow Model**: The declarative graph definition.
2. **Generated Schema**: Typed Ballerina definitions specific to the model.
3. **Generated Client**: A type-safe client to interact with the workflow.
4. **Workflow Runtime**: The core engine and supporting libraries (e.g., for Memory).
5. **User Logic**: The user-written Ballerina functions containing the business logic.

The typical user flow follows these steps:

1. The user **defines** the workflow model in a dedicated `.bal` file.
2. The user **generates** the typed schema and client based on the model.
3. The user **implements** the business logic for each node as standard Ballerina functions.
4. The user **initializes** the workflow, providing a memory provider and other configurations.
5. Finally, the user **integrates** the workflow into a service or automation using the generated client.

---

## 1. The Workflow Model

The workflow model is a directed graph of nodes and edges defined using standard Ballerina values. Similar to the Ballerina `persist` model, the workflow model is defined in a separate file (e.g., `workflows/model.bal`). It contains the following components:

### Nodes

A node represents a unit of work. There are two main types:

- **Event Node**: Represents a step that waits for an external trigger (e.g., a Start Event, Timer Event, or an event that interrupts a flow).
- **Task/Activity Node**: Represents a step that executes immediately and contains business logic.

A node has two distinct representations:

1. **Model Representation**: An abstract definition in the workflow model file that defines the node's properties, inputs, and outputs.
2. **Implementation Representation**: The concrete business logic, defined as a function in a user source file.

The function signatures differ for each node type:

- **Event Node Function Signature:**

  ```ballerina
  public function <event_node_name>(WorkflowContext ctx, ET event) returns error?
  ```

  Where `ET` is the specific event type generated from the model.

- **Task/Activity Node Function Signature:**

  ```ballerina
  public function <task_node_name>(WorkflowContext ctx) returns error?
  ```

  The `WorkflowContext` is a generated type containing the workflow instance, variables, and other contextual information.

### Edges

An edge is a directed connection between two nodes, defining the path of execution. An edge defines the `source` and `target` nodes and can have an optional `condition` that points to a Ballerina function. This function must return `true` for the path to be taken.

### Variables

Variables store custom data within a workflow instance and are persisted with the workflow's state. They are defined in the model and become accessible through the `WorkflowContext` during execution.


## 2. Generated Workflow Schema

Based on the user's workflow model, a tool generates a separate module (e.g., `generated/<workflow_name>/model.bal`). This file contains workflow-specific Ballerina types, including the `Variables` record, typed `Events`, and the `WorkflowContext`.

## 3. Generated Workflow Client

A type-safe client to interact with the workflow is also generated (e.g., `generated/<workflow_name>/client.bal`). This client is the key to achieving **interruptibility** and **correlation**. It is used to start workflow, send events to them, and query their status. Each workflow model has its own uniquely generated client.

## 4. Workflow Runtime & Libraries

The core execution and orchestration logic is provided by a standard Ballerina module, `ballerina/workflow`. Supporting libraries provide pluggable functionalities, such as different `Memory` providers (in-memory, database, etc.).

## 5. User Logic

This is the business logic implemented by the developer as standard Ballerina functions in their source files. This logic is reusable across different workflow models. Users integrate their workflows into services or automations using the generated client.

---

## Achieving Key Workflow Features

This model's clean separation of concerns allows for the straightforward implementation of core features.

### Persistence

Persistence is handled by the engine between node executions to ensure reliability.

1. The engine executes the function associated with **Node A**.
2. Upon its successful completion, the engine updates the workflow instance's state (its variables and current position in the graph).
3. **Before** moving to the next node, the engine calls the `Memory` component to **save the entire state** of the workflow instance.
4. Only after the state is successfully saved does the engine proceed to execute **Node B**.

This guarantees that if the system fails, the workflow can be resumed from the last successfully completed step.

### Interruptibility

Interruptibility is the ability to pause a workflow to wait for an external event.

1. When the engine's execution reaches an **Event Node**, it automatically **suspends** the workflow instance.
2. The engine saves the complete state of the workflow to memory. The instance now remains dormant, awaiting a specific event.
3. The engine maintains an internal queue of expected events for all suspended workflow instances.

### Correlation

Correlation is the ability to route an incoming event to the correct waiting workflow instance.

1. When defining the workflow model, the user specifies **correlation properties** for event nodes. A unique `anydata` value can be used as a correlation property, so matching is done based on the value of this property.
2. When an external system sends an event via the generated client, it provides the data containing this correlation property.
3. The engine uses the correlation property's value to find the unique workflow instance that is waiting for that specific event and value.
4. Once a match is found, the engine updates the instance's state with the event data and **resumes** its execution from the event node.

---

## Open Questions & Challenges

### Handling Parallelism and State

Parallel execution in this model introduces a significant challenge with state management, as multiple active nodes could attempt to modify a shared set of variables, leading to race conditions. A simple fork-join pattern is insufficient. Instead, the model handles this using a **fan-out/fan-in** approach with isolated variable scopes.

- **Fan-Out**: When a node initiates multiple parallel paths (e.g., `A` transitions to both `B` and `C`), the engine doesn't share the live state. Instead, it **clones the workflow's variable context** for each new branch. This gives paths `A->B` and `A->C` isolated scopes, allowing them to modify variables without interfering with each other.

- **Fan-In**: When these parallel paths later converge on a single node (e.g., `B` and `C` both transition to `D`), the engine must reconcile the different states. It performs a **merge operation** on the variable contexts from the completed branches. This requires a defined strategy to handle potential conflicts if the same variable was modified differently in parallel paths, such as a "last-write-wins" policy or allowing the user to define custom merge logic.

This **clone-on-fan-out** and **merge-on-fan-in** strategy ensures that data integrity is maintained, even in complex workflows with multiple active parallel paths.

### Handling Cycles in the Graph

There are several approaches to implementing loops in a workflow model. Each has its own trade-offs regarding visibility, persistence, and interruptibility.

1. The Cyclical Graph Approach
2. The Internal Loop Approach
3. Structural Control Flow Nodes

#### The Cyclical Graph Approach

This is the standard and most powerful method for handling loops in a workflow. It's done by creating a **cycle in the graph**, where an edge from a downstream node points back to an upstream node.

**How it works:**

1. **The Loop Body**: One or more nodes represent the work to be done in a single iteration (e.g., a `ProcessItem` node).
2. **The Gatekeeper**: After the loop body, a decision point is needed. This can be a dedicated "decision node" or simply a **conditional edge** that checks if the loop should continue.
3. **The Cycle**: If the condition to continue is met (e.g., a function `hasMoreItems()` returns `true`), the workflow follows an edge that points back to the beginning of the loop body.
4. **The Exit**: If the condition is false, the workflow follows a different edge that continues to the rest of the process.

**Advantages of this approach:**

- **Visibility**: The loop is an explicit part of the workflow model. Anyone looking at the graph can see the iterative nature of the process.
- **Persistence**: The workflow's state is saved after **each iteration** (i.e., after each node in the cycle completes). If the system fails after processing 50 out of 100 items, it can resume from item 51, not from the beginning.
- **Interruptibility**: The flow can be paused, interrupted by an event, or timed out *between* iterations, which is impossible if the loop is hidden inside a single task.

#### The Internal Loop Approach

This method involves hiding the iteration logic inside a single Task/Activity node. The implementation function for a node like `ProcessAllItems` would contain a standard Ballerina `foreach` or `while` loop.

**How it works:**
The workflow engine calls a single function. That function runs its own internal loop from start to finish and then returns. From the engine's perspective, this is just **one atomic task**.

**Disadvantages:**

- **No Visibility**: The workflow model just shows a single `ProcessAllItems` task. It gives no insight into the progress of the loop.
- **No Granular Persistence**: State is saved only *before* the task starts and *after* the entire loop completes. A failure mid-loop means the entire set of operations must be restarted.
- **No Interruptibility**: The entire loop is a single transaction that cannot be paused or influenced by external events between its internal steps.

This "black box" approach is only suitable for very short, non-critical loops that operate on in-memory data and don't require the resilience and visibility that a true workflow engine provides.

#### Structural Control Flow Nodes

Instead of expressing all logic through graph topology (cycles for loops, branches for ifs), the model itself would include nodes for if-else, while, and foreach.

**How It Would Work:**

- **`While` Node**: This node would encapsulate a **sub-flow** (a sequence of other nodes that form the loop's body). It would also take a condition. The engine would execute the condition, if true, then the sub-flow, then re-evaluate the condition, and repeat until the condition is false.

- **`ForEach` Node**: Similar to a `While` node, this would encapsulate a sub-flow. It would take a collection (an array or list) as input and execute the sub-flow once for each item in the collection, often exposing the current item to the sub-flow's context.

**Advantages:**

- **High Readability**: The workflow model becomes much easier to read and understand. A `While` node is more semantically clear than a cycle in a graph, making the model's intent obvious, especially for developers and low-code users.
- **Simplicity of Models**: It can significantly simplify the visual graph by reducing the number of nodes and edges required to represent common patterns.
- **Familiarity**: The constructs are immediately familiar to anyone with programming experience, potentially lowering the learning curve.
