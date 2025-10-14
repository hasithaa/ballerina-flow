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

# Defines the workflow node structure
public type WorkflowNode record {
    string id;
    string name;
    string 'type;
    map<anydata> properties?;
    WorkflowConnection[] connections?;
};

# Defines the workflow connection structure
public type WorkflowConnection record {
    string sourceId;
    string targetId;
    string label?;
};

# Defines the workflow graph structure
public type WorkflowGraph record {
    string id;
    string name;
    string description?;
    WorkflowNode[] nodes;
    WorkflowConnection[] connections;
};

# Execute a workflow graph
#
# + graph - The workflow graph to execute
# + return - Result of the execution or error
public function executeWorkflow(WorkflowGraph graph) returns anydata|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowExecutor",
    name: "execute"
} external;

# Validate a workflow graph
#
# + graph - The workflow graph to validate
# + return - True if valid, error otherwise
public function validateWorkflow(WorkflowGraph graph) returns boolean|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowValidator",
    name: "validate"
} external;

# Parse workflow from JSON
#
# + jsonStr - JSON string representation of the workflow
# + return - Parsed workflow graph or error
public function parseWorkflowFromJson(string jsonStr) returns WorkflowGraph|error = @java:Method {
    'class: "io.ballerina.workflow.WorkflowParser",
    name: "parseFromJson"
} external;