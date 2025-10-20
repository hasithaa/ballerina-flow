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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed workflow model with utilities for code generation.
 *
 * @since 0.1.0
 */
public class WorkflowModel {
    private String name;
    private String description;
    private Map<String, WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    
    public WorkflowModel() {
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
    }
    
    public WorkflowModel(String name, String description) {
        this.name = name;
        this.description = description;
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, WorkflowNode> getNodes() { return nodes; }
    public void setNodes(Map<String, WorkflowNode> nodes) { this.nodes = nodes; }
    
    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }
    
    // Utility methods
    public String getCapitalizedName() {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    public String getCamelCaseName() {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }
    
    public List<WorkflowNode> getStartEventNodes() {
        return nodes.values().stream()
                .filter(node -> "StartEvent".equals(node.getKind()))
                .toList();
    }
    
    public List<WorkflowNode> getEventNodes() {
        return nodes.values().stream()
                .filter(node -> "Event".equals(node.getKind()))
                .toList();
    }
    
    public List<WorkflowNode> getActivityNodes() {
        return nodes.values().stream()
                .filter(node -> "Activity".equals(node.getKind()))
                .toList();
    }
    
    public List<String> getUniqueConditions() {
        return edges.stream()
                .map(WorkflowEdge::getCondition)
                .filter(condition -> condition != null && !condition.isEmpty())
                .distinct()
                .toList();
    }
    
    public void addNode(String id, WorkflowNode node) {
        nodes.put(id, node);
    }
    
    public void addEdge(WorkflowEdge edge) {
        edges.add(edge);
    }
    
    /**
     * Represents a workflow node.
     */
    public static class WorkflowNode {
        private String kind;
        private String description;
        private String outputType;
        private String template;
        private List<NodeInput> inputs;
        
        public WorkflowNode() {
            this.inputs = new ArrayList<>();
        }
        
        // Getters and setters
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getOutputType() { return outputType; }
        public void setOutputType(String outputType) { this.outputType = outputType; }
        
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
        
        public List<NodeInput> getInputs() { return inputs; }
        public void setInputs(List<NodeInput> inputs) { this.inputs = inputs; }
        
        public boolean hasOutput() {
            return outputType != null && !outputType.isEmpty() && !"()".equals(outputType);
        }
    }
    
    /**
     * Represents a workflow edge.
     */
    public static class WorkflowEdge {
        private String startNodeId;
        private String endNodeId;
        private String condition;
        
        public WorkflowEdge() {}
        
        public WorkflowEdge(String startNodeId, String endNodeId, String condition) {
            this.startNodeId = startNodeId;
            this.endNodeId = endNodeId;
            this.condition = condition;
        }
        
        // Getters and setters
        public String getStartNodeId() { return startNodeId; }
        public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
        
        public String getEndNodeId() { return endNodeId; }
        public void setEndNodeId(String endNodeId) { this.endNodeId = endNodeId; }
        
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
    }
    
    /**
     * Represents a node input parameter.
     */
    public static class NodeInput {
        private String name;
        private String type;
        
        public NodeInput() {}
        
        public NodeInput(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}