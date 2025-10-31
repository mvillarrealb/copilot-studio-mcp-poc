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
  value       = azurerm_container_app.this.ingress[0].fqdn
}

output "app_url" {
  description = "URL of the Container App"
  value       = "https://${azurerm_container_app.this.ingress[0].fqdn}"
}

output "latest_revision_name" {
  description = "Name of the latest revision"
  value       = azurerm_container_app.this.latest_revision_name
}

output "latest_revision_fqdn" {
  description = "FQDN of the latest revision"
  value       = azurerm_container_app.this.latest_revision_fqdn
}