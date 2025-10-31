variable "name" {
  description = "Name of the Log Analytics workspace"
  type        = string
}

variable "location" {
  description = "Azure region where the Log Analytics workspace will be created"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "sku" {
  description = "SKU of the Log Analytics workspace"
  type        = string
  default     = "PerGB2018"
}

variable "retention_in_days" {
  description = "Retention period in days for the Log Analytics workspace"
  type        = number
  default     = 30
}

variable "tags" {
  description = "Tags to be applied to the Log Analytics workspace"
  type        = map(string)
  default     = {}
}