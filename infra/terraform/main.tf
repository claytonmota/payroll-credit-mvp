// Placeholder Terraform module — target AWS deployment.
// Not fully wired for `terraform apply` yet: variables, backend, and
// per-service ECS/EKS task definitions to be filled in the next
// iteration (see docs/ROADMAP.md).

terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "payroll-credit-mvp"
}

// --- VPC (stub) ---
// A real module would provision a 3-AZ VPC with public + private subnets,
// NAT gateway, and route tables. Left as a stub so this file compiles
// standalone.

// --- Managed Kafka (MSK Serverless recommended for MVP) ---
// resource "aws_msk_serverless_cluster" "kafka" { ... }

// --- Managed Postgres (RDS) ---
// resource "aws_db_instance" "postgres" { ... }

// --- ECS/EKS cluster + services ---
// For each of ingestion-service, income-verification-service,
// decision-service: a task definition + service definition wiring
// KAFKA_BOOTSTRAP_SERVERS and SPRING_DATASOURCE_* to the resources above.
