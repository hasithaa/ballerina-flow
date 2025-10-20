/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.workflow.cli.generators;

import io.ballerina.workflow.cli.utils.WorkflowModel;
import io.ballerina.workflow.cli.utils.WorkflowModel.WorkflowNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generator for workflow client code.
 *
 * @since 0.1.0
 */
public class ClientGenerator {
    
    private final WorkflowModel model;
    
    public ClientGenerator(WorkflowModel model) {
        this.model = model;
    }
    
    /**
     * Generate the complete client code.
     *
     * @return Generated client code
     */
    public String generate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            // Auto-generated workflow client
            // Workflow: %s
            // Generated: %s
            
            import ballerina/workflow;
            
            # Generated workflow client for %s
            public client class %sWorkflowClient {
                
                private %sWorkflow workflow;
                private workflow:MemoryProvider provider;
                
                # Initialize the workflow client
                #
                # + workflow - Implementation of the workflow interface
                # + provider - Memory provider for workflow persistence
                public function init(%sWorkflow workflow, workflow:MemoryProvider provider) {
                    self.workflow = workflow;
                    self.provider = provider;
                }
                
                %s
                
                %s
                
                # Get workflow status
                #
                # + workflowId - The workflow instance ID
                # + return - Current status of the workflow or error
                public function getWorkflowStatus(string workflowId) returns string|error {
                    // TODO: Implement status retrieval
                    return "RUNNING";
                }
                
                # Get workflow variables
                #
                # + workflowId - The workflow instance ID
                # + return - Current variables of the workflow or error
                public function getWorkflowVariables(string workflowId) returns Results|error {
                    // TODO: Implement variable retrieval
                    return {};
                }
                
                # Get workflow execution history
                #
                # + workflowId - The workflow instance ID
                # + return - Execution history or error
                public function getWorkflowHistory(string workflowId) returns string[]|error {
                    // TODO: Implement history retrieval
                    return [];
                }
            }
            """,
            model.getName(),
            timestamp,
            model.getName(),
            model.getCapitalizedName(),
            model.getCapitalizedName(),
            model.getCapitalizedName(),
            generateStartEventMethods(),
            generateEventMethods()
        );
    }
    
    /**
     * Generate types and context definitions.
     *
     * @return Generated types code
     */
    public String generateTypes() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            // Auto-generated workflow types
            // Workflow: %s
            // Generated: %s
            
            import ballerina/workflow;
            
            %s
            
            %s
            
            %s
            """,
            model.getName(),
            timestamp,
            generateWorkflowInterface(),
            generateResultsType(),
            generateContextType()
        );
    }
    
    private String generateStartEventMethods() {
        List<WorkflowNode> startNodes = model.getStartEventNodes();
        
        return startNodes.stream()
            .map(this::generateStartEventMethod)
            .collect(Collectors.joining("\n\n                "));
    }
    
    private String generateStartEventMethod(WorkflowNode node) {
        String methodName = findNodeId(node);
        String inputParams = generateInputParameters(node);
        String returnType = node.hasOutput() ? node.getOutputType() : "string";
        
        return String.format("""
            # Start %s workflow
            #
            %s
            # + return - Workflow instance ID or error
            public remote function %s(%s) returns %s|error {
                // TODO: Implement workflow start logic
                return "workflow-instance-" + (check int:random(1000, 9999)).toString();
            }""",
            methodName,
            generateInputDocumentation(node),
            methodName,
            inputParams,
            returnType
        );
    }
    
    private String generateEventMethods() {
        List<WorkflowNode> eventNodes = model.getEventNodes();
        
        return eventNodes.stream()
            .map(this::generateEventMethod)
            .collect(Collectors.joining("\n\n                "));
    }
    
    private String generateEventMethod(WorkflowNode node) {
        String methodName = findNodeId(node);
        String inputParams = generateInputParameters(node);
        
        return String.format("""
            # Send %s event to workflow
            #
            # + workflowId - The workflow instance ID
            %s
            # + return - Error if operation fails
            public remote function %s(string workflowId%s) returns error? {
                // TODO: Implement event handling logic
            }""",
            methodName,
            generateInputDocumentation(node),
            methodName,
            inputParams.isEmpty() ? "" : ", " + inputParams
        );
    }
    
    private String generateWorkflowInterface() {
        StringBuilder interface_ = new StringBuilder();
        interface_.append(String.format("# Interface for %s workflow implementation\n", model.getCapitalizedName()));
        interface_.append(String.format("public type %sWorkflow object {\n", model.getCapitalizedName()));
        
        // Add methods for StartEvent and Activity nodes
        model.getNodes().forEach((id, node) -> {
            if ("StartEvent".equals(node.getKind()) || "Activity".equals(node.getKind())) {
                String inputParams = generateFunctionParameters(node);
                String returnType = node.hasOutput() ? node.getOutputType() : "error?";
                
                interface_.append(String.format("    public function %s(Context ctx%s) returns %s;\n", 
                    id, 
                    inputParams.isEmpty() ? "" : ", " + inputParams,
                    returnType));
            }
        });
        
        // Add condition methods
        model.getUniqueConditions().forEach(condition -> {
            interface_.append(String.format("    public function %s(Context ctx) returns boolean|error;\n", condition));
        });
        
        interface_.append("};");
        return interface_.toString();
    }
    
    private String generateResultsType() {
        StringBuilder results = new StringBuilder();
        results.append("# Results record containing outputs from workflow nodes\n");
        results.append("public type Results readonly & record {|\n");
        
        // Add fields for nodes with outputs
        model.getNodes().forEach((id, node) -> {
            if (node.hasOutput()) {
                results.append(String.format("    %s %s?;\n", node.getOutputType(), id));
            }
        });
        
        // Add fields for conditions
        model.getUniqueConditions().forEach(condition -> {
            results.append(String.format("    boolean %s?;\n", condition));
        });
        
        results.append("|};");
        return results.toString();
    }
    
    private String generateContextType() {
        return String.format("""
            # Context object passed to workflow functions
            public type Context record {|
                readonly string workflowId;
                readonly string nodeId;
                Results results;
            |};""");
    }
    
    private String generateInputParameters(WorkflowNode node) {
        if (node.getInputs() == null || node.getInputs().isEmpty()) {
            return "";
        }
        
        return node.getInputs().stream()
            .map(input -> input.getType() + " " + input.getName())
            .collect(Collectors.joining(", "));
    }
    
    private String generateFunctionParameters(WorkflowNode node) {
        return generateInputParameters(node);
    }
    
    private String generateInputDocumentation(WorkflowNode node) {
        if (node.getInputs() == null || node.getInputs().isEmpty()) {
            return "";
        }
        
        return node.getInputs().stream()
            .map(input -> String.format("# + %s - %s parameter", input.getName(), input.getType()))
            .collect(Collectors.joining("\n            "));
    }
    
    private String findNodeId(WorkflowNode node) {
        return model.getNodes().entrySet().stream()
            .filter(entry -> entry.getValue() == node)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("unknown");
    }
}