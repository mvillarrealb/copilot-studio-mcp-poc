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

# 🆕 NOMBRE GENERADO SIMPLE PARA ACR
output "container_registry_generated_name" {
  description = "Nombre generado para el Container Registry (sin caracteres inválidos)"
  value       = local.acr_name
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

# 🆕 NUEVOS OUTPUTS
output "container_app_port" {
  description = "Puerto configurado para la aplicación"
  value       = module.container_app_instance.target_port
}

output "container_app_outbound_ips" {
  description = "Direcciones IP de salida de la Container App"
  value       = module.container_app_instance.outbound_ip_addresses
}

output "environment_variables_count" {
  description = "Número de variables de entorno configuradas"
  value       = module.container_app_instance.environment_variables_count
}

output "container_app_id" {
  description = "ID completo de la Container App"
  value       = module.container_app_instance.id
}

output "latest_revision_name" {
  description = "Nombre de la última revisión desplegada"
  value       = module.container_app_instance.latest_revision_name
}