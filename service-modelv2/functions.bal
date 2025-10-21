import ballerina/io;
import workflow.workflow;

@workflow:Activity
public function processPayment(string orderId) returns boolean|error {
    io:println("ACTIVITY: Processing payment for " + orderId);
    // ... logic to call a real payment gateway ...
    if orderId == "fail-me" {
        return error("Payment gateway timed out");
    }
    io:println("ACTIVITY: Payment successful");
    return true;
}

@workflow:Activity
public function shipOrder(string orderId) returns error? {
    io:println("ACTIVITY: Shipping order " + orderId);
    // ... logic to call a shipping API ...
    io:println("ACTIVITY: Shipping complete");
    return;
}