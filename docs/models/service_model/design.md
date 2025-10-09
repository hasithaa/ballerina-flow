# Design Deep Dive: The Listener, Service, and Client Model

This model is designed to feel native to Ballerina developers by leveraging the platform's core `listener -> service -> remote function` paradigm. Instead of defining a workflow in a separate data structure or model, the workflow's structure is implicitly defined by the code itself.

The central idea is that a workflow can coexist seamlessly with other integration artifacts. It can be called by an HTTP service and can, in turn, make calls to a JMS broker, all while isolating the core workflow logic from protocol-level details.

## Components of the Design

The design consists of five core components:

1.  **Workflow Model**: The `service` declaration itself.
2.  **Node (Workflow Step)**: A `remote function` within the service.
3.  **Edge (Transition)**: A `client call` from one remote function to another.
4.  **Workflow Engine**: The `workflow:Listener` that the service attaches to.
5.  **Generated Client**: The compiler-generated, typed client for the service.

## 1. The Workflow Model

The model is the **`service` declaration itself**. The entire graph of nodes and edges is implicitly defined by the service's remote functions and how they call each other. The code *is* the model.

## 2. Node (Workflow Step)

A node is a **`remote function`** within the service. Each remote function represents a single, atomic step in the workflow. The engine guarantees persistence around the execution of these functions.

There will be two types of remote functions signatures:
1. Event Handlers: where we receive an external event to start or resume a workflow.
2. Task/Activity Handlers: where we perform some business logic as part of the workflow.

## 3. Edge (Transition)

No explicit edge structure is defined. Edges are implicitly defined by **client calls** between remote functions.

An edge is a **client call** from one remote function to another. The orchestration logic is written directly in the code. For example, when `remote function A` uses the service's generated client to call `remote function B`, it defines a directed edge from A to B.

## 4. Workflow Engine

The engine is the **`workflow:Listener`** that the service attaches to. This listener is the "magic" behind the scenes. It intercepts calls to remote functions, manages state, handles persistence, and coordinates the execution.

## 5. Generated Client

A client is automatically generated for the service (by the compiler). This client will be part of the generated code when the workflow service is compiled.

The client is the **compiler-generated, typed client** for the service. It serves two purposes:

1.  **Internal Orchestration**: It is used *inside* a remote function to call the next step in the workflow.
2.  **External Interaction**: It is used by other artifacts (like an HTTP service) to start a new workflow instance or send an event to a running one.

A potential issue is the chicken-and-egg problem: Source code is needed to generate the client, and client code is needed to call the other remote functions. This may require a bootstrapping process.

One option is to have two clients:
1.  An **internal client** used *inside* the workflow service to call the next step in the process. This is generic client, as we can use `self` to call other remote functions. Calling this client schedule the next step in the workflow. Not execute it directly.. 
2.  An **external client** used by other artifacts (like an HTTP service) to start a new workflow instance or send an event to a running one.

## Achieving Key Workflow Features

This model's tight integration with Ballerina's service architecture provides a unique way to handle core features.

### Persistence

Persistence is managed at the **boundary of each remote function call**.

1.  When a remote function is called, the listener intercepts it and saves the state (including the function arguments) to the configured `Memory` provider.
2.  The engine then executes the remote function's logic.
3.  Upon successful completion, the state is updated and saved again.
    If a failure occurs during the function's execution, the system can roll back to the state before the call was made.

### Interruptibility

A workflow is paused when a remote function **completes its logic without calling another remote function**.

1.  A node like `waitForApproval` is implemented as a remote function.
2.  Its job is simply to update the state to "waiting" and then `return`.
3.  Since it doesn't make a client call to the next step, the execution path stops. The listener persists this final state, and the workflow instance becomes dormant.

### Correlation

Correlation is achieved by invoking a different remote function on the service to resume a workflow.

1.  An external event (e.g., an API call triggered by a button click) calls a specific remote function, like `approveOrder(string orderId, ...)`.
2.  This function's logic uses the `orderId` to load the correct workflow state.
3.  It then uses the internal client to call the next step in the process (e.g., `self.client->processPayment(orderId)`), effectively resuming the workflow.

## Open Questions & Challenges

### How Visualization Works

Since the workflow is an implicit graph defined in code, visualization requires static analysis. A **compiler plugin** would:

1.  Parse the workflow service's source code.
2.  Identify all the remote functions (nodes).
3.  Analyze the body of each function to find all the internal client calls, which represent the edges.
4.  Use this information to render a visual diagram of the workflow.

This approach is inspired by how a GraphQL service, which is also defined by code, can be represented as a graph.

### Other Challenges

1. Loops and conditions are implicit, need special handling in visualization.
2. First class loops, where individual steps is persisted requres additional annotation based design.

