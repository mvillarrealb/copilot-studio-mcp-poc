# Azure Container Apps Infrastructure with Terraform

This repository contains Terraform modules to deploy a complete Azure Container Apps infrastructure including:

- Resource Group
- Container Registry
- Log Analytics Workspace
- Container App Environment
- Container App Instance with automated build and deployment

## Architecture

```
Resource Group
├── Container Registry (ACR)
├── Log Analytics Workspace
├── Container App Environment
└── Container App Instance
```

## Prerequisites

1. **Azure CLI** - Install and login to Azure
   ```bash
   az login
   ```

2. **Terraform** - Install Terraform
   ```bash
   # On macOS using Homebrew
   brew install terraform
   
   # On Windows using winget
   winget install Hashicorp.Terraform
   ```

3. **Azure Subscription** - Ensure you have an active Azure subscription

## Project Structure

```
infra/
├── main.tf                    # Main configuration
├── variables.tf               # Variable definitions
├── outputs.tf                 # Output definitions
├── providers.tf               # Provider configuration
├── environments/
│   └── dev.tfvars.json       # Development environment variables
└── modules/
    ├── resource_group/
    ├── container_registry/
    ├── log_analytics_workspace/
    ├── container_app_environment/
    └── container_app_instance/
```

## Deployment Instructions

### 1. Navigate to the infra directory
```bash
cd infra
```

### 2. Initialize Terraform
```bash
terraform init
```

### 3. Plan the deployment
```bash
terraform plan -var-file="environments/dev.tfvars.json"
```

### 4. Validate the configuration
```bash
terraform validate
```

### 5. Apply the configuration
```bash
terraform apply -var-file="environments/dev.tfvars.json" -auto-approve
```

## Key Features

### Automated Container Build and Deployment
The Container App Instance module includes a `null_resource` that:
- Automatically builds the Docker image from your source code
- Pushes the image to Azure Container Registry using `az acr build`
- Deploys the image to Container Apps

### Security Best Practices
- Uses managed identity for Container Registry authentication
- Implements proper resource naming conventions
- Uses secure default configurations

### Modular Design
Each Azure resource is defined in its own module for:
- Reusability
- Maintainability
- Separation of concerns

## Configuration

### Environment Variables
Edit `environments/dev.tfvars.json` to customize your deployment:

```json
{
  "environment": "dev",
  "location": "westus",
  "project_name": "mcp-app",
  "source_code_path": "../ado-demo-mcp",
  "tags": {
    "Environment": "dev",
    "Project": "mcp-app",
    "ManagedBy": "terraform"
  }
}
```

### Variables Description

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `environment` | Environment name (dev, staging, prod) | "dev" | No |
| `location` | Azure region | "westus" | No |
| `project_name` | Project name for resource naming | "mcp-app" | No |
| `source_code_path` | Path to source code with Dockerfile | - | Yes |
| `tags` | Common tags for all resources | {} | No |

## Outputs

After successful deployment, you'll get:

- **Container App URL**: The public URL of your deployed application
- **Container Registry Details**: Login server and registry name
- **Resource Group Information**: Name and ID
- **Log Analytics Workspace**: ID for monitoring

## Clean Up

To destroy all created resources:

```bash
terraform destroy -var-file="environments/dev.tfvars.json" -auto-approve
```

## Troubleshooting

### Common Issues

1. **Azure CLI not logged in**
   ```bash
   az login
   ```

2. **Insufficient permissions**
   - Ensure your Azure account has Contributor role on the subscription

3. **Container build fails**
   - Verify the Dockerfile exists in the source code path
   - Check that the Azure CLI is properly authenticated

4. **Resource naming conflicts**
   - Container Registry names must be globally unique
   - Modify the `project_name` variable if needed

### Logs and Monitoring

- Access application logs through Azure Portal > Container Apps > your app > Log stream
- Monitor performance through Log Analytics Workspace
- View container metrics in Azure Monitor

## Best Practices

1. **Version Control**: Keep your Terraform state in a remote backend (Azure Storage)
2. **Security**: Use Azure Key Vault for sensitive configuration values
3. **Monitoring**: Set up alerts and dashboards in Azure Monitor
4. **Cost Management**: Implement auto-scaling and resource optimization

## Additional Resources

- [Azure Container Apps Documentation](https://docs.microsoft.com/en-us/azure/container-apps/)
- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Azure CLI Reference](https://docs.microsoft.com/en-us/cli/azure/)