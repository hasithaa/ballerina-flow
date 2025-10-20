# Ballerina Workflow Graph Model

This implementation provides a comprehensive workflow management system for Ballerina with the following components:

## Components

- **[Java Workflow Engine](./native/)**: Core workflow engine implemented in Java with native Ballerina integration
- **[Ballerina Workflow Module](./ballerina/)**: Ballerina library for workflow modeling and execution
- **[Workflow CLI Tool](./cli-workflow/)**: Command-line tool to generate Ballerina code from workflow model schema
- **[Workflow Core Library](./workflow-core/)**: Shared core functionality and models
- **[Compiler Plugin](./compiler-plugin/)**: Ballerina compiler plugin for workflow validation
- **[Plugin Tests](./compiler-plugin-test/)**: Test suite for the compiler plugin

## Building from Source

### Prerequisites

1. Download and install Java SE Development Kit (JDK) version 21
2. Set up your GitHub personal access token with read package permissions:
   ```bash
   export packageUser=<Username>
   export packagePAT=<Personal access token>
   ```

### Building

Execute the following commands to build from source:

1. Build all modules:
   ```bash
   ./gradlew clean build
   ```

2. Run a specific module:
   ```bash
   ./gradlew :workflow-cli:build
   ./gradlew :workflow-ballerina:build
   ./gradlew :workflow-native:build
   ```

3. List all projects:
   ```bash
   ./gradlew projects
   ```

## Project Structure

```
graphical-model/
├── ballerina/              # Ballerina workflow module
│   ├── workflow.bal
│   ├── Ballerina.toml
│   └── build.gradle
├── native/                 # Java native implementation
│   └── src/main/java/
├── cli-workflow/           # CLI tool
│   └── src/main/java/
├── workflow-core/          # Core library
│   └── src/main/java/
├── compiler-plugin/        # Ballerina compiler plugin
│   └── src/main/java/
├── compiler-plugin-test/   # Plugin tests
│   └── src/test/java/
├── build-config/           # Build configuration
│   └── checkstyle/
├── build.gradle           # Root build script
├── settings.gradle        # Gradle settings
├── gradle.properties      # Project properties
└── gradlew               # Gradle wrapper
```


## Note

This is a PoC done by Hasitha Aravinda for demonstration purposes only. It is not intended for production use.
