# Lovable Microservices Platform

This repository contains the backend platform for a multi-service application built around Spring Boot, Spring Cloud, Kubernetes, and event-driven integrations. The system is split into focused services for identity and billing, central configuration, service discovery, API routing, AI-assisted workflows, and project/workspace management.

The codebase is structured for both local development and Kubernetes deployment. Local infrastructure is provided with Docker Compose, while production-style deployment is described through Kubernetes manifests and GitHub Actions workflows that build container images and deploy them to GKE.

## Repository Structure

```text
.
|-- account-service
|-- common-service
|-- config-service
|-- discovery-service
|-- gateway-service
|-- intelligence-service
|-- workspace-service
|-- k8s
|   |-- infra
|   |-- proxy
|   |-- services
|   `-- stateful
|-- .github/workflows
`-- services.docker-compose.yml
```

## Architecture Overview

The platform follows a standard Spring Cloud microservice layout:

- `config-service` is the centralized configuration server.
- `discovery-service` is the Eureka registry.
- `gateway-service` is the public API entry point.
- `account-service` manages authentication, user lookups, billing, and payment webhooks.
- `workspace-service` manages projects, files, project members, internal preview deployment logic, and file storage workflows.
- `intelligence-service` provides AI/chat-oriented functionality and collaborates with workspace/account services.
- `common-service` contains reusable shared code used by multiple services.

At runtime, the core request flow is:

```text
Client
  ->
Gateway Service
  ->
Account / Workspace / Intelligence services
  ->
PostgreSQL / MinIO / Kafka / Redis / external providers
```

Supporting platform services:

- Spring Cloud Config for centralized config loading
- Eureka for service discovery
- Kafka for async communication
- Redis for preview routing
- MinIO for object/file storage
- pgvector/PostgreSQL for relational and vector-capable storage
- GKE for orchestration

## Tech Stack

### Core Backend

- Java 21
- Spring Boot `4.0.3`
- Spring Cloud `2025.1.x`
- Spring Cloud Config Server
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway
- Spring Data JPA
- Spring Security
- Spring Validation
- OpenFeign
- Actuator

### AI and Messaging

- Spring AI `2.0.0-M3`
- Apache Kafka
- Spring Kafka

### Data and Storage

- PostgreSQL
- pgvector
- Redis
- MinIO

### Payments and External Integrations

- Stripe Java SDK

### Containerization and Deployment

- Docker
- Google Jib Maven Plugin
- Kubernetes
- Google Kubernetes Engine (GKE)
- GitHub Actions

### Preview/Proxy Layer

- Node.js
- `http-proxy`
- `ioredis`

## Services

### 1. Account Service

Path: `account-service`

Primary responsibilities:

- user sign-up and login
- internal user lookup APIs for other services
- billing/subscription lookup
- checkout and billing portal creation
- Stripe webhook handling

Observed API surface from controllers:

- `/auth/signup`
- `/auth/login`
- `/api/me/subscription`
- `/api/payments/checkout`
- `/api/payments/portal`
- `/webhooks/payment`
- `/internal/v1/users/{id}`
- `/internal/v1/users/by-email`
- `/internal/v1/billing/current-plan`

Runtime details:

- container port: `9050`
- health endpoint: `/account/actuator/health`
- depends on Config Server and Kubernetes secrets for DB/JWT/Stripe values

### 2. Common Service

Path: `common-service`

This module is a shared library used by other services. It contains reusable code for cross-service concerns rather than acting as a standalone externally exposed API.

Current consumers in this repository include:

- `account-service`
- `intelligence-service`
- `workspace-service`

### 3. Config Service

Path: `config-service`

Primary responsibilities:

- centralized Spring configuration
- configuration sourcing from the external config repository
- profile-based configuration for local and Kubernetes environments

Runtime details:

- port: `8888`
- Spring annotation: `@EnableConfigServer`
- Kubernetes health endpoint: `/actuator/health`

The service reads configuration from:

- Git repository: `lovable-config-server`
- local/default config values
- environment variables in Kubernetes

### 4. Discovery Service

Path: `discovery-service`

Primary responsibilities:

- Eureka service registration and discovery

Runtime details:

- port: `8761`
- Spring annotation: `@EnableEurekaServer`

### 5. Gateway Service

Path: `gateway-service`

Primary responsibilities:

- central ingress to backend services
- service routing
- integration with the config/discovery ecosystem

Runtime details:

- container port: `8080`
- Spring annotation: `@EnableDiscoveryClient`
- Kubernetes health endpoint: `/actuator/health`

### 6. Intelligence Service

Path: `intelligence-service`

Primary responsibilities:

- AI/chat workflows
- streaming chat responses
- project-aware intelligence features
- Kafka-based response handling
- internal calls to account-service and workspace-service

Observed endpoints and integrations:

- `/chat/stream`
- `/chat/projects/{projectId}`
- `/api/usage`
- Kafka topic consumer: `file-store-responses`
- Feign clients to account and workspace internal APIs

Runtime details:

- container port: `9030`
- health endpoint: `/intelligence/actuator/health`
- uses Spring AI and PostgreSQL

### 7. Workspace Service

Path: `workspace-service`

Primary responsibilities:

- project CRUD
- project member management
- file tree and file content operations
- internal permission checking
- Kafka-driven file storage processing
- preview deployment orchestration inside Kubernetes

Observed endpoints and integrations:

- `/projects`
- `/projects/{id}`
- `/projects/{id}/deploy`
- `/projects/{projectId}/members`
- `/projects/{projectId}/files`
- `/projects/{projectId}/files/content`
- `/internal/v1/projects/{projectId}/files/tree`
- `/internal/v1/projects/{projectId}/files/content`
- `/internal/v1/projects/{projectId}/permissions/check`
- Kafka topic consumer: `file-storage-request-event`

Runtime details:

- container port: `9020`
- health endpoint: `/workspace/actuator/health`
- uses MinIO, Redis, PostgreSQL, Kafka, Stripe SDK, and Fabric8 Kubernetes Client

## Local Infrastructure

The repository includes [services.docker-compose.yml](./services.docker-compose.yml) for local support infrastructure.

Included services:

- `pgvector` on host port `9010`
- `minio` on host ports `9000` and `9001`
- `kafka` on host ports `9092` and `29092`

These containers provide the minimum shared infrastructure needed for local development and integration testing.

Start them with:

```bash
docker compose -f services.docker-compose.yml up -d
```

Stop them with:

```bash
docker compose -f services.docker-compose.yml down
```

## Running Services Locally

Most services are standard Maven Spring Boot applications.

Typical local startup order:

1. Start infrastructure from `services.docker-compose.yml`.
2. Start `config-service`.
3. Start `discovery-service`.
4. Start `gateway-service`.
5. Start domain services such as `account-service`, `workspace-service`, and `intelligence-service`.

Example:

```bash
cd config-service
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd config-service
.\mvnw.cmd spring-boot:run
```

Repeat the same pattern for other modules.

## Configuration Model

Configuration is split between:

- Spring Config Server
- application config files inside each service
- Kubernetes `Secret` objects
- Kubernetes `ConfigMap` objects

Important runtime patterns in this repo:

- service config imports use `CONFIG_SERVER_URL`
- Kubernetes services run with `SPRING_PROFILES_ACTIVE=k8s`
- sensitive values such as DB passwords, JWT secrets, Stripe credentials, Git credentials, and AI keys are expected through secrets

Shared Kubernetes config is defined in:

- `k8s/infra/namespaces.yml`

This includes:

- `lovable-core` namespace
- `lovable-previews` namespace
- `lovable-shared-config` ConfigMap

## Kubernetes Layout

Kubernetes manifests are grouped by concern:

- `k8s/infra` for namespaces, ingress, network policies, and runner pool
- `k8s/services` for stateless application services
- `k8s/stateful` for infrastructure stateful workloads such as Kafka, MinIO, Redis, and pgvector
- `k8s/proxy` for the wildcard preview proxy

Core application namespace:

- `lovable-core`

Preview namespace:

- `lovable-previews`

## Preview Infrastructure

The preview system is a distinct part of the platform.

Key pieces:

- a runner pool deployment in `lovable-previews`
- a Node-based wildcard proxy in `k8s/proxy`
- Redis-backed hostname to preview target resolution
- MinIO-based file sync support through a sidecar in the runner pool

The proxy service:

- reads target routing data from Redis
- forwards HTTP and WebSocket traffic
- resolves preview routes dynamically

This is designed for per-project preview environments and temporary runtime workloads.

## CI/CD

GitHub Actions workflows live in `.github/workflows`.

Current repository workflows build and deploy selected services to GKE by:

1. checking out the repo
2. setting up Java 21
3. making Maven wrappers executable on Linux runners
4. building container images with Jib
5. pushing images to Docker Hub
6. authenticating to Google Cloud with Workload Identity Federation
7. retrieving GKE cluster credentials
8. updating the Kubernetes deployment image with `kubectl set image`
9. waiting for rollout completion

Current deploy workflows exist for:

- account-service
- config-service
- gateway-service
- intelligence-service
- workspace-service

## Required Secrets and Runtime Inputs

Examples of values referenced by this repository:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_SERVICE_ACCOUNT`
- `GCP_CLUSTER`
- `GCP_ZONE`
- `GIT_USERNAME`
- `GIT_PASSWORD`
- `JWT_SECRET`
- `STRIPE_API_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `AI_API_KEY`
- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`

Do not commit real secrets to the repository. Store them in:

- GitHub Actions secrets for CI/CD
- Kubernetes `Secret` resources for runtime
- local environment variables for development

## Build Notes

This repository uses the Jib Maven plugin to build Docker images without requiring a Dockerfile for the Java services.

Some modules also depend on `common-service`, so CI pipelines may install that shared module before building dependent services.

## Suggested Development Workflow

1. Run shared infrastructure with Docker Compose.
2. Start config and discovery services first.
3. Start gateway and domain services.
4. Validate actuator health endpoints.
5. Test internal service-to-service communication.
6. Use GitHub Actions to validate container build and deployment behavior.

## Known Gaps / Repo Notes

- `discovery-service` does not currently have a matching deploy workflow in `.github/workflows`.
- `common-service` is treated as a shared library and not as a standalone deployed runtime service.
- The preview subsystem has separate infrastructure concerns from the core request/response microservices.

## License

No explicit license file is present in this repository at the time of writing.
