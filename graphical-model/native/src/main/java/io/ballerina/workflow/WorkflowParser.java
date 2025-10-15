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

package io.ballerina.workflow;

import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow code generator and utility functions for workflow model descriptors.
 * 
 * This class provides utilities for working with workflow models that are 
 * statically defined in Ballerina code, not dynamically parsed from JSON/YAML.
 *
 * @since 0.1.0
 */
public class WorkflowParser {

    /**
     * Generate Ballerina client code for a workflow model.
     * This is used by the Ballerina compiler plugin to generate static code.
     *
     * @param model the workflow model descriptor
     * @param clientName the name of the generated client
     * @return generated Ballerina code as string
     */
    public static BString generateWorkflowClient(BMap<BString, Object> model, BString clientName) {
        StringBuilder code = new StringBuilder();
        
        // Generate client class
        code.append("// Auto-generated workflow client for: ")
            .append(model.getStringValue(StringUtils.fromString("name")))
            .append("\n\n");
            
        code.append("public class ").append(clientName.getValue()).append(" {\n\n");
        
        // Generate workflow model as static data
        code.append("    private final WorkflowModelDescriptor model = {\n");
        code.append("        name: \"").append(model.getStringValue(StringUtils.fromString("name"))).append("\",\n");
        
        if (model.containsKey(StringUtils.fromString("description"))) {
            code.append("        description: \"")
                .append(model.getStringValue(StringUtils.fromString("description")))
                .append("\",\n");
        }
        
        code.append("        nodes: {},\n");  // TODO: Generate nodes
        code.append("        edges: []\n");   // TODO: Generate edges
        code.append("    };\n\n");
        
        // Generate client methods
        code.append("    public function execute(map<anydata> inputs = {}) returns WorkflowContext|error {\n");
        code.append("        return executeWorkflow(self.model, inputs);\n");
        code.append("    }\n\n");
        
        code.append("    public function validate() returns boolean|error {\n");
        code.append("        return validateWorkflowModel(self.model);\n");
        code.append("    }\n\n");
        
        code.append("}\n");
        
        return StringUtils.fromString(code.toString());
    }

    /**
     * Get workflow model metadata for code generation.
     *
     * @param model the workflow model descriptor
     * @return metadata map with code generation information
     */
    public static BMap<BString, Object> getWorkflowMetadata(BMap<BString, Object> model) {
        BMap<BString, Object> metadata = ValueCreator.createMapValue();
        
        // Extract information needed for code generation
        metadata.put(StringUtils.fromString("name"), model.get(StringUtils.fromString("name")));
        metadata.put(StringUtils.fromString("nodeCount"), 
            StringUtils.fromString(String.valueOf(getNodeCount(model))));
        metadata.put(StringUtils.fromString("edgeCount"), 
            StringUtils.fromString(String.valueOf(getEdgeCount(model))));
            
        return metadata;
    }
    
    private static int getNodeCount(BMap<BString, Object> model) {
        @SuppressWarnings("unchecked")
        BMap<BString, Object> nodes = (BMap<BString, Object>) model.get(StringUtils.fromString("nodes"));
        return nodes != null ? nodes.size() : 0;
    }
    
    private static int getEdgeCount(BMap<BString, Object> model) {
        BArray edges = (BArray) model.get(StringUtils.fromString("edges"));
        return edges != null ? (int) edges.size() : 0;
    }

    /**
     * Get all nodes of a specific type from the workflow model.
     *
     * @param model the workflow model descriptor
     * @param nodeType the type of nodes to retrieve
     * @return map of node identifiers to nodes of the specified type
     */
    public static BMap<BString, Object> getNodesByType(BMap<BString, Object> model, BString nodeType) {
        BMap<BString, Object> result = ValueCreator.createMapValue();
        
        @SuppressWarnings("unchecked")
        BMap<BString, Object> nodes = (BMap<BString, Object>) model.get(StringUtils.fromString("nodes"));
        
        if (nodes != null) {
            for (BString nodeId : nodes.getKeys()) {
                @SuppressWarnings("unchecked")
                BMap<BString, Object> node = (BMap<BString, Object>) nodes.get(nodeId);
                if (node != null) {
                    BString type = (BString) node.get(StringUtils.fromString("node"));
                    if (nodeType.getValue().equals(type.getValue())) {
                        result.put(nodeId, node);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Find all incoming edges for a specific node.
     *
     * @param model the workflow model descriptor
     * @param nodeId the identifier of the node
     * @return array of edges that end at the specified node
     */
    public static BArray getIncomingEdges(BMap<BString, Object> model, BString nodeId) {
        List<Object> incomingEdges = new ArrayList<>();
        
        BArray edges = (BArray) model.get(StringUtils.fromString("edges"));
        if (edges != null) {
            for (int i = 0; i < edges.size(); i++) {
                @SuppressWarnings("unchecked")
                BMap<BString, Object> edge = (BMap<BString, Object>) edges.get(i);
                BString endNode = (BString) edge.get(StringUtils.fromString("endNode"));
                if (nodeId.getValue().equals(endNode.getValue())) {
                    incomingEdges.add(edge);
                }
            }
        }
        
        ArrayType arrayType = TypeCreator.createArrayType(TypeCreator.createRecordType("Edge", null, 0, false, 0));
        return ValueCreator.createArrayValue(incomingEdges.toArray(), arrayType);
    }

    /**
     * Find all outgoing edges from a specific node.
     *
     * @param model the workflow model descriptor
     * @param nodeId the identifier of the node
     * @return array of edges that start from the specified node
     */
    public static BArray getOutgoingEdges(BMap<BString, Object> model, BString nodeId) {
        List<Object> outgoingEdges = new ArrayList<>();
        
        BArray edges = (BArray) model.get(StringUtils.fromString("edges"));
        if (edges != null) {
            for (int i = 0; i < edges.size(); i++) {
                @SuppressWarnings("unchecked")
                BMap<BString, Object> edge = (BMap<BString, Object>) edges.get(i);
                BString startNode = (BString) edge.get(StringUtils.fromString("startNode"));
                if (nodeId.getValue().equals(startNode.getValue())) {
                    outgoingEdges.add(edge);
                }
            }
        }
        
        ArrayType arrayType = TypeCreator.createArrayType(TypeCreator.createRecordType("Edge", null, 0, false, 0));
        return ValueCreator.createArrayValue(outgoingEdges.toArray(), arrayType);
    }

    /**
     * Create an empty workflow model template for code generation.
     *
     * @param name the workflow name
     * @param description optional description (can be null)
     * @return empty workflow model template
     */
    public static BMap<BString, Object> createWorkflowTemplate(BString name, Object description) {
        BMap<BString, Object> model = ValueCreator.createMapValue();
        
        model.put(StringUtils.fromString("name"), name);
        if (description != null && description instanceof BString) {
            model.put(StringUtils.fromString("description"), (BString) description);
        }
        
        // Create empty nodes and edges for code generation
        model.put(StringUtils.fromString("nodes"), ValueCreator.createMapValue());
        ArrayType arrayType = TypeCreator.createArrayType(TypeCreator.createRecordType("Edge", null, 0, false, 0));
        model.put(StringUtils.fromString("edges"), ValueCreator.createArrayValue(new Object[0], arrayType));
        
        return model;
    }
}