

- [ ] Find a solution to how to refer Existing types in the model schema or refer them later. What this means is instead of typedesc in variables, we might want to refer to an existing type in the schema using just the name of the type. (This is a low priority task)



Simple solution. 
 - Model is only for modeling, and defining the types. Only Types, No functions
 - Implementation is only for implementation.
 - No Variable concept, only node output types, and input only for Events


Tasks. 

- [ ] Create Schema for the model. 
- [ ] Create a CLI tool to generate Ballerina code from the schema.
- [ ] Create a Ballerina compiler plugin to validate the generated code.
- [ ] Create a Java workflow engine to execute the workflow.

- [ ] Mock persistence layer for the workflow engine.
