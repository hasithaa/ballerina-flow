# Workflow CLI Tool

This enhanced CLI tool provides comprehensive workflow management capabilities for Ballerina workflow projects.

## Commands

### `workflow new <name>`
Creates a new workflow model file under the `workflows/` directory.

**Usage:**
```bash
workflow new "Order Processing"
```

**What it does:**
- Creates a `workflows/` directory if it doesn't exist
- Generates a new `.bal` file with a basic workflow template
- Includes sample nodes (StartEvent, Activity) and edges
- Provides a complete `WorkflowModelDescriptor` structure

**Example output:**
```
Creating new workflow: Order Processing
========================================
Created workflows directory
Successfully created workflow model:
   File: /path/to/project/workflows/order_processing.bal

Next steps:
   1. Edit order_processing.bal to define your workflow structure
   2. Run 'workflow gen Order Processing' to generate client code
```

### `workflow gen <name>`
Generates Ballerina client code from a workflow model.

**Usage:**
```bash
workflow gen "Order Processing"
workflow gen "Order Processing" --create-impl
workflow gen "Order Processing" --update-impl
```

**Options:**
- `--create-impl`: Creates a `workflow.bal` implementation file with basic method stubs
- `--update-impl`: Updates existing `workflow.bal` implementation file, adding only new methods

**What it does:**
- Parses the workflow model from `workflows/<name>.bal`
- Generates client code in `generated/<name>/`
- Creates type-safe interfaces and context objects
- Optionally generates or updates implementation files

**Generated Files:**
- `generated/<name>/client.bal` - Workflow client with remote methods for StartEvent and Event nodes
- `generated/<name>/types.bal` - Type definitions, interfaces, and context records
- `workflow.bal` - Implementation skeleton (when using `--create-impl` or `--update-impl`)

**Example output:**
```
Generating code for workflow: Order Processing
==========================================
Parsing workflow model...
Generating client code...
Generated client code:
   /path/to/project/generated/order_processing
   client.bal
   types.bal
Creating implementation file...
Created workflow.bal with basic implementation

Integration example:
   import generated.order_processing;

   OrderprocessingWorkflowClient client = check new(new OrderprocessingWorkflowImpl(), new workflow:InMemoryProvider());
   string workflowId = check client->startOrderprocessing(...);
```

## Generated Code Structure

### Client Code (`client.bal`)
- **WorkflowClient class**: Main client for workflow interaction
- **Remote methods**: For StartEvent and Event nodes (e.g., `startOrderprocessing()`, `sendPayment()`)
- **Management methods**: `getWorkflowStatus()`, `getWorkflowVariables()`, `getWorkflowHistory()`
- **Type-safe**: All methods are generated based on the workflow model structure

### Type Definitions (`types.bal`)
- **Workflow interface**: Contract that implementations must follow
- **Results record**: Contains outputs from all workflow nodes
- **Context record**: Passed to all workflow functions, contains instance ID and results
- **Input/Output types**: Based on node definitions in the model

### Implementation File (`workflow.bal`)
- **Implementation class**: Skeleton implementation of the workflow interface
- **Method stubs**: For each Activity and StartEvent node
- **Condition methods**: For edge conditions in the workflow
- **Error handling**: Default implementations return "Not implemented" errors

## Development Workflow

1. **Create**: `workflow new "My Workflow"`
2. **Model**: Edit the generated workflow model file
3. **Generate**: `workflow gen "My Workflow" --create-impl`
4. **Implement**: Fill in the business logic in `workflow.bal`
5. **Update**: `workflow gen "My Workflow" --update-impl` (when adding new nodes)
6. **Integrate**: Use the generated client in your Ballerina services

## Example Integration

```ballerina
import generated.order_processing;

// Create workflow client
OrderprocessingWorkflowClient orderWorkflow = check new(
    new OrderprocessingWorkflowImpl(), 
    new workflow:InMemoryProvider()
);

// In an HTTP service
service /order on new http:Listener(8080) {
    resource function post start(Order order) returns json|error {
        string workflowId = check orderWorkflow->startOrderprocessing(order);
        return { instanceId: workflowId };
    }
    
    resource function post payment(json paymentInfo, string instanceId) returns error? {
        check orderWorkflow->sendPayment(instanceId, paymentInfo);
    }
}
```

## Building and Installation

```bash
# Build the CLI tool
./gradlew :workflow-cli:build

# Create distribution
./gradlew :workflow-cli:shadowJar

# Use the CLI (from project root)
java -jar cli-workflow/build/libs/workflow-cli-0.1.0-SNAPSHOT.jar <command>

# Or use the generated script (after distribution)
./build/distributions/workflow-cli-0.1.0-SNAPSHOT/bin/workflow-cli <command>
```

## Features

- **Template Generation**: Creates workflow models with proper structure
- **Code Generation**: Type-safe client and interface generation
- **Implementation Support**: Automatic skeleton generation and incremental updates
- **Error Handling**: Comprehensive error messages and validation
- **Integration Ready**: Generated code works seamlessly with Ballerina workflow runtime

## Architecture

The CLI tool consists of several key components:

- **Commands**: `NewCommand` and `GenerateCommand` handle the CLI interface
- **Parsers**: `WorkflowModelParser` extracts workflow structure from `.bal` files
- **Generators**: `ClientGenerator` and `ImplementationGenerator` create the output code
- **Models**: `WorkflowModel` represents the parsed workflow structure
- **Templates**: `WorkflowTemplateGenerator` creates initial workflow files

This design provides a clean separation of concerns and makes the tool extensible for future enhancements.