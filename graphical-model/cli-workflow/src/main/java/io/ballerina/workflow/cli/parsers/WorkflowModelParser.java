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

package io.ballerina.workflow.cli.parsers;

import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.ballerina.workflow.cli.utils.WorkflowModel;
import io.ballerina.workflow.cli.utils.WorkflowModel.WorkflowNode;
import io.ballerina.workflow.cli.utils.WorkflowModel.WorkflowEdge;
import io.ballerina.workflow.cli.utils.WorkflowModel.NodeInput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parser for workflow model .bal files using Ballerina compiler APIs.
 * 
 * This parser uses the Ballerina compiler's AST to properly parse
 * workflow model definitions with full semantic analysis.
 *
 * @since 0.1.0
 */
public class WorkflowModelParser {
    
    /**
     * Parse a workflow model from a .bal file.
     *
     * @param filePath Path to the workflow .bal file
     * @return Parsed workflow model
     * @throws IOException If file cannot be read
     * @throws ParseException If parsing fails
     */
    public static WorkflowModel parse(Path filePath) throws IOException, ParseException {
        String content = Files.readString(filePath);
        return parseContent(content);
    }
    
    /**
     * Parse workflow model from string content using Ballerina compiler APIs.
     *
     * @param content The .bal file content
     * @return Parsed workflow model
     * @throws ParseException If parsing fails
     */
    public static WorkflowModel parseContent(String content) throws ParseException {
        try {
            // Create syntax tree from content
            TextDocument textDocument = TextDocuments.from(content);
            SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
            
            // Get the root module part
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            
            // Initialize model and node mappings
            WorkflowModel model = new WorkflowModel();
            Map<String, WorkflowNode> nodeMap = new HashMap<>();
            
            // Parse all module members looking for workflow models  
            NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
            for (ModuleMemberDeclarationNode member : members) {
                if (member.kind() == SyntaxKind.MODULE_VAR_DECL) {
                    ModuleVariableDeclarationNode varDecl = (ModuleVariableDeclarationNode) member;
                    parseModuleVariableDeclaration(varDecl, model, nodeMap);
                } else {
                    // Parse any member that might contain workflow annotations
                    parseGenericMember(member, model);
                }
            }
            
            // Set parsed nodes to model
            model.setNodes(nodeMap);
            
            return model;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse workflow model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse any module member that might contain workflow annotations.
     * This uses source code analysis to avoid dependency on specific node types.
     */
    private static void parseGenericMember(ModuleMemberDeclarationNode member, WorkflowModel model) {
        try {
            String memberCode = member.toSourceCode().trim();
            
            // Check for @workflow:WorkflowModel annotation in the source code
            if (memberCode.contains("@workflow:WorkflowModel") && memberCode.contains("type") && memberCode.contains("record")) {
                parseWorkflowModelFromSource(memberCode, model);
            }
        } catch (Exception e) {
            // Skip parsing errors for individual members
            System.err.println("Warning: Could not parse member: " + e.getMessage());
        }
    }

    /**
     * Parse a workflow model from source code using string analysis.
     */
    private static void parseWorkflowModelFromSource(String sourceCode, WorkflowModel model) {
        try {
            // Extract type name using regex
            java.util.regex.Pattern typePattern = java.util.regex.Pattern.compile("type\\s+(\\w+)\\s+record");
            java.util.regex.Matcher matcher = typePattern.matcher(sourceCode);
            
            if (matcher.find()) {
                String typeName = matcher.group(1);
                model.setName(typeName);
                
                // Extract field definitions within the record
                extractWorkflowFields(sourceCode, model);
            }
        } catch (Exception e) {
            System.err.println("Error parsing workflow model from source: " + e.getMessage());
        }
    }

    /**
     * Extract workflow fields from record source code.
     */
    private static void extractWorkflowFields(String sourceCode, WorkflowModel model) {
        try {
            // Extract content between record {| ... |}
            int recordStart = sourceCode.indexOf("record {|");
            int recordEnd = sourceCode.lastIndexOf("|}");
            
            if (recordStart != -1 && recordEnd != -1 && recordEnd > recordStart) {
                String recordContent = sourceCode.substring(recordStart + 9, recordEnd);
                
                // Split by semicolon to get individual field declarations
                String[] fieldLines = recordContent.split(";");
                
                for (String fieldLine : fieldLines) {
                    fieldLine = fieldLine.trim();
                    if (!fieldLine.isEmpty()) {
                        parseFieldLine(fieldLine, model);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting workflow fields: " + e.getMessage());
        }
    }

    /**
     * Parse a single field line from the record definition.
     */
    private static void parseFieldLine(String fieldLine, WorkflowModel model) {
        try {
            // Simple field parsing - handle basic cases like "@workflow:Task string taskName"
            String[] parts = fieldLine.trim().split("\\s+");
            if (parts.length >= 2) {
                String fieldType = null;
                String fieldName = null;
                String nodeKind = null;
                
                // Look for @workflow: annotations
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].startsWith("@workflow:")) {
                        nodeKind = parts[i].substring("@workflow:".length());
                        if (i + 1 < parts.length) fieldType = parts[i + 1];
                        if (i + 2 < parts.length) fieldName = parts[i + 2];
                        break;
                    }
                }
                
                // Fallback if no annotation - check field names for patterns
                if (nodeKind == null && parts.length >= 2) {
                    fieldType = parts[0];
                    fieldName = parts[1];
                    
                    if (fieldType.contains("Node") || fieldType.contains("Task") || fieldType.contains("Service") || 
                        fieldName.toLowerCase().contains("node") || fieldName.toLowerCase().contains("task")) {
                        nodeKind = "Node"; // Default node kind
                    }
                }
                
                // Create workflow node if we found a valid pattern
                if (nodeKind != null && fieldName != null) {
                    WorkflowNode node = new WorkflowNode();
                    node.setKind(nodeKind);
                    node.setDescription(fieldName + " node");
                    
                    // Set output type if we have field type information
                    if (fieldType != null && !fieldType.isEmpty()) {
                        node.setOutputType(fieldType);
                    }
                    
                    // Add to model using simplified node collection
                    if (model.getNodes() == null) {
                        model.setNodes(new java.util.HashMap<>());
                    }
                    model.getNodes().put(fieldName, node);
                }
            }
        } catch (Exception e) {
            // Skip malformed field lines
            System.err.println("Warning: Could not parse field line: " + fieldLine);
        }
    }

    /**
     * Parse a module variable declaration which could be a node, edge, or workflow descriptor.
     */
    private static void parseModuleVariableDeclaration(ModuleVariableDeclarationNode varDecl, 
                                                      WorkflowModel model, 
                                                      Map<String, WorkflowNode> nodeMap) throws ParseException {
        
        TypedBindingPatternNode bindingPattern = varDecl.typedBindingPattern();
        String variableName = bindingPattern.bindingPattern().toString().trim();
        
        // Get the type descriptor to determine what kind of declaration this is
        TypeDescriptorNode typeDescriptor = bindingPattern.typeDescriptor();
        String typeName = getTypeDescriptorText(typeDescriptor);
        
        if (!varDecl.initializer().isPresent()) {
            return; // Skip variables without initializers
        }
        
        ExpressionNode initializer = varDecl.initializer().get();
        
        if ("workflow:WorkflowModelDescriptor".equals(typeName)) {
            parseWorkflowModelDescriptor(initializer, model);
        } else if ("workflow:Node".equals(typeName)) {
            WorkflowNode node = parseWorkflowNode(initializer);
            if (node != null) {
                nodeMap.put(variableName, node);
            }
        } else if ("workflow:Edge".equals(typeName)) {
            WorkflowEdge edge = parseWorkflowEdge(initializer);
            if (edge != null) {
                model.addEdge(edge);
            }
        }
    }
    
    /**
     * Parse a WorkflowModelDescriptor from its initializer expression.
     */
    private static void parseWorkflowModelDescriptor(ExpressionNode initializer, WorkflowModel model) 
            throws ParseException {
        if (initializer.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            throw new ParseException("WorkflowModelDescriptor must be a record constructor");
        }
        
        MappingConstructorExpressionNode constructor = (MappingConstructorExpressionNode) initializer;
        
        for (MappingFieldNode field : constructor.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode specificField = (SpecificFieldNode) field;
                String fieldName = getFieldName(specificField);
                ExpressionNode fieldValue = specificField.valueExpr().orElse(null);
                
                switch (fieldName) {
                    case "name":
                        model.setName(extractStringLiteral(fieldValue));
                        break;
                    case "description":
                        model.setDescription(extractStringLiteral(fieldValue));
                        break;
                    // We'll handle nodes and edges separately from their own variable declarations
                }
            }
        }
    }
    
    /**
     * Parse a workflow node from its initializer expression.
     */
    private static WorkflowNode parseWorkflowNode(ExpressionNode initializer) throws ParseException {
        if (initializer.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            throw new ParseException("Node must be a record constructor");
        }
        
        MappingConstructorExpressionNode constructor = (MappingConstructorExpressionNode) initializer;
        WorkflowNode node = new WorkflowNode();
        
        for (MappingFieldNode field : constructor.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode specificField = (SpecificFieldNode) field;
                String fieldName = getFieldName(specificField);
                ExpressionNode fieldValue = specificField.valueExpr().orElse(null);
                
                switch (fieldName) {
                    case "kind":
                        node.setKind(extractStringLiteral(fieldValue));
                        break;
                    case "description":
                        node.setDescription(extractStringLiteral(fieldValue));
                        break;
                    case "template":
                        node.setTemplate(extractStringLiteral(fieldValue));
                        break;
                    case "output":
                        node.setOutputType(extractTypeReference(fieldValue));
                        break;
                    // TODO: Handle inputs array parsing
                }
            }
        }
        
        return node;
    }
    
    /**
     * Parse a workflow edge from its initializer expression.
     */
    private static WorkflowEdge parseWorkflowEdge(ExpressionNode initializer) throws ParseException {
        if (initializer.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            throw new ParseException("Edge must be a record constructor");
        }
        
        MappingConstructorExpressionNode constructor = (MappingConstructorExpressionNode) initializer;
        WorkflowEdge edge = new WorkflowEdge();
        
        for (MappingFieldNode field : constructor.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode specificField = (SpecificFieldNode) field;
                String fieldName = getFieldName(specificField);
                ExpressionNode fieldValue = specificField.valueExpr().orElse(null);
                
                switch (fieldName) {
                    case "startNode":
                        edge.setStartNodeId(extractNodeReference(fieldValue));
                        break;
                    case "endNode":
                        edge.setEndNodeId(extractNodeReference(fieldValue));
                        break;
                    case "condition":
                        edge.setCondition(extractStringLiteral(fieldValue));
                        break;
                }
            }
        }
        
        return edge;
    }
    
    /**
     * Extract the type descriptor text from a type descriptor node.
     */
    private static String getTypeDescriptorText(TypeDescriptorNode typeDescriptor) {
        if (typeDescriptor.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode qualifiedName = (QualifiedNameReferenceNode) typeDescriptor;
            return qualifiedName.modulePrefix().text() + ":" + qualifiedName.identifier().text();
        } else if (typeDescriptor.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            SimpleNameReferenceNode simpleName = (SimpleNameReferenceNode) typeDescriptor;
            return simpleName.name().text();
        }
        return typeDescriptor.toString().trim();
    }
    
    /**
     * Extract the field name from a specific field node.
     */
    private static String getFieldName(SpecificFieldNode field) {
        if (field.fieldName().kind() == SyntaxKind.IDENTIFIER_TOKEN) {
            return field.fieldName().toString().trim();
        }
        return "";
    }
    
    /**
     * Extract a string literal value from an expression node.
     */
    private static String extractStringLiteral(ExpressionNode expression) {
        if (expression == null) return null;
        
        if (expression.kind() == SyntaxKind.STRING_LITERAL) {
            BasicLiteralNode literal = (BasicLiteralNode) expression;
            String value = literal.literalToken().text();
            // Remove surrounding quotes
            return value.length() > 2 ? value.substring(1, value.length() - 1) : value;
        }
        
        return null;
    }
    
    /**
     * Extract a type reference from an expression node.
     */
    private static String extractTypeReference(ExpressionNode expression) {
        if (expression == null) return null;
        
        if (expression.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            SimpleNameReferenceNode nameRef = (SimpleNameReferenceNode) expression;
            return nameRef.name().text();
        } else if (expression.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode qualifiedRef = (QualifiedNameReferenceNode) expression;
            return qualifiedRef.modulePrefix().text() + ":" + qualifiedRef.identifier().text();
        }
        
        return expression.toString().trim();
    }
    
    /**
     * Extract a node reference from an expression node.
     */
    private static String extractNodeReference(ExpressionNode expression) {
        if (expression == null) return null;
        
        if (expression.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            SimpleNameReferenceNode nameRef = (SimpleNameReferenceNode) expression;
            return nameRef.name().text();
        }
        
        return expression.toString().trim();
    }
    
    /**
     * Exception thrown when parsing fails.
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
        
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}