# Deployment Modes

There are several deployment models for running Ballerina applications that use the Temporal workflow engine.

Code must be organized so that there is a clear separation between the API layer (Integration) and the Worker layer (Workflow Logic), as they have different scaling and fault-tolerance requirements.

-----

## 1. Demo Mode: All-in-One with Embedded Engine

This mode is for development and demonstration only. The entire integration contains the API, Workflow Logic, and Temporal Dev Engine all in one process.

  * **Description:**
      * If using K8s, a single K8s Pod runs one container from the Ballerina integration.
      * An external database (like H2 or an external Postgres) might be used for persistence, but the engine itself is not clustered.
  * **Pros:** Simple, zero-dependency setup for local testing.
  * **Cons:** Not scalable, no fault-tolerance, not for production.

<!-- end list -->

```mermaid
graph TD
    subgraph "Ballerina Process"
        API["Any BI Integration"]
        WorkflowLogic["Workflow Logic"]
        Workflow["Workflow Library"]
        TemporalEngine["Temporal Dev Engine"]
        API --"Client Call"--> Workflow
        WorkflowLogic <--"Local"--> Workflow
        Workflow --gRPC--> TemporalEngine
    end
    
    User[User] --Network Call--> API
```

-----

## 2\. Co-located Model: Standard Cluster

The Integration logic (API) and Workflow logic (Worker) are co-located in the same process, but they connect to a central, external engine.

  * **Description:**
      * A central, **external Temporal Cluster** is deployed once (e.g., in K8s).
      * The Ballerina Docker image is deployed as a K8s Deployment with **multiple replicas**.
      * **Each pod** runs both the Integration and the Workflow logic.
      * The Workflow Library connects to the external Temporal Cluster via gRPC.
  * **Pros:** Simpler to deploy than the decoupled model (one K8s deployment for the integration app).
  * **Cons:** Scaling is coupled (API and Workers scale together). A crash in the API code will also crash the Worker in that pod.

<!-- end list -->

```mermaid
graph TD
    subgraph "K8s Cluster"
        subgraph "External Temporal Cluster"
            direction LR
            Cluster[Temporal] --- DB[(Database)]
        end

        subgraph "Ballerina App - K8s Deployment"
            direction LR
            subgraph "Pod 1"
                API1["Any BI Integration"]
                WorkflowLogic1["Workflow Logic"]
                Workflow1["Workflow Library"]
            end
            
            subgraph "Pod 2"
                API2["Any BI Integration"]
                WorkflowLogic2["Workflow Logic"]
                Workflow2["Workflow Library"]
            end
            
            subgraph "..."
                API3["Any BI Integration"]
                WorkflowLogic3["Workflow Logic"]
                Workflow3["Workflow Library"]
            end
        end
    end

    User[User] --> API1
    User --> API2
    User --> API3

    API1 --"Client Call"--> Workflow1
    WorkflowLogic1 <--"Local"--> Workflow1

    API2 --"Client Call"--> Workflow2
    WorkflowLogic2 <--"Local"--> Workflow2
    API3 --"Client Call"--> Workflow3
    WorkflowLogic3 <--"Local"--> Workflow3


    Workflow1 -- "gRPC" --> Cluster
    Workflow2 -- "gRPC" --> Cluster
    Workflow3 -- "gRPC" --> Cluster

    style User fill:#f9f,stroke:#333
    style Cluster fill:#fdf,stroke:#333
```

## 3. Decoupled Model

For this deployment, the API layer and the Worker layer are fully decoupled at the source level, resulting in two separate integrations (e.g., two Ballerina projects) that are deployed independently.

The goal is to physically separate the API fleet from the Worker fleet.

  * **Description:**
      * A central, **external Temporal Cluster** is deployed once.
      * **Deployment 1 (API Fleet):** Runs the **API Integration** image. This deployment only starts the integration logic (API). Its `Workflow Library` (Client) makes gRPC calls to the Temporal Cluster.
      * **Deployment 2 (Worker Fleet):** Runs the separate **Workflow Integration** image. This deployment only starts the **Workflow Logic** and **Workflow Library** (Worker). Its `Workflow Library` makes gRPC calls to the Temporal Cluster to poll for tasks.
  * **Pros:** Independent scaling (scale APIs on traffic, scale Workers on load). Fault isolation (API can crash without stopping Workers, and vice-versa).
  * **Cons:** More complex project/source configuration (requires managing two separate Ballerina projects and Docker builds).

<!-- end list -->

```mermaid
graph TD
    subgraph "Kubernetes Cluster"
        subgraph "External Temporal Cluster"
            direction LR
            Cluster[Temporal] --- DB[(Database)]
        end
        
        subgraph "Deployment 1: API Fleet (Project 1)"
            direction LR
            subgraph "Pod 1"
                API1["API (Integration)"]
                WorkflowClient1["Workflow Library (Client)"]
            end
            
            subgraph "Pod 2"
                API2["API (Integration)"]
                WorkflowClient2["Workflow Library (Client)"]
            end
            
            subgraph "..."
                API3["API (Integration)"]
                WorkflowClient3["Workflow Library (Client)"]
            end
        end

        subgraph "Deployment 2: Worker Fleet (Project 2)"
            direction LR
            subgraph "Pod A"
                WorkflowLogicA["Workflow Logic"]
                WorkflowWorkerA["Workflow Library (Worker)"]
            end
            
            subgraph "Pod B"
                WorkflowLogicB["Workflow Logic"]
                WorkflowWorkerB["Workflow Library (Worker)"]
            end
            
            subgraph "...."
                WorkflowLogicC["Workflow Logic"]
                WorkflowWorkerC["Workflow Library (Worker)"]
            end
        end
    end

        User[User] --> API1
        User --> API2
        User --> API3

        API1 --"Client Call"--> WorkflowClient1
        API2 --"Client Call"--> WorkflowClient2
        API3 --"Client Call"--> WorkflowClient3
        WorkflowLogicA <--"Local"--> WorkflowWorkerA
        WorkflowLogicB <--"Local"--> WorkflowWorkerB
        WorkflowLogicC <--"Local"--> WorkflowWorkerC

    WorkflowClient1 -- "gRPC (Start, Signal, etc.)" --> Cluster
    WorkflowClient2 -- "gRPC (Start, Signal, etc.)" --> Cluster
    WorkflowClient3 -- "gRPC (Start, Signal, etc.)" --> Cluster
    
    WorkflowWorkerA -- "gRPC (Poll Task Queue)" --> Cluster
    WorkflowWorkerB -- "gRPC (Poll Task Queue)" --> Cluster
    WorkflowWorkerC -- "gRPC (Poll Task Queue)" --> Cluster

    style User fill:#f9f,stroke:#333
    style Cluster fill:#fdf,stroke:#333
```