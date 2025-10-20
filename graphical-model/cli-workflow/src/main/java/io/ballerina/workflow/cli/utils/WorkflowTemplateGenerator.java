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

package io.ballerina.workflow.cli.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating workflow templates.
 *
 * @since 0.1.0
 */
public class WorkflowTemplateGenerator {
    
    /**
     * Generate a basic workflow template.
     *
     * @param workflowName Name of the workflow
     * @return Generated workflow template content
     */
    public static String generateWorkflowTemplate(String workflowName) {
        String capitalizedName = capitalizeFirst(workflowName);
        String variableName = toCamelCase(workflowName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            // Auto-generated workflow template
            // Workflow: %s
            // Generated: %s
            
            import hasithaaravinda/workflow;
            
            // --- Node Definitions ---
            
            // StartEvent marks an entry point, triggered externally
            final workflow:Node start%s = {
                kind: "StartEvent",
                description: "Start %s workflow",
                inputs: [
                    { name: "data", type: %sData }
                ],
                output: json
            };
            
            // Activity nodes represent standard synchronous tasks
            final workflow:Node process = {
                kind: "Activity",
                description: "Process the workflow data",
                output: json
            };
            
            final workflow:Node complete = {
                kind: "Activity",
                description: "Complete the workflow"
            };
            
            // --- Edge Definitions ---
            final workflow:Edge edge1 = {
                startNode: start%s,
                endNode: process
            };
            
            final workflow:Edge edge2 = {
                startNode: process,
                endNode: complete
            };
            
            // --- Main Workflow Descriptor ---
            public final workflow:WorkflowModelDescriptor %s = {
                name: "%s",
                description: "%s workflow model",
                nodes: {
                    "start%s": start%s,
                    "process": process,
                    "complete": complete
                },
                edges: [edge1, edge2]
            };
            
            // TODO: Define additional types as needed
            // Example:
            // public type %sData record {|
            //     string id;
            //     string status;
            // |};
            """, 
            capitalizedName,
            timestamp,
            capitalizedName,
            capitalizedName,
            capitalizedName,
            capitalizedName,
            variableName,
            capitalizedName,
            capitalizedName,
            capitalizedName,
            capitalizedName,
            capitalizedName
        );
    }
    
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace(" ", "");
    }
    
    private static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(capitalizeFirst(words[i]));
        }
        return result.toString();
    }
}