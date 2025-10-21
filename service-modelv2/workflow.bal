import workflow.workflow;

import ballerina/io;

listener workflow:WorkflowEngine workflowEngine = check new (check new workflow:RdbmsProvider());
final OrderWorkflowClient orderWorkflowClient = check new (workflowEngine);

service "OrderWorkflow" on workflowEngine {

    // 1. WORKFLOW STATE (rebuilt on replay)
    private string[] items = [];
    private boolean isApproved = false;
    private boolean isRejected = false;
    private string status = "PENDING_PAYMENT";

    @workflow:StartEvent
    remote function processOrder(@workflow:Correlation string orderId) returns string|error {

        // This call is deterministic. On replay, it provides the
        // saved result without re-running the function. Compiler will
        // optimize this call. something like loadState("processPayment") ?: processPayment();
        boolean|error paymentResult = processPayment(orderId);

        if paymentResult is error {
            self.status = "PAYMENT_FAILED";
            return "PAYMENT_FAILED";
        }

        self.status = "PENDING_APPROVAL";
        io:println("WORKFLOW: Waiting for human approval...");

        // 3. THE AWAIT
        // The workflow pauses here (thread is released)
        // and waits for the condition to be true.
        // If boolean, this will be wrapped in to a closure. Need to see if we can make it work without closures.
        check workflow:await(self.isApproved || self.isRejected);

        // 4. RESUMPTION
        // Code continues here after a signal is received.
        if self.isRejected {
            self.status = "REJECTED";
            return "REJECTED";
        }

        // Schedule a wait for 2 hours before shipping
        check workflow:sleep({hours: 2});

        self.status = "SHIPPING";
        io:println("WORKFLOW: Order approved, shipping...");

        // Execute the final activity
        // This call is deterministic. On replay, it provides the
        // saved result without re-running the function. Compiler will
        // optimize this call. something like loadState("shipOrder") ?: shipOrder();
        check shipOrder(orderId);

        self.status = "COMPLETED";
        return "COMPLETED";
    }

    // 5. EVENT HANDLERS

    // SIGNAL (asynchronous, fire-and-forget)
    @workflow:Signal
    remote function approve(boolean approvalStatus) {
        if approvalStatus {
            self.isApproved = true;
        } else {
            self.isRejected = true;
        }
    }

    // UPDATE (synchronous, read-write, returns value)
    @workflow:Update
    remote function addItem(string newItem) returns int {
        self.items.push(newItem);
        return self.items.length();
    }

    // QUERY (synchronous, read-only, returns value)
    @workflow:Query
    remote function getStatus() returns string {
        return self.status;
    }
}
