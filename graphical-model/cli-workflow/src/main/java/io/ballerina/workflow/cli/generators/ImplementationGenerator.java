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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generator for workflow implementation code.
 *
 * @since 0.1.0
 */
public class ImplementationGenerator {
    
    private final WorkflowModel model;
    
    public ImplementationGenerator(WorkflowModel model) {
        this.model = model;
    }
    
    /**
     * Generate complete implementation file.
     *
     * @return Generated implementation code
     */
    public String generateComplete() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            // Workflow implementation for %s
            // Generated: %s
            
            import ballerina/io;
            import generated.%s;
            
            # Implementation of %s workflow
            public class %sWorkflowImpl {
                *%sWorkflow;
                
                %s
                
                %s
            }
            """,
            model.getName(),
            timestamp,
            model.getName().toLowerCase().replace(" ", "_"),
            model.getName(),
            model.getCapitalizedName(),
            model.getCapitalizedName(),
            generateNodeMethods(),
            generateConditionMethods()
        );
    }
    
    /**
     * Update existing implementation by adding missing methods.
     *
     * @param existingContent The current content of the implementation file
     * @return Updated content with new methods added
     */
    public String updateExisting(String existingContent) {
        Set<String> existingMethods = extractExistingMethods(existingContent);
        List<String> newMethods = new ArrayList<>();
        
        // Generate missing node methods
        model.getNodes().forEach((id, node) -> {
            if (("StartEvent".equals(node.getKind()) || "Activity".equals(node.getKind())) 
                && !existingMethods.contains(id)) {
                newMethods.add(generateNodeMethod(id, node));
            }
        });
        
        // Generate missing condition methods
        model.getUniqueConditions().forEach(condition -> {
            if (!existingMethods.contains(condition)) {
                newMethods.add(generateConditionMethod(condition));
            }
        });
        
        if (newMethods.isEmpty()) {
            return existingContent;
        }
        
        // Find the last closing brace and insert new methods before it
        int lastBraceIndex = existingContent.lastIndexOf("}");
        if (lastBraceIndex == -1) {
            return existingContent + "\n\n" + String.join("\n\n    ", newMethods);
        }
        
        String beforeBrace = existingContent.substring(0, lastBraceIndex).trim();
        String newMethodsStr = "    " + String.join("\n\n    ", newMethods);
        
        return beforeBrace + "\n\n" + newMethodsStr + "\n}";
    }
    
    private String generateNodeMethods() {
        return model.getNodes().entrySet().stream()
            .filter(entry -> "StartEvent".equals(entry.getValue().getKind()) || 
                           "Activity".equals(entry.getValue().getKind()))
            .map(entry -> generateNodeMethod(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("\n\n    "));
    }
    
    private String generateNodeMethod(String nodeId, WorkflowNode node) {
        String inputParams = generateFunctionParameters(node);
        String returnType = node.hasOutput() ? node.getOutputType() : "error?";
        String returnStatement = generateReturnStatement(node);
        
        return String.format("""
            # Implementation for %s node
            # %s
            public function %s(Context ctx%s) returns %s {
                // TODO: Implement %s logic
                io:println("Executing %s node for workflow: " + ctx.workflowId);
                
                %s
            }""",
            nodeId,
            node.getDescription() != null ? node.getDescription() : "Process " + nodeId,
            nodeId,
            inputParams.isEmpty() ? "" : ", " + inputParams,
            returnType,
            nodeId,
            nodeId,
            returnStatement
        );
    }
    
    private String generateConditionMethods() {
        return model.getUniqueConditions().stream()
            .map(this::generateConditionMethod)
            .collect(Collectors.joining("\n\n    "));
    }
    
    private String generateConditionMethod(String condition) {
        return String.format("""
            # Evaluate %s condition
            public function %s(Context ctx) returns boolean|error {
                // TODO: Implement %s condition logic
                io:println("Evaluating %s condition for workflow: " + ctx.workflowId);
                
                return error("Not implemented");
            }""",
            condition,
            condition,
            condition,
            condition
        );
    }
    
    private String generateFunctionParameters(WorkflowNode node) {
        if (node.getInputs() == null || node.getInputs().isEmpty()) {
            return "";
        }
        
        return node.getInputs().stream()
            .map(input -> input.getType() + " " + input.getName())
            .collect(Collectors.joining(", "));
    }
    
    private String generateReturnStatement(WorkflowNode node) {
        if (!node.hasOutput()) {
            return "return error(\"Not implemented\");";
        }
        
        String outputType = node.getOutputType();
        if (outputType == null) {
            return "return error(\"Not implemented\");";
        }
        
        // Generate appropriate return value based on type
        switch (outputType.toLowerCase()) {
            case "string":
                return "return \"TODO: implement\";";
            case "int":
            case "integer":
                return "return 0;";
            case "float":
            case "decimal":
                return "return 0.0;";
            case "boolean":
                return "return false;";
            case "json":
                return "return {};";
            default:
                if (outputType.contains("[]")) {
                    return "return [];";
                } else if (outputType.startsWith("map<") || outputType.contains("record")) {
                    return "return {};";
                } else {
                    return "return error(\"Not implemented\");";
                }
        }
    }
    
    private Set<String> extractExistingMethods(String content) {
        Set<String> methods = new HashSet<>();
        
        // Simple regex to find function definitions
        // This is a basic implementation - in production, you'd want proper AST parsing
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public function ")) {
                String methodName = line.substring("public function ".length());
                int parenIndex = methodName.indexOf("(");
                if (parenIndex > 0) {
                    methods.add(methodName.substring(0, parenIndex));
                }
            }
        }
        
        return methods;
    }
}