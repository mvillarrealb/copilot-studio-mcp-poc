# Flujo de Trabajo - Azure DevOps API

## Configuración Inicial

Antes de realizar cualquier consulta, es necesario configurar las variables de autenticación:

```bash
AZURE_DEVOPS_ORG="https://dev.azure.com/tu-organizacion"
PROJECT_NAME="tu-proyecto"
PAT_TOKEN="tu-personal-access-token"
AUTH_HEADER="Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"
```

## 1. Flujo de Trabajo: Dado una Epic ID

### 1.1 Buscar las Historias Asociadas a esa Épica

**Paso 1: Obtener detalles de la Épica y sus relaciones**

```bash
curl -s -X GET \
  "${AZURE_DEVOPS_ORG}/_apis/wit/workitems/{EPIC_ID}?\$expand=relations&api-version=7.1-preview.3" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json"
```

**Paso 2: Buscar historias usando WIQL (Work Item Query Language)**

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State], [System.Description] FROM WorkItemLinks WHERE [Source].[System.Id] = {EPIC_ID} AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' MODE (Recursive)"
  }'
```

### 1.2 Buscar las Tareas Asociadas a esa Épica

**Paso 1: Obtener todas las tareas relacionadas directa o indirectamente con la épica**

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] FROM WorkItemLinks WHERE [Source].[System.Id] = {EPIC_ID} AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Task'\'' MODE (Recursive)"
  }'
```

**Paso 2: Obtener tareas a través de las historias de usuario (enfoque de dos pasos)**

Primero obtener las historias:
```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id] FROM WorkItemLinks WHERE [Source].[System.Id] = {EPIC_ID} AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Issue'\''"
  }'
```

Luego, para cada historia ID obtenida, buscar sus tareas:
```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItemLinks WHERE [Source].[System.Id] = {USER_STORY_ID} AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Task'\''"
  }'
```

## 2. Flujo de Trabajo: Dado un Epic Name (parcial)

### 2.1 Buscar las Épicas que Coincidan con ese Nombre Parcial

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate] FROM WorkItems WHERE [System.WorkItemType] = '\''Epic'\'' AND [System.TeamProject] = '\''${PROJECT_NAME}'\'' AND [System.Title] CONTAINS '\''NOMBRE_PARCIAL_EPIC'\'' ORDER BY [System.CreatedDate] DESC"
  }'
```

### 2.2 Buscar las Historias Asociadas a esas Épicas

**Paso 1: Para cada Epic ID obtenido en el paso anterior, ejecutar:**

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State], [System.Description] FROM WorkItemLinks WHERE [Source].[System.Id] IN ({EPIC_ID_1}, {EPIC_ID_2}, {EPIC_ID_N}) AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Issue'\'' MODE (Recursive)"
  }'
```

### 2.3 Buscar las Tareas Asociadas a esas Épicas

**Paso 1: Obtener todas las tareas de múltiples épicas en una sola consulta**

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] FROM WorkItemLinks WHERE [Source].[System.Id] IN ({EPIC_ID_1}, {EPIC_ID_2}, {EPIC_ID_N}) AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Task'\'' MODE (Recursive)"
  }'
```

## 3. Consultas Complementarias

### 3.1 Obtener Detalles Completos de Work Items

Una vez que tienes una lista de IDs, puedes obtener detalles completos:

```bash
curl -s -X GET \
  "${AZURE_DEVOPS_ORG}/_apis/wit/workitems?ids={ID1},{ID2},{ID3}&api-version=7.1-preview.3" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"
```

### 3.2 Obtener Jerarquía Completa

Para obtener toda la jerarquía de work items:

```bash
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State] FROM WorkItemLinks WHERE [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' MODE (Recursive)"
  }'
```

### 3.3 Verificar Autenticación y Acceso al Proyecto

**Obtener información del proyecto:**
```bash
curl -s -X GET \
  "${AZURE_DEVOPS_ORG}/_apis/projects?api-version=7.1-preview.4" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"
```

**Obtener detalles específicos del proyecto:**
```bash
curl -s -X GET \
  "${AZURE_DEVOPS_ORG}/_apis/projects/${PROJECT_ID}?api-version=7.1-preview.4" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"
```

## 4. Ejemplos de Uso Completo

### Ejemplo 1: Buscar todo el contenido de la épica con ID 123

```bash
# 1. Obtener detalles de la épica
EPIC_ID=123
curl -s -X GET \
  "${AZURE_DEVOPS_ORG}/_apis/wit/workitems/${EPIC_ID}?\$expand=relations&api-version=7.1-preview.3" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"

# 2. Obtener todas las historias de la épica
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d "{
    \"query\": \"SELECT [System.Id], [System.Title], [System.State] FROM WorkItemLinks WHERE [Source].[System.Id] = ${EPIC_ID} AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward' AND [Target].[System.WorkItemType] = 'Issue' MODE (Recursive)\"
  }"

# 3. Obtener todas las tareas de la épica
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d "{
    \"query\": \"SELECT [System.Id], [System.Title], [System.State] FROM WorkItemLinks WHERE [Source].[System.Id] = ${EPIC_ID} AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward' AND [Target].[System.WorkItemType] = 'Task' MODE (Recursive)\"
  }"
```

### Ejemplo 2: Buscar épicas por nombre parcial "infraestructura"

```bash
# 1. Buscar épicas que contengan "infraestructura" en el título
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItems WHERE [System.WorkItemType] = '\''Epic'\'' AND [System.TeamProject] = '\''marcoLabs'\'' AND [System.Title] CONTAINS '\''infraestructura'\'' ORDER BY [System.CreatedDate] DESC"
  }'

# 2. Usar los IDs obtenidos para buscar historias (ejemplo con IDs 101,102,103)
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItemLinks WHERE [Source].[System.Id] IN (101, 102, 103) AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Issue'\'' MODE (Recursive)"
  }'

# 3. Buscar tareas de esas épicas
curl -s -X POST \
  "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItemLinks WHERE [Source].[System.Id] IN (101, 102, 103) AND [System.Links.LinkType] = '\''System.LinkTypes.Hierarchy-Forward'\'' AND [Target].[System.WorkItemType] = '\''Task'\'' MODE (Recursive)"
  }'
```

## 5. Manejo de Errores Comunes

### Error 401 - No autorizado
- Verificar que el PAT sea válido y no haya expirado
- Asegurar que el PAT tenga permisos de lectura para Work Items

### Error 403 - Prohibido
- El PAT no tiene permisos suficientes
- Permisos requeridos: Work Items (Read), Code (Read), Project and Team (Read)

### Error 404 - No encontrado
- El proyecto especificado no existe o no tienes acceso
- Verificar el nombre del proyecto y la organización

## 6. Notas Importantes

1. **Tipos de Work Items**: En Azure DevOps, los tipos más comunes son:
   - `Epic`: Épicas
   - `Issue`: Historias de usuario (en algunos proyectos puede ser `User Story`)
   - `Task`: Tareas

2. **Relaciones**: Se utiliza `System.LinkTypes.Hierarchy-Forward` para obtener elementos hijos.

3. **Modo Recursivo**: El `MODE (Recursive)` en WIQL permite obtener toda la jerarquía de elementos hijos.

4. **Límites de Consulta**: Las consultas WIQL tienen límites en el número de resultados. Para grandes conjuntos de datos, puede ser necesario paginar.

5. **Autenticación**: Siempre usar HTTPS y mantener los PAT seguros.