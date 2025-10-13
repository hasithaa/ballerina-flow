public type WorkflowModelDescriptor record {|
    string name;
    string description?;
    map<Node> nodes;
    Edge[] edges;
|};

public type Node ActivityNode|EventNode|WhileNode|ForEachNode;

// User defined nodes
public type ActivityNode record {|
    "Activity" node;
    string description?;
    typedesc outputType = typeof ();
    GraphicalMetadata graphical?;
|};

// Event Node - Waits for an external event to occur
public type EventNode record {|
    "Event" node;
    string description?;
    map<typedesc> variables;
    map<string> correlationProperties;
    typedesc outputType = typeof ();
    GraphicalMetadata graphical?;
|};

public type Edge record {|
    string startNode;
    string endNode;
    Condition condition?;
    GraphicalMetadata graphical?;
|};

// Extend Edge with onEvents

public type Condition record {|
    string name;
    isolated function? ref = ();
    string description?;
    GraphicalMetadata graphical?;
|};

public type WhileNode record {|
    string description?;
    Condition condition;
    map<Node> nodes;
    GraphicalMetadata graphical?;
|};

public type ForEachNode record {|
    string description?;
    typedesc<map<anydata>|anydata[]> collectionType;
    string iteratorVariable;
    map<Node> nodes;
    GraphicalMetadata graphical?;
|};

public type WorkflowModel distinct object {};

public type GraphicalMetadata record {|
    float x;
    float y;
    float width?;
    float height?;
    string label?;
    string color?;
    string lineColor?;
    string lineStyle?;
    string icon?;
    string impl?;
    json...;
|};

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Generated Types. 
/////////////////////////////////////////////////////////////////////////////////////////////////////
// 1. WorkflowContext
// id - Unique identifier for the workflow instance. (string)
// model - The workflow model name. (string)
// inputs - Contains all the input variables defined in the workflow. (Readonly) 
// results - Contains all the variables (Node output) variables in the workflow. (Readonly) (if node executes only once, type is T, if multiple times possible type is T[])
// variables - Contain a property bag to store any additional variables needed during execution. (Untyped map) 
