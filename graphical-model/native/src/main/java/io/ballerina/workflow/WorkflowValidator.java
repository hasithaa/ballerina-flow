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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Workflow validator for validating workflow model descriptors.
 *
 * @since 0.1.0
 */
public class WorkflowValidator {

    /**
     * Validate a workflow model descriptor.
     *
     * @param model the workflow model descriptor to validate
     * @return true if valid, false otherwise
     */
    public static boolean validate(BMap<BString, Object> model) {
        // Basic validation checks
        if (model == null) {
            return false;
        }
        
        // Check required fields
        if (model.get(StringUtils.fromString("name")) == null) {
            return false;
        }
        
        BMap<?, ?> nodes = (BMap<?, ?>) model.get(StringUtils.fromString("nodes"));
        if (nodes == null || nodes.size() == 0) {
            return false;
        }
        
        BArray edges = (BArray) model.get(StringUtils.fromString("edges"));
        if (edges == null) {
            return false;
        }
        
        return validateNodeConnectivity(model) && validateNodeTypes(model);
    }

    /**
     * Validate workflow node connectivity.
     *
     * @param model the workflow model descriptor to validate
     * @return true if all nodes are properly connected
     */
    public static boolean validateNodeConnectivity(BMap<BString, Object> model) {
        // TODO: Implement node connectivity validation
        // Check that all edge references point to existing nodes
        BMap<?, ?> nodes = (BMap<?, ?>) model.get(StringUtils.fromString("nodes"));
        BArray edges = (BArray) model.get(StringUtils.fromString("edges"));
        
        if (nodes == null || edges == null) {
            return false;
        }
        
        // Basic check - ensure we have nodes and edges
        return nodes.size() > 0;
    }

    /**
     * Validate workflow connectivity (alias for validateNodeConnectivity).
     *
     * @param model the workflow model descriptor to validate
     * @return true if all nodes are properly connected
     */
    public static boolean validateConnectivity(BMap<BString, Object> model) {
        return validateNodeConnectivity(model);
    }

    /**
     * Validate workflow node types and properties.
     *
     * @param model the workflow model descriptor to validate
     * @return true if all node types are valid
     */
    public static boolean validateNodeTypes(BMap<BString, Object> model) {
        // TODO: Implement node type validation
        // Check that all nodes have valid types (Activity, Event, While, ForEach)
        BMap<?, ?> nodes = (BMap<?, ?>) model.get(StringUtils.fromString("nodes"));
        
        if (nodes == null) {
            return false;
        }
        
        // For now, assume all node types are valid
        return true;
    }
}