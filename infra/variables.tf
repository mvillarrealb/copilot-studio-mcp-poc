variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "location" {
  description = "Azure region where resources will be created"
  type        = string
  default     = "westus"
}

variable "project_name" {
  description = "Name of the project used for resource naming"
  type        = string
  default     = "mcp-app"
}

variable "source_code_path" {
  description = "Path to the source code directory containing Dockerfile"
  type        = string
}

variable "tags" {
  description = "Common tags to be applied to all resources"
  type        = map(string)
  default = {
    Environment = "dev"
    Project     = "mcp-app"
    ManagedBy   = "terraform"
  }
}