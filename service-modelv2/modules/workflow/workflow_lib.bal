public annotation Activity on function;

public annotation StartEvent on service remote function;

public annotation Signal on service remote function;

public annotation Query on service remote function;

public annotation Update on service remote function;

public annotation Correlation on parameter;

public isolated class WorkflowEngine {

    public isolated function init(PersistentProvider provider) {
    }

    public isolated function attach(WorkflowModel svc, string attachPoint) returns error? {
    }

    public isolated function detach(WorkflowModel svc) returns error? {
    }

    public isolated function 'start() returns error? {
    }

    public isolated function gracefulStop() returns error? {
    }

    public isolated function immediateStop() returns error? {
    }

    public isolated function getClient() returns WorkflowEngineClient {
        return new ();
    }

}

public type WorkflowModel distinct service object {};

public type PersistentProvider distinct object {};

public class RdbmsProvider {
    *PersistentProvider;
}

public function await((function () returns boolean)|boolean conditionFunc) returns error? {
    // Implementation for pausing the workflow until the condition is true
}

public function sleep(Duration duration) returns error? {
    // Implementation for sleeping/waiting for the specified duration
}

public type Duration record {
    int months?;
    int days?;
    int hours?;
    int minutes?;
    int seconds?;
    int milliseconds?;
};

public type Execution record {|
    string id;
|};

public isolated class WorkflowEngineClient {

    public isolated function search(string process, map<anydata> cid) returns Execution?|error {
    }

    public isolated function startNew(string workflowName, string methodName, anydata... args) returns Execution|error {
        return error("Not implemented");
    }

    public isolated function signal(Execution execution, string signalName, anydata... args) returns error? {
        return error("Not implemented");
    }

    public isolated function update(Execution execution, string updateName, anydata... args) returns anydata|error {
        return error("Not implemented");
    }

    public isolated function query(Execution execution, string queryName, anydata... args) returns anydata|error {
        return error("Not implemented");
    }

    public isolated function stop(Execution execution) returns error? {
        return error("Not implemented");
    }

    public isolated function getState(Execution execution) returns map<anydata>|error {
        return error("Not implemented");
    }

}
