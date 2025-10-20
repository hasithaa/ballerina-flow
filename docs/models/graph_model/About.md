# The Functional Graph Model

This approach is a highly improved developer experience over a simple "Workflow as a Library" model (Option 5). Instead of forcing developers to grapple with a complex runtime API, the complexity is shifted to the modeling and code generation phases. The result is a much simpler and more intuitive implementation process.


## How Workflows Fit into the Ballerina Ecosystem

Historically, workflow engines like WSO2 BPS were separate runtime environments dedicated to executing processes. To interact with the outside world, they were almost always fronted by an ESB, creating a "sandwich architecture" (`Client <-> ESB <-> BPS`). This design meant protocols were handled in one system and workflow logic in another, increasing deployment and operational complexity.

This separation was once necessary because it simplified the workflow engine's design, allowing it to focus solely on process execution features like a simple persistence boundary, protocol-independent interruptibility, and data-based correlation.

In contrast, Ballerina is designed to be **both the ESB and the workflow host**. A single Ballerina integration can handle both protocol transformations and stateful workflow execution. This new design finds a middle ground, simplifying the overall architecture while still maintaining a clear separation of concerns within the same runtime.

A key design goal is to allow workflow integrations to co-exist with standard services, remaining independent of specific protocols or event sources.


## The Developer Experience

With a pure "Workflow as a Library" approach, developers must learn and use a complex, imperative API to define and execute workflows, which often leads to errors and a steep learning curve. The new approach provides a better middle ground by using a declarative model to define the workflow's structure, which is then used to generate easy-to-use, type-safe code.

A developer follows a clear, four-step process:

1.  **Model**: Define the workflow's structure—its nodes, edges, and variables—declaratively in a dedicated `.bal` file.
2.  **Generate**: Use a Ballerina tool to process the model and generate a typed interface, a context object, and a dedicated client.
3.  **Implement**: Write the business logic for each node by implementing the generated interface, giving you full control over the code.
4.  **Integrate**: Use the generated client in other artifacts (like HTTP services) to start and interact with the workflow.

Excellent. This section provides the concrete "how-to" for the modeling step. I've polished the text and added explanations to highlight the key concepts you're showcasing in the code, without simplifying the code itself.

-----

### Step 1: Defining the Workflow Model

The workflow model is the single source of truth for the flow's structure. It is a declarative definition that serves as the blueprint for the code generation and runtime execution of the workflow.

  - It's defined in a `.bal` file inside the `workflows/` directory (e.g., `workflows/order_processing.bal`).
  - Each file must contain a `final` variable of type `workflow:WorkflowModelDescriptor`.
  - This descriptor contains the workflow's name, nodes, edges, and variables.

The following example for an `OrderProcessing` workflow demonstrates how these components are defined in practice.

#### Example: `workflows/order_processing.bal`

```ballerina
import ballerina/workflow;
import my_project/commons;

// --- Node Definitions ---

// 'StartEvent' marks an entry point, triggered externally.
final workflow:Node startOrder = {
    kind: "StartEvent",
    description: "Validate incoming order",
    inputs: [
        { name: "data", type: Order }
    ],
    output: Order
};

// 'Event' nodes pause the workflow to wait for an external message.
final workflow:Node payment = {
    kind: "Event",
    description: "Process payment",
    inputs: [
        { name: "paymentInfo", type: json }
    ]
};

// 'Activity' nodes are standard synchronous tasks for business logic.
final workflow:Node fulfill = {
    kind: "Activity",
    description: "Fulfill order"
};

final workflow:Node reject = {
    kind: "Activity",
    description: "Reject order"
};

// --- Templated Activity Nodes ---

// Templates like 'Agent' hint to the code generator what kind of
// implementation structure to create.
final workflow:Node agentApprove = {
    kind: "Activity",
    template: "Agent",
    description: "Approve order",
    output: boolean
};

// The 'callFunction' template directly invokes an existing function.
final workflow:Node sendEmail = {
    kind: "Activity",
    template: "callFunction",
    description: "Send email notification",
};


// --- Edge Definitions ---
final workflow:Edge edge0 = {
    startNode: startOrder,
    endNode: agentApprove
};
final workflow:Edge edge1 = {
    startNode: agentApprove,
    endNode: payment,
    condition: "isValid"
};
final workflow:Edge edge2 = {
    startNode: agentApprove,
    endNode: reject,
    condition: "isInvalid"
};
final workflow:Edge edge3 = {
    startNode: payment,
    endNode: fulfill
};
final workflow:Edge edge4 = {
    startNode: payment,
    endNode: sendEmail
};

// --- Main Workflow Descriptor ---
public final workflow:WorkflowModelDescriptor sampleWorkflow = {
    name: "OrderProcessing",
    description: "A sample order processing workflow",
    nodes: { "startOrder": startOrder, "agentApprove": agentApprove, "payment": payment, "fulfill": fulfill, "reject": reject, "sendEmail": sendEmail },
    edges: [ edge0, edge1, edge2, edge3, edge4 ]
};

public type Order record {|
    string orderId;
    decimal amount;
|};
```

#### Understanding the Node Kinds

The `node` field in each node definition is critical, as it specifies the node's behavioral type. The example above showcases several kinds:

  - `StartEvent`: This marks an official entry point for the workflow. The code generator will create a corresponding method in the client.
  - `Event`: This represents a point where the workflow pauses and waits for an external message. The generator will create a method in the client to receive this event and resume the flow.
  - `Activity`: This is the most common type, representing a standard, synchronous task where your business logic is executed. The generator will create a function skeleton for you to implement.
    - **Templated Activities** (`Agent`, `callFunction`, etc.): These are specialized activity types that act as **hints** for the code generator.
        - A `callFunction` node, like `sendEmail`, instructs the tool to directly wire up a call to an existing, reusable function (`ref: commons:sendEmail`) instead of generating a new skeleton.
        - An `Agent` or `HumanTask` node could hint to the generator to create a function skeleton with pre-built boilerplate for interacting with an AI agent or a human task service.

We will discuss more node kinds and templates in future iterations.

## Step 2: Understanding the Generated Code

After the model is defined, a tool processes it to generate a Ballerina sub-module containing type-safe code. This bridges the declarative model and the imperative user logic.

#### Generated Interface (The Skeleton)

The tool generates an `object` type that acts as an interface. The developer's implementation logic **must** conform to this contract.

```ballerina
// This defines the contract for your implementation class.
type OrderProcessingWorkflow object {
    // A function for each Activity and StartEvent node.
    public function startOrder(Context ctx, Order data) returns string|error;
    public function agentApprove(Context ctx) returns boolean|error;
    public function payment(Context ctx, json paymentInfo) returns error?;
    public function fulfill(Context ctx) returns error?;
    public function reject(Context ctx) returns error?;
    public function sendEmail(Context ctx) returns error?;

    // A function for each unique condition on the edges.
    public function isValid(Context ctx) returns boolean|error;
    public function isInvalid(Context ctx) returns boolean|error;
};
```

#### Generated Context and Variables

The tool generates records to hold the workflow's state and variables, ensuring type safety.

```ballerina
// A typed record for results from nodes that produce outputs. immutable.
type Results readonly & record {|
    Order startOrder?;
    boolean agentApprove?;
    boolean isValid?;
    boolean isInvalid?;
|};

// The context object passed to every function, providing
// access to the instance ID and results.
type Context record {|
    readonly string workflowId; // The unique instance ID
    readonly string id;
    Results results;
|};
```

We may not support `Variables` in the first version. The idea is that variables are just node outputs, and nodes can read other node outputs as inputs.

#### Generated Workflow Client

A dedicated client is generated for interacting with the workflow from the outside.

```ballerina
client class OrderProcessingWorkflowClient {
    // The constructor takes the user's implementation and runtime configs.
    public function init(OrderProcessingWorkflow workflow, workflow:MemoryProvider provider) {}

    // A remote method for each 'StartEvent' node.
    // Returns the unique workflow instance ID.
    public remote function startOrder(Order data) returns string|error;

    // A remote method for each 'Event' node to receive triggers.
    public remote function payment(string workflowId, json paymentInfo) returns error?;

    // Standard methods to manage and query instances.
    public function getWorkflowStatus(string workflowId) returns string|error;
    public function getWorkflowVariables(string workflowId) returns Variables|error;
    public function getWorkflowHistory(string workflowId) returns string[]|error;
}
```

## Step 3: Implementing the Business Logic

The developer now implements the generated `OrderProcessingWorkflow` interface. The tool can generate a skeleton `class` to get started.

```ballerina
// The user fills in the logic for each step.
class OrderProcessingWorkflowImpl {
    *OrderProcessingWorkflow; // Implements the generated interface

    public function startOrder(Context ctx, Order data) returns string {
        // User implementation here...
        io:println("Started order processing for: ", data.orderId);
        return "Order started successfully";
    }

    public function fulfill(Context ctx) returns void {
        // User implementation here...
        io:println("Fulfilling order for: ", ctx.results.startOrder?.orderId ?: "unknown");
    }

    public function reject(Context ctx) returns void {
        // User implementation here...
    }

    // Implementation for the edge conditions.
    public function isValid(Context ctx) returns boolean {
        return ctx.results.startOrder?.amount ?: 0 > 1000;
    }

    public function isInvalid(Context ctx) returns boolean {
        return !self.isValid(ctx);
    }
}
```

## Step 4: Integrating and Running the Workflow

Finally, the generated client is used to embed the workflow in other parts of the system, like an HTTP service.

**Runtime configuration, such as the persistence provider, is injected here during client initialization.** This answers the `TODO`: memory and persistence are runtime concerns, not part of the static model.

```ballerina
// Create the workflow client, injecting the implementation and a memory provider.
// This could be InMemoryProvider, DatabaseProvider, etc.
OrderProcessingWorkflowClient orderMgtWorkflow = check new(new OrderProcessingWorkflowImpl(), new workflow:InMemoryProvider());

// Use the client within an HTTP service.
service /order on new http:Listener(8080) {

    // The 'startOrder' resource calls the client's start method.
    resource function post start(Order order) returns json|error {
        string workflowId = check orderMgtWorkflow->startOrder(order);
        return { instanceId: workflowId };
    }

    // The 'payment' resource sends an event to a running workflow instance.
    resource function post payment(json paymentInfo, string instanceId) returns error? {
        check orderMgtWorkflow->payment(instanceId, paymentInfo);
    }
}
```

