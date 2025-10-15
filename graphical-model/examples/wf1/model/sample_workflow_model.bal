import hasithaaravinda/workflow;

// Sample workflow model definition
public final workflow:WorkflowModelDescriptor sampleWorkflow = {
    name: "OrderProcessing",
    description: "A sample order processing workflow",
    nodes: {
        "validate": {
            node: "Activity",
            description: "Validate incoming order"
        },
        "payment": {
            node: "Activity", 
            description: "Process payment"
        },
        "fulfill": {
            node: "Activity",
            description: "Fulfill order"
        }
    },
    edges: [
        {
            startNode: "validate",
            endNode: "payment"
        },
        {
            startNode: "payment", 
            endNode: "fulfill"
        }
    ]
};