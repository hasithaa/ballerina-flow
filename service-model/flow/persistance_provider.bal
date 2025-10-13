
public type ErrorDetails record {|
    string message;
    string reason;
    json details?;
|};

public type StepOutput record {|
    json value?;
    ErrorDetails errorDetails?;
|};

public type WorkflowNode record {|
    string id;
    string functionName;
    string? stepCorrelationId;
    json inputs;
    StepOutput? outputs;
    map<json> variables;
    string status;
|};

public type WorkflowGraph record {|
    string workflowId;
    string status;
    WorkflowNode[] nodes;
    map<json> variables;
    int lastUpdated;
|};

public type PersistenceProvider distinct client object {
    remote function save(WorkflowGraph graph) returns error?;
    remote function getByCorrelationId(string workflowId, string? correlationId) returns WorkflowGraph?|error;
    remote function searchByStatus(string status) returns WorkflowGraph[]|error;
};

public class FileBasedPersistenceProvider {

}



