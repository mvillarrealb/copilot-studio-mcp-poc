output "id" {
  description = "ID of the Container App"
  value       = azurerm_container_app.this.id
}

output "name" {
  description = "Name of the Container App"
  value       = azurerm_container_app.this.name
}

output "fqdn" {
  description = "FQDN of the Container App"
  value       = try(azurerm_container_app.this.ingress[0].fqdn, null)
}

output "app_url" {
  description = "URL of the Container App"
  value       = try("https://${azurerm_container_app.this.ingress[0].fqdn}", null)
}

output "latest_revision_name" {
  description = "Name of the latest revision"
  value       = azurerm_container_app.this.latest_revision_name
}

output "latest_revision_fqdn" {
  description = "FQDN of the latest revision"
  value       = azurerm_container_app.this.latest_revision_fqdn
}

# 🆕 NUEVOS OUTPUTS
output "target_port" {
  description = "Puerto objetivo configurado"
  value       = var.target_port
}

output "outbound_ip_addresses" {
  description = "Direcciones IP de salida"
  value       = azurerm_container_app.this.outbound_ip_addresses
}

output "custom_domain_verification_id" {
  description = "ID de verificación de dominio personalizado"
  value       = azurerm_container_app.this.custom_domain_verification_id
}

output "environment_variables_count" {
  description = "Número de variables de entorno configuradas"
  value       = length(var.environment_variables)
}