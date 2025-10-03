# Ballerina Flow Module Makefile

# Default target
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  build    - Build the Ballerina module"
	@echo "  push     - Push the module to local repository"
	@echo "  clean    - Clean build artifacts"
	@echo "  all      - Build and push the module"

# Build the Ballerina module
.PHONY: build
build:
	@echo "Building Ballerina module..."
	cd modules/flow && bal pack

# Push to local repository
.PHONY: push
push: build
	@echo "Pushing to local repository..."
	cd modules/flow && bal push --repository=local

# Clean build artifacts
.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	cd modules/flow && rm -rf target/

# Build and push in one command
.PHONY: all
all: push
	@echo "Build and push completed successfully!"

# Development helpers
.PHONY: check
check:
	@echo "Checking Ballerina syntax..."
	cd modules/flow && bal build --offline

.PHONY: test
test:
	@echo "Running tests..."
	cd modules/flow && bal test