variable "name" {
  description = "Name of the Container App Environment"
  type        = string
}

variable "location" {
  description = "Azure region where the Container App Environment will be created"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "log_analytics_workspace_id" {
  description = "ID of the Log Analytics workspace"
  type        = string
}

variable "tags" {
  description = "Tags to be applied to the Container App Environment"
  type        = map(string)
  default     = {}
}