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

package io.ballerina.workflow.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Main CLI class for Ballerina workflow tools.
 *
 * @since 0.1.0
 */
@Command(name = "bal-workflow", 
         description = "Ballerina workflow client code generator",
         mixinStandardHelpOptions = true,
         version = "0.1.0")
public class WorkflowCli implements Callable<Integer> {

    @Parameters(index = "0", description = "The workflow model definition file (.bal)")
    private File modelFile;

    @Option(names = {"-o", "--output"}, description = "Output directory for generated client code")
    private File outputDir = new File(".");

    @Option(names = {"-c", "--client"}, description = "Generated client class name")
    private String clientName = "WorkflowClient";

    @Option(names = {"-p", "--package"}, description = "Package name for generated code")
    private String packageName = "workflow";

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WorkflowCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("🚀 Ballerina Workflow Client Generator");
        System.out.println("=====================================");
        System.out.println("Model file: " + modelFile.getAbsolutePath());
        System.out.println("Output directory: " + outputDir.getAbsolutePath());
        System.out.println("Client name: " + clientName);
        System.out.println("Package: " + packageName);
        System.out.println();
        
        if (!modelFile.exists()) {
            System.err.println("❌ Error: Workflow model file not found: " + modelFile.getAbsolutePath());
            return 1;
        }
        
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Generate static Ballerina client code
        generateWorkflowClient();
        
        System.out.println("✅ Workflow client code generated successfully!");
        System.out.println("   Generated: " + clientName + ".bal");
        System.out.println();
        System.out.println("💡 Usage:");
        System.out.println("   import " + packageName + ";");
        System.out.println("   " + clientName + " client = new();");
        System.out.println("   WorkflowContext result = check client.execute();");
        
        return 0;
    }
    
    private void generateWorkflowClient() throws Exception {
        // TODO: Parse the .bal model file and generate client code
        // This would integrate with the Ballerina compiler to:
        // 1. Parse the WorkflowModelDescriptor from the .bal file
        // 2. Generate optimized client code
        // 3. Create type-safe workflow execution methods
        
        String clientCode = generateClientTemplate();
        
        File outputFile = new File(outputDir, clientName + ".bal");
        java.nio.file.Files.write(outputFile.toPath(), clientCode.getBytes());
    }
    
    private String generateClientTemplate() {
        return String.format("""
            // Auto-generated workflow client
            // Source: %s
            // Generated: %s
            
            import ballerina/workflow;
            
            # Generated workflow client for executing the workflow model
            public class %s {
                
                # Execute the workflow with given inputs
                #
                # + inputs - Input parameters for the workflow
                # + return - Workflow execution context or error
                public function execute(map<anydata> inputs = {}) returns workflow:WorkflowContext|error {
                    // TODO: Replace with actual model from parsed file
                    workflow:WorkflowModelDescriptor model = {
                        name: "GeneratedWorkflow",
                        description: "Auto-generated workflow from %s",
                        nodes: {},
                        edges: []
                    };
                    
                    return workflow:executeWorkflow(model, inputs);
                }
                
                # Validate the workflow model
                #
                # + return - True if valid, error otherwise
                public function validate() returns boolean|error {
                    // TODO: Validate actual model
                    return true;
                }
                
                # Get workflow metadata
                #
                # + return - Workflow metadata
                public function getMetadata() returns map<string> {
                    return {
                        "name": "GeneratedWorkflow",
                        "source": "%s",
                        "generated": "%s"
                    };
                }
            }
            """, 
            modelFile.getName(),
            java.time.LocalDateTime.now(),
            clientName,
            modelFile.getName(),
            modelFile.getName(),
            java.time.LocalDateTime.now()
        );
    }
}