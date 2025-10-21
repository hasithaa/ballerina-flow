import ballerina/http;


service /api on new http:Listener(9090) {

    // --- START a new workflow ---
    resource function post 'order/[string orderId]() returns http:Accepted|error {

        // Start the workflow (asynchronous)
        _ = check orderWorkflowClient->processOrder(orderId);
        return http:ACCEPTED;
    }

    // --- SIGNAL a workflow (fire-and-forget) ---
    resource function post approve/[string orderId](@http:Payload json payload) returns http:Accepted|error {
        boolean isApproved = check payload.approved.ensureType();

        // Send the signal (asynchronous)
        check orderWorkflowClient->approve(orderId,isApproved);
        return http:ACCEPTED;
        }

    // --- UPDATE a workflow (sync, read-write) ---
    resource function post item/[string orderId](@http:Payload json payload) returns int|error {
        string item = check payload.item.ensureType();

        // Call the update and wait for the response
        int newCount = check orderWorkflowClient->addItem(orderId, item);
        return newCount;
    }

    // --- QUERY a workflow (sync, read-only) ---
    resource function get status/[string orderId]() returns string|error {

        // Call the query and wait for the response
        string status = check orderWorkflowClient->getStatus(orderId);
        return status;
    }
}