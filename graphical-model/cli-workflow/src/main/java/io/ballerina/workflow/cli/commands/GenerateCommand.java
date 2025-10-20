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

package io.ballerina.workflow.cli.commands;

import io.ballerina.workflow.cli.generators.ClientGenerator;
import io.ballerina.workflow.cli.generators.ImplementationGenerator;
import io.ballerina.workflow.cli.parsers.WorkflowModelParser;
import io.ballerina.workflow.cli.utils.WorkflowModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Command to generate Ballerina code from workflow model.
 *
 * @since 0.1.0
 */
@Command(name = "gen",
         description = "Generate Ballerina code from workflow model")
public class GenerateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the workflow to generate code for")
    private String workflowName;

    @Option(names = {"--create-impl"}, 
            description = "Create workflow.bal implementation file with basic structure")
    private boolean createImpl;

    @Option(names = {"--update-impl"}, 
            description = "Update existing workflow.bal implementation file (add new methods only)")
    private boolean updateImpl;

    @Override
    public Integer call() throws Exception {
        System.out.println("Generating code for workflow: " + workflowName);
        System.out.println("==========================================");
        
        try {
            // Find the workflow model file
            String fileName = workflowName.toLowerCase().replace(" ", "_") + ".bal";
            Path workflowFile = Paths.get("workflows", fileName);
            
            if (!Files.exists(workflowFile)) {
                System.err.println("Error: Workflow model file not found: " + workflowFile);
                System.err.println("   Run 'workflow new " + workflowName + "' to create it first");
                return 1;
            }
            
            // Parse the workflow model
            System.out.println("Parsing workflow model...");
            WorkflowModel model = WorkflowModelParser.parse(workflowFile);
            
            // Create generated directory structure
            Path generatedDir = Paths.get("generated", workflowName.toLowerCase().replace(" ", "_"));
            Files.createDirectories(generatedDir);
            
            // Generate client code
            System.out.println("Generating client code...");
            ClientGenerator clientGenerator = new ClientGenerator(model);
            String clientCode = clientGenerator.generate();
            
            // Write client files
            Files.write(generatedDir.resolve("client.bal"), clientCode.getBytes());
            
            // Generate types and context
            String typesCode = clientGenerator.generateTypes();
            Files.write(generatedDir.resolve("types.bal"), typesCode.getBytes());
            
            System.out.println("Generated client code:");
            System.out.println("   " + generatedDir.toAbsolutePath());
            System.out.println("   client.bal");
            System.out.println("   types.bal");
            
            // Handle implementation generation
            if (createImpl || updateImpl) {
                handleImplementationGeneration(model, createImpl);
            }
            
            System.out.println();
            System.out.println("Integration example:");
            System.out.println("   import generated." + workflowName.toLowerCase().replace(" ", "_") + ";");
            System.out.println();
            System.out.println("   " + model.getCapitalizedName() + "WorkflowClient client = check new(new " + 
                             model.getCapitalizedName() + "WorkflowImpl(), new workflow:InMemoryProvider());");
            System.out.println("   string workflowId = check client->start" + model.getCapitalizedName() + "(...);");
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error generating code: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private void handleImplementationGeneration(WorkflowModel model, boolean createMode) throws IOException {
        Path implFile = Paths.get("workflow.bal");
        ImplementationGenerator implGenerator = new ImplementationGenerator(model);
        
        if (createMode) {
            if (Files.exists(implFile)) {
                System.err.println("Warning: workflow.bal already exists, skipping implementation generation");
                System.err.println("   Use --update-impl to update existing implementation");
                return;
            }
            
            System.out.println("Creating implementation file...");
            String implCode = implGenerator.generateComplete();
            Files.write(implFile, implCode.getBytes());
            System.out.println("Created workflow.bal with basic implementation");
            
        } else { // updateMode
            if (!Files.exists(implFile)) {
                System.err.println("Warning: workflow.bal not found, creating new file");
                System.err.println("   Use --create-impl for initial creation");
                String implCode = implGenerator.generateComplete();
                Files.write(implFile, implCode.getBytes());
                System.out.println("Created workflow.bal with basic implementation");
                return;
            }
            
            System.out.println("Updating implementation file...");
            String existingContent = Files.readString(implFile);
            String updatedContent = implGenerator.updateExisting(existingContent);
            
            if (!updatedContent.equals(existingContent)) {
                Files.write(implFile, updatedContent.getBytes());
                System.out.println("Updated workflow.bal with new methods");
            } else {
                System.out.println("No updates needed for workflow.bal");
            }
        }
    }
}