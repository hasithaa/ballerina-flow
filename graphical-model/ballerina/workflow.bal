// Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

# Memory provider interface for workflow persistence
public type MemoryProvider object {
    // Methods for storing and retrieving workflow state
};

# In-memory provider implementation
public class InMemoryProvider {
    *MemoryProvider;
    
    public function init() {
        // Initialize in-memory storage
    }
};

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Workflow Execution Context
/////////////////////////////////////////////////////////////////////////////////////////////////////

# Workflow Context - Contains all the runtime information for workflow execution
#
# + id - Unique identifier for the workflow instance
# + model - The workflow model name
# + inputs - Contains all the input variables defined in the workflow (readonly)
# + results - Contains all the variables (Node output) variables in the workflow (readonly)
# + variables - Contains a property bag to store any additional variables needed during execution
public type WorkflowContext record {|
    string id;
    string model;
    readonly map<anydata> results;
|};

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Workflow Execution APIs
/////////////////////////////////////////////////////////////////////////////////////////////////////

# Create a new workflow instance from a workflow model descriptor
#
# + model - The workflow model descriptor
# + inputs - Initial input variables for the workflow
# + return - A new workflow context instance or error
public function createWorkflowInstance(WorkflowModelDescriptor model, map<anydata> inputs = {}) returns WorkflowContext|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "createInstance"
} external;

# Execute a workflow step for a given node
#
# + context - The workflow context
# + nodeId - The identifier of the node to execute
# + return - Updated workflow context or error
public function executeWorkflowStep(WorkflowContext context, string nodeId) returns WorkflowContext|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "executeStep"
} external;

# Execute a complete workflow model
#
# + model - The workflow model descriptor to execute
# + inputs - Initial input variables for the workflow
# + return - Final workflow context with results or error
public function executeWorkflow(WorkflowModelDescriptor model, map<anydata> inputs = {}) returns WorkflowContext|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "execute"
} external;

# Get the next executable nodes in the workflow
#
# + context - The workflow context
# + return - Array of node identifiers that can be executed next
public function getNextNodes(WorkflowContext context) returns string[]|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "getNextNodes"
} external;

# Check if the workflow execution is complete
#
# + context - The workflow context
# + return - True if workflow is complete, false otherwise
public function isWorkflowComplete(WorkflowContext context) returns boolean|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "isComplete"
} external;

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Workflow Validation APIs
/////////////////////////////////////////////////////////////////////////////////////////////////////

# Validate a workflow model descriptor
#
# + model - The workflow model descriptor to validate
# + return - True if valid, error otherwise
public function validateWorkflowModel(WorkflowModelDescriptor model) returns boolean|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowValidator",
    name: "validate"
} external;

# Validate workflow node connectivity
#
# + model - The workflow model descriptor to validate
# + return - True if all nodes are properly connected, error otherwise
public function validateNodeConnectivity(WorkflowModelDescriptor model) returns boolean|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowValidator",
    name: "validateConnectivity"
} external;

# Validate workflow node types and properties
#
# + model - The workflow model descriptor to validate
# + return - True if all node types are valid, error otherwise
public function validateNodeTypes(WorkflowModelDescriptor model) returns boolean|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowValidator",
    name: "validateNodeTypes"
} external;

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Workflow Code Generation APIs
/////////////////////////////////////////////////////////////////////////////////////////////////////

# Generate workflow client code from a workflow model (used by compiler plugin)
#
# + model - The workflow model descriptor
# + clientName - Name of the generated client class
# + return - Generated Ballerina client code or error
public function generateWorkflowClient(WorkflowModelDescriptor model, string clientName) returns string|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "generateWorkflowClient"
} external;

# Get workflow model metadata for code generation
#
# + model - The workflow model descriptor
# + return - Metadata map with code generation information
public function getWorkflowMetadata(WorkflowModelDescriptor model) returns map<string>|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser", 
    name: "getWorkflowMetadata"
} external;

# Create an empty workflow model template for code generation
#
# + name - The workflow name
# + description - Optional description
# + return - Empty workflow model template
public function createWorkflowTemplate(string name, string? description = ()) returns WorkflowModelDescriptor|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "createWorkflowTemplate"
} external;

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Workflow Utility APIs
/////////////////////////////////////////////////////////////////////////////////////////////////////

# Get all nodes of a specific type from the workflow model
#
# + model - The workflow model descriptor
# + nodeType - The type of nodes to retrieve ("Activity", "Event", "While", "ForEach")
# + return - Map of node identifiers to nodes of the specified type
public function getNodesByType(WorkflowModelDescriptor model, string nodeType) returns map<Node>|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "getNodesByType"
} external;

# Find all incoming edges for a specific node
#
# + model - The workflow model descriptor
# + nodeId - The identifier of the node
# + return - Array of edges that end at the specified node
public function getIncomingEdges(WorkflowModelDescriptor model, string nodeId) returns Edge[]|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "getIncomingEdges"
} external;

# Find all outgoing edges from a specific node
#
# + model - The workflow model descriptor
# + nodeId - The identifier of the node
# + return - Array of edges that start from the specified node
public function getOutgoingEdges(WorkflowModelDescriptor model, string nodeId) returns Edge[]|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "getOutgoingEdges"
} external;