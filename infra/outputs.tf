output "resource_group_name" {
  description = "Name of the created resource group"
  value       = module.resource_group.name
}

output "resource_group_id" {
  description = "ID of the created resource group"
  value       = module.resource_group.id
}

output "container_registry_login_server" {
  description = "Login server URL for the container registry"
  value       = module.container_registry.login_server
}

output "container_registry_name" {
  description = "Name of the container registry"
  value       = module.container_registry.name
}

output "log_analytics_workspace_id" {
  description = "ID of the Log Analytics workspace"
  value       = module.log_analytics_workspace.id
}

output "container_app_environment_id" {
  description = "ID of the Container App Environment"
  value       = module.container_app_environment.id
}

output "container_app_url" {
  description = "URL of the deployed container app"
  value       = module.container_app_instance.app_url
}

output "container_app_fqdn" {
  description = "FQDN of the deployed container app"
  value       = module.container_app_instance.fqdn
}