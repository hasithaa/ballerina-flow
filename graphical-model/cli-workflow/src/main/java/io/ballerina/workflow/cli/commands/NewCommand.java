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

import io.ballerina.workflow.cli.utils.WorkflowTemplateGenerator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Command to create a new workflow model file.
 *
 * @since 0.1.0
 */
@Command(name = "new",
         description = "Create a new workflow model file under workflows directory")
public class NewCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the workflow to create")
    private String workflowName;

    // TODO: Validate workflow name (no special chars, not empty, alphanumeric)

    @Override
    public Integer call() throws Exception {
        System.out.println("Creating new workflow: " + workflowName);        
        try {
            // Create workflows directory if it doesn't exist
            Path workflowsDir = Paths.get("workflows");
            if (!Files.exists(workflowsDir)) {
                Files.createDirectories(workflowsDir);
                System.out.println("Created workflows directory");
            }
            
            // Create the workflow file
            String fileName = workflowName.toLowerCase().replace(" ", "_") + ".bal";
            Path workflowFile = workflowsDir.resolve(fileName);
            
            if (Files.exists(workflowFile)) {
                System.err.println("Error: Workflow file already exists: " + workflowFile);
                return 1;
            }
            
            // Generate template content
            String workflowContent = WorkflowTemplateGenerator.generateWorkflowTemplate(workflowName);
            Files.write(workflowFile, workflowContent.getBytes());
            
            System.out.println("Successfully created workflow model:");
            System.out.println("   File: " + workflowFile.toAbsolutePath());
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("   1. Edit " + fileName + " to define your workflow structure");
            System.out.println("   2. Run 'workflow gen " + workflowName + "' to generate client code");
            System.out.println();
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error creating workflow file: " + e.getMessage());
            return 1;
        }
    }
}