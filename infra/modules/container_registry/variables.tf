variable "name" {
  description = "Name of the container registry"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "location" {
  description = "Azure region where the container registry will be created"
  type        = string
}

variable "sku" {
  description = "SKU of the container registry"
  type        = string
  default     = "Basic"
}

variable "admin_enabled" {
  description = "Enable admin user for the container registry"
  type        = bool
  default     = true
}

variable "public_network_access_enabled" {
  description = "Enable public network access for the container registry"
  type        = bool
  default     = true
}

variable "network_rule_bypass_option" {
  description = "Network rule bypass option for the container registry"
  type        = string
  default     = "AzureServices"
}

variable "tags" {
  description = "Tags to be applied to the container registry"
  type        = map(string)
  default     = {}
}