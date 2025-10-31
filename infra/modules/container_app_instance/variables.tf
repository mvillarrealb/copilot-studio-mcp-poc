variable "name" {
  description = "Name of the Container App"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "location" {
  description = "Azure region where the Container App will be created"
  type        = string
}

variable "container_app_environment_id" {
  description = "ID of the Container App Environment"
  type        = string
}

variable "container_registry_server" {
  description = "Container registry server URL"
  type        = string
}

variable "container_registry_name" {
  description = "Name of the container registry"
  type        = string
}

variable "source_code_path" {
  description = "Path to the source code directory containing Dockerfile"
  type        = string
}

variable "image_name" {
  description = "Name of the container image"
  type        = string
}

variable "container_name" {
  description = "Name of the container"
  type        = string
  default     = "main"
}

variable "revision_mode" {
  description = "Revision mode for the Container App"
  type        = string
  default     = "Single"
}

variable "min_replicas" {
  description = "Minimum number of replicas"
  type        = number
  default     = 0
}

variable "max_replicas" {
  description = "Maximum number of replicas"
  type        = number
  default     = 10
}

variable "cpu" {
  description = "CPU allocation for the container"
  type        = number
  default     = 0.25
}

variable "memory" {
  description = "Memory allocation for the container"
  type        = string
  default     = "0.5Gi"
}

variable "target_port" {
  description = "Target port for ingress"
  type        = number
  default     = 8080
}

variable "external_enabled" {
  description = "Enable external ingress"
  type        = bool
  default     = true
}

variable "allow_insecure_connections" {
  description = "Allow insecure connections"
  type        = bool
  default     = false
}

variable "transport" {
  description = "Transport protocol for ingress"
  type        = string
  default     = "auto"
}

variable "registry_identity" {
  description = "Identity for registry authentication"
  type        = string
  default     = "system"
}

variable "environment_variables" {
  description = "Environment variables for the container"
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "Tags to be applied to the Container App"
  type        = map(string)
  default     = {}
}