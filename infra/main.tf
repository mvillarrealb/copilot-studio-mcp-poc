# Local values for consistent naming
locals {
  name_prefix = "${var.project_name}-${var.environment}"
  common_tags = merge(var.tags, {
    Environment = var.environment
    Location    = var.location
  })
}

# Resource Group Module
module "resource_group" {
  source = "./modules/resource_group"

  name     = "${local.name_prefix}-rg"
  location = var.location
  tags     = local.common_tags
}

# Container Registry Module
module "container_registry" {
  source = "./modules/container_registry"

  name                = replace("${local.name_prefix}acr", "-", "")
  resource_group_name = module.resource_group.name
  location            = var.location
  tags                = local.common_tags

  depends_on = [module.resource_group]
}

# Log Analytics Workspace Module
module "log_analytics_workspace" {
  source = "./modules/log_analytics_workspace"

  name                = "${local.name_prefix}-law"
  resource_group_name = module.resource_group.name
  location            = var.location
  tags                = local.common_tags

  depends_on = [module.resource_group]
}

# Container App Environment Module
module "container_app_environment" {
  source = "./modules/container_app_environment"

  name                         = "${local.name_prefix}-cae"
  resource_group_name          = module.resource_group.name
  location                     = var.location
  log_analytics_workspace_id   = module.log_analytics_workspace.id
  tags                         = local.common_tags

  depends_on = [module.log_analytics_workspace]
}

# Container App Instance Module
module "container_app_instance" {
  source = "./modules/container_app_instance"

  name                        = "${local.name_prefix}-ca"
  resource_group_name         = module.resource_group.name
  location                    = var.location
  container_app_environment_id = module.container_app_environment.id
  container_registry_server   = module.container_registry.login_server
  container_registry_name     = module.container_registry.name
  source_code_path            = var.source_code_path
  image_name                  = "${local.name_prefix}-app"
  tags                        = local.common_tags

  depends_on = [module.container_app_environment, module.container_registry]
}