resource "null_resource" "build_and_push_image" {
  triggers = {
    source_file = filemd5("${var.source_code_path}/Dockerfile")
    image_tag = var.image_tag
  }
  provisioner "local-exec" {
    command = <<EOT
    az acr login --resource-group ${var.resource_group_name} --name ${var.container_registry_name} --expose-token &&
    az acr build --resource-group ${var.resource_group_name} --registry ${var.container_registry_name} --image ${var.image_name}:${var.image_tag} --file ${var.source_code_path}/Dockerfile ${var.source_code_path}/.
    EOT
  }
}

# Container App resource
resource "azurerm_container_app" "this" {
  name                         = var.name
  container_app_environment_id = var.container_app_environment_id
  resource_group_name          = var.resource_group_name
  revision_mode                = var.revision_mode

  template {
    min_replicas = var.min_replicas
    max_replicas = var.max_replicas

    container {
      name   = var.container_name
      image  = "${var.container_registry_server}/${var.image_name}:${var.image_tag}"
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = var.environment_variables
        content {
          name        = env.value.name
          value       = env.value.value
          secret_name = env.value.secret_ref
        }
      }
    }
  }

  ingress {
    allow_insecure_connections = var.allow_insecure_connections
    external_enabled           = var.external_enabled
    target_port                = var.target_port
    transport                  = var.transport

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  # Solo crear el secret si tenemos una contraseña
  dynamic "secret" {
    for_each = var.registry_password != null ? [1] : []
    content {
      name  = "registry-password"
      value = var.registry_password
    }
  }

  # Configuración condicional del registry
  dynamic "registry" {
    for_each = [1]
    content {
      server = var.container_registry_server
      
      # Si tenemos credenciales de admin, usar username/password
      username             = var.registry_username
      password_secret_name = var.registry_password != null ? "registry-password" : null
      
      # Si no tenemos credenciales, usar identidad del sistema
      identity = var.registry_username == null ? var.registry_identity : null
    }
  }

  tags = var.tags

  depends_on = [null_resource.build_and_push_image]
}