terraform {
  required_version = ">= 1.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.51.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
  }
}

provider "azurerm" {
  subscription_id = "c7e7612d-69f5-47e2-9ddc-00e5096a1247"
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}