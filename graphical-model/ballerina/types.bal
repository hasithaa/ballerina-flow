
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
public type Node StartEventNode|EventNode|ActivityNode|WhileNode|ForEachNode;

# Input parameter for a node
#
# + name - name of the input parameter
# + 'type - type descriptor of the input parameter
public type NodeInput record {|
    string name;
    typedesc 'type;
|};

# Start Event Node - Represents an entry point triggered externally
#
# + kind - "StartEvent" node type
# + description - additional description of the start event node
# + inputs - input parameters for the start event
# + output - type of the output produced by the start event
# + graphical - graphical metadata for the node
public type StartEventNode record {|
    "StartEvent" kind;
    string description?;
    NodeInput[] inputs?;
    typedesc output?;
    GraphicalMetadata graphical?;
|};

# Event Node - Represents a point where workflow pauses to wait for external message
# 
# + kind - "Event" node type
# + description - additional description of the event node
# + inputs - input parameters for the event
# + output - type of the output produced by the event
# + graphical - graphical metadata for the node
public type EventNode record {|
    "Event" kind;
    string description?;
    NodeInput[] inputs?;
    typedesc output?;
    GraphicalMetadata graphical?;
|};

# Activity Node - Represents a task or action in the workflow
#
# + kind - "Activity" node type
# + description - additional description of the activity node
# + output - type of the output produced by the activity
# + graphical - graphical metadata for the node
public type ActivityNode record {|
    "Activity" kind;
    string description?;
    typedesc output?;
    GraphicalMetadata graphical?;
|};

# Edge - Represents a directed connection between two nodes in the workflow model
#
# + startNode - reference to the start node
# + endNode - reference to the end node
# + condition - optional condition name to evaluate for traversing this edge
# + graphical - graphical metadata for the edge
public type Edge record {|
    Node startNode;
    Node endNode;
    string condition?;
    GraphicalMetadata graphical?;
|};

# While Node - Represents a while loop in the workflow
# 
# + kind - "While" node type
# + description - additional description of the while node
# + condition - condition name to evaluate for the while loop
# + nodes - map of node identifiers to their corresponding nodes within the while loop
# + graphical - graphical metadata for the while node
public type WhileNode record {|
    "While" kind;
    string description?;
    string condition;
    map<Node> nodes;
    GraphicalMetadata graphical?;
|};

# ForEach Node - Represents a for each loop in the workflow
# 
# + kind - "ForEach" node type
# + description - additional description of the for each node
# + collectionType - type of the collection to iterate over
# + iteratorVariable - name of the variable to use for iterating over the collection
# + nodes - map of node identifiers to their corresponding nodes within the for each loop
# + graphical - graphical metadata for the for each node
public type ForEachNode record {|
    "ForEach" kind;
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