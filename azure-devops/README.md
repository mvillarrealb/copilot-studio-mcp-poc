# Azure DevOps Work Items Automation

Este proyecto automatiza la creación de work items (épicas, historias de usuario y tareas) en Azure DevOps utilizando Azure CLI.

## 📁 Estructura del Proyecto

```
azure-devops/
├── config.sh                 # Configuración del proyecto
├── run_ado.sh                # Script principal ejecutable
├── README.md                 # Esta documentación
└── work-items/
    ├── epics/                # Definiciones de épicas
    │   ├── 01-infraestructura-como-codigo.md
    │   ├── 02-ci-cd.md
    │   └── 03-proyecto-fullstack.md
    ├── user-stories/         # Definiciones de historias de usuario
    │   ├── 01-iac-terraform-modules.md
    │   ├── 02-iac-remote-state.md
    │   ├── 03-iac-automation.md
    │   ├── 04-cicd-build-pipeline.md
    │   ├── 05-cicd-release-pipeline.md
    │   ├── 06-cicd-monitoring.md
    │   ├── 07-fullstack-api-backend.md
    │   ├── 08-fullstack-frontend-ui.md
    │   └── 09-fullstack-database.md
    └── tasks/                # Definiciones de tareas
        ├── 01-create-resource-group-module.md
        ├── 02-create-storage-account-module.md
        ├── 03-configure-terraform-backend.md
        ├── 04-implement-state-locking.md
        ├── 05-create-build-pipeline.md
        └── 06-configure-artifacts-publishing.md
```

## 🎯 Work Items Incluidos

### 📋 3 Épicas:
1. **Infraestructura como Código** - Implementación de IaC con Terraform
2. **Integración Continua y Despliegue Continuo (CI/CD)** - Automatización de pipelines
3. **Proyecto Fullstack** - Desarrollo de aplicación web completa

### 📖 9 Historias de Usuario (3 por épica):
- **Infraestructura como Código:**
  - Configurar Módulos de Terraform para Azure
  - Implementar Estado Remoto de Terraform
  - Automatizar Despliegue de Infraestructura

- **CI/CD:**
  - Configurar Pipeline de Construcción
  - Implementar Pipeline de Despliegue
  - Configurar Monitoreo de Pipelines

- **Proyecto Fullstack:**
  - Desarrollar API REST Backend
  - Crear Interfaz de Usuario Responsiva
  - Implementar Capa de Persistencia

### ✅ 6+ Tareas (2+ por historia de usuario):
Cada historia de usuario incluye tareas técnicas específicas con criterios de aceptación detallados.

## 🚀 Prerrequisitos

1. **Azure CLI** instalado y configurado
   ```bash
   # En macOS
   brew install azure-cli
   
   # En Windows
   winget install Microsoft.AzureCLI
   ```

2. **Extensión Azure DevOps** para Azure CLI
   ```bash
   az extension add --name azure-devops
   ```

3. **Autenticación en Azure**
   ```bash
   az login
   ```

4. **Proyecto en Azure DevOps** existente con permisos para crear work items

## ⚙️ Configuración

1. **Editar `config.sh`** con los valores de tu organización:
   ```bash
   # Azure DevOps Organization URL
   AZURE_DEVOPS_ORG="https://dev.azure.com/tu-organizacion"
   
   # Project name in Azure DevOps
   PROJECT_NAME="tu-proyecto"
   
   # Area Path (opcional)
   AREA_PATH="tu-proyecto"
   
   # Iteration Path (opcional)
   ITERATION_PATH="tu-proyecto"
   ```

2. **Verificar permisos** en Azure DevOps:
   - Contributor o superior en el proyecto
   - Permisos para crear work items

## 🎮 Uso

### Ejecución Completa
```bash
cd azure-devops
./run_ado.sh
```

### Salida Esperada
```
╔══════════════════════════════════════════════════════════════╗
║           Azure DevOps Work Items Creation Script            ║
║                     GitHub Copilot                          ║
╚══════════════════════════════════════════════════════════════╝

[2025-10-31 10:30:15] Verificando prerrequisitos...
[SUCCESS] Prerrequisitos verificados correctamente

[2025-10-31 10:30:16] Validando configuración...
[SUCCESS] Configuración validada correctamente

🎯 CREANDO ÉPICAS...
[2025-10-31 10:30:18] Creando épica: Infraestructura como Código
[SUCCESS] Épica creada con ID: 1001

📖 CREANDO HISTORIAS DE USUARIO...
[2025-10-31 10:30:25] Creando historia de usuario: Configurar Módulos de Terraform para Azure
[SUCCESS] Historia de usuario creada con ID: 1002

✅ CREANDO TAREAS...
[2025-10-31 10:30:35] Creando tarea: Crear módulo de Resource Group con variables de ubicación y tags
[SUCCESS] Tarea creada con ID: 1003

🎉 ¡PROCESO COMPLETADO EXITOSAMENTE!
Revisa tus work items en: https://dev.azure.com/tu-organizacion/tu-proyecto/_workitems
```

## � **Características Avanzadas**

- ✅ **Validación automática** de prerrequisitos
- ✅ **Vinculación jerárquica** (Épica → Historia → Tarea)  
- ✅ **Logging con colores** y timestamps
- ✅ **Manejo robusto de errores**
- ✅ **Configuración centralizada**
- ✅ **Tracking de IDs en JSON** para laboratorio reproducible
- ✅ **Script de limpieza** automática (`shutdown.sh`)
- ✅ **Backup automático** antes de eliminar
- ✅ **Documentación completa**

## 🧪 **Laboratorio Reproducible**

Este proyecto incluye un **laboratorio completamente reproducible**:

### 🚀 **Crear Entorno**
```bash
./run_ado.sh
```
- Crea 18 work items (3 épicas + 9 issues + 6 tareas)
- Guarda todos los IDs en `work_items.json`
- Vincula jerárquicamente los elementos

### 🗑️ **Limpiar Entorno**  
```bash
./shutdown.sh
```
- Elimina TODOS los work items creados
- Backup automático antes de eliminar
- Confirmación requerida para seguridad

### 📊 **Tracking Completo**
- **IDs guardados** en formato JSON estructurado
- **Relaciones padre-hijo** preservadas
- **Metadatos completos** (fechas, archivos fuente, etc.)

Ver documentación completa en **[LABORATORIO.md](./LABORATORIO.md)**

## 📝 Personalización

### Agregar Nuevos Work Items
1. **Crear archivo markdown** en la carpeta correspondiente
2. **Seguir la estructura** de los archivos existentes
3. **Ejecutar el script** nuevamente

### Modificar Work Items Existentes
1. **Editar archivos markdown** según necesidades
2. **Mantener la estructura** de encabezados
3. **Re-ejecutar el script** (creará nuevos work items)

### Estructura de Archivos Markdown

#### Épicas
```markdown
# Épica: [Título]
## Descripción
[Descripción detallada]
## Criterios de Aceptación
- [ ] Criterio 1
## Valor de Negocio
[Valor esperado]
```

#### Historias de Usuario
```markdown
# Historia de Usuario: [Título]
## Épica
[Nombre de la épica padre]
## Descripción
**Como** [rol]
**Quiero** [funcionalidad]
**Para** [beneficio]
```

#### Tareas
```markdown
# Tarea: [Título]
## Historia de Usuario
[Nombre de la historia padre]
## Descripción
[Descripción técnica]
```

## 🐛 Troubleshooting

### Errores Comunes

1. **"Azure CLI no está instalado"**
   ```bash
   # Instalar Azure CLI
   brew install azure-cli  # macOS
   # o
   winget install Microsoft.AzureCLI  # Windows
   ```

2. **"No estás autenticado en Azure"**
   ```bash
   az login
   ```

3. **"Error al crear work item"**
   - Verificar permisos en Azure DevOps
   - Verificar que el proyecto existe
   - Revisar configuración en `config.sh`

4. **"Extensión Azure DevOps no encontrada"**
   ```bash
   az extension add --name azure-devops
   ```
