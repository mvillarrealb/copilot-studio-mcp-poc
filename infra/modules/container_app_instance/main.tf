# Build and push the Docker image using Azure CLI
resource "null_resource" "build_and_push_image" {
  triggers = {
    source_code_hash = filesha256("${var.source_code_path}/Dockerfile")
    timestamp        = timestamp()
  }

  provisioner "local-exec" {
    command = <<-EOT
      # Login to Azure Container Registry
      az acr login --name ${var.container_registry_name}
      
      # Build and push the image using Azure CLI
      az acr build \
        --registry ${var.container_registry_name} \
        --image ${var.image_name}:latest \
        --file ${var.source_code_path}/Dockerfile \
        ${var.source_code_path}
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
      image  = "${var.container_registry_server}/${var.image_name}:latest"
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = var.environment_variables
        content {
          name  = env.key
          value = env.value
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

  registry {
    server   = var.container_registry_server
    identity = var.registry_identity
  }

  tags = var.tags

  depends_on = [null_resource.build_and_push_image]
}