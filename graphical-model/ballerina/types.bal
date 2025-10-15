
# Workflow Model Descriptor
#
# + name - name of the workflow model  
# + description - additional description of the workflow model  
# + nodes - nodes in the workflow model  
# + edges - edges between the nodes in the workflow model
public type WorkflowModelDescriptor record {|
    string name;
    string description?;
    map<Node> nodes;
    Edge[] edges;
|};

# Represents a node in the workflow model
public type Node ActivityNode|EventNode|WhileNode|ForEachNode;

# Activity Node - Represents a task or action in the workflow
#
# + node - "Activity" node type
# + description - additional description of the activity node
# + outputType - type of the output produced by the activity, excluding errors. Defaults to '()' (no output)
# + graphical - graphical metadata for the node
public type ActivityNode record {|
    "Activity" node;
    string description?;
    typedesc outputType = typeof ();
    GraphicalMetadata graphical?;
|};

# Event Node - Represents an event in the workflow
# 
# + node - "Event" node type
# + description - additional description of the event node
# + variables - map of variable names to their types that the event produces
# + correlationProperties - map of correlation property names to their types for event correlation
# + outputType - type of the output produced by the event, excluding errors. Defaults to '()' (no output)
# + graphical - graphical metadata for the node
public type EventNode record {|
    "Event" node;
    string description?;
    map<typedesc> variables;
    map<string> correlationProperties;
    typedesc outputType = typeof ();
    GraphicalMetadata graphical?;
|};

# Edge - Represents a directed connection between two nodes in the workflow model
#
# + startNode - start node identifier
# + endNode - end node identifier
# + condition - optional condition to evaluate for traversing this edge
# + graphical - graphical metadata for the edge
public type Edge record {|
    string startNode;
    string endNode;
    Condition condition?;
    GraphicalMetadata graphical?;
|};

// TODO: Extend Edge with onEvents

# Condition - Represents a condition in the workflow model
# + name - name of the condition
# + description - additional description of the condition
# + graphical - graphical metadata for the condition
public type Condition record {|
    string name;
    string description?;
    GraphicalMetadata graphical?;
|};

# While Node - Represents a while loop in the workflow
# 
# + description - additional description of the while node
# + condition - condition to evaluate for the while loop
# + nodes - map of node identifiers to their corresponding nodes within the while loop
# + graphical - graphical metadata for the while node
public type WhileNode record {|
    string description?;
    Condition condition;
    map<Node> nodes;
    GraphicalMetadata graphical?;
|};

# ForEach Node - Represents a for each loop in the workflow
# 
# + description - additional description of the for each node
# + collectionType - type of the collection to iterate over
# + iteratorVariable - name of the variable to use for iterating over the collection
# + nodes - map of node identifiers to their corresponding nodes within the for each loop
# + graphical - graphical metadata for the for each node
public type ForEachNode record {|
    string description?;
    typedesc<map<anydata>|anydata[]> collectionType;
    string iteratorVariable;
    map<Node> nodes;
    GraphicalMetadata graphical?;
|};

# Workflow Model - Distinct type to represent a workflow model
public type WorkflowModel distinct object {};

# Graphical Metadata - Optional metadata for graphical representation of nodes and edges
# 
# + x - x-coordinate of the graphical representation
# + y - y-coordinate of the graphical representation
# + width - width of the graphical representation
# + height - height of the graphical representation
# + label - label of the graphical representation
# + color - color of the graphical representation
# + lineColor - color of the line connecting two nodes
# + lineStyle - style of the line connecting two nodes
# + icon - icon of the graphical representation
# + impl - implementation of the graphical representation
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
