#!/bin/bash

# Script para probar todas las consultas de Azure DevOps REST API
# Basado en las consultas documentadas en ADO_API.md

set -e

# =====================================================
# CONFIGURACIÓN - Modifica estas variables
# =====================================================

AZURE_DEVOPS_ORG="https://dev.azure.com/villarrealm"
PROJECT_NAME="marcoLabs"
PAT_TOKEN=""  # Coloca tu Personal Access Token aquí

# =====================================================
# COLORES PARA OUTPUT
# =====================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# =====================================================
# FUNCIONES DE LOGGING
# =====================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_test() {
    echo -e "${CYAN}[TEST]${NC} $1"
}

log_result() {
    echo -e "${PURPLE}[RESULT]${NC} $1"
}

# =====================================================
# VALIDACIONES INICIALES
# =====================================================

validate_config() {
    log_info "Validando configuración..."
    
    if [ -z "$PAT_TOKEN" ]; then
        log_error "PAT_TOKEN está vacío. Configúralo en la parte superior del script."
        log_info "Para obtener un PAT, ve a: $AZURE_DEVOPS_ORG/_usersSettings/tokens"
        exit 1
    fi
    
    if [ -z "$AZURE_DEVOPS_ORG" ] || [ -z "$PROJECT_NAME" ]; then
        log_error "AZURE_DEVOPS_ORG o PROJECT_NAME están vacíos"
        exit 1
    fi
    
    # Validar formato del PAT (debe ser alfanumérico)
    if [[ ! "$PAT_TOKEN" =~ ^[A-Za-z0-9]+$ ]]; then
        log_warning "El PAT contiene caracteres especiales. Asegúrate de que sea correcto."
    fi
    
    log_success "Configuración válida"
    log_info "Organización: $AZURE_DEVOPS_ORG"
    log_info "Proyecto: $PROJECT_NAME"
    log_info "PAT Length: ${#PAT_TOKEN} caracteres"
}

# =====================================================
# CONFIGURACIÓN DE HEADERS
# =====================================================

setup_auth() {
    AUTH_HEADER="Authorization: Basic $(echo -n ":${PAT_TOKEN}" | base64)"
    CONTENT_TYPE="Content-Type: application/json"
    log_success "Headers de autenticación configurados"
    
    # Debug: Mostrar el header generado (sin mostrar el token completo)
    local auth_preview=$(echo -n ":${PAT_TOKEN}" | base64 | head -c 20)
    log_info "Auth Header Preview: Authorization: Basic ${auth_preview}..."
}

# =====================================================
# PRUEBA DE AUTENTICACIÓN SIMPLE
# =====================================================

test_auth() {
    log_info "Probando autenticación básica..."
    
    # Probar con el endpoint más simple primero
    local test_url="${AZURE_DEVOPS_ORG}/_apis/projects?api-version=7.1-preview.4"
    
    response=$(curl -s -w "%{http_code}" -X GET \
        "$test_url" \
        -H "$AUTH_HEADER" \
        -o /tmp/auth_test.json)
    
    http_code=$(echo "$response" | tail -c 4)
    
    if [ "$http_code" = "200" ]; then
        log_success "✅ Autenticación exitosa"
        if command -v jq >/dev/null 2>&1; then
            project_count=$(jq '.count // .value | length' /tmp/auth_test.json 2>/dev/null || echo "0")
            log_info "Proyectos accesibles: $project_count"
        fi
        return 0
    else
        log_error "❌ Fallo de autenticación (HTTP $http_code)"
        log_error "URL probada: $test_url"
        
        if [ "$http_code" = "401" ]; then
            log_error "El PAT es inválido o ha expirado"
            log_info "Soluciones:"
            log_info "  1. Verifica el PAT en: $AZURE_DEVOPS_ORG/_usersSettings/tokens"
            log_info "  2. Genera un nuevo PAT si el actual expiró"
            log_info "  3. Asegúrate de copiar el PAT completo sin espacios"
        fi
        
        cat /tmp/auth_test.json | head -5
        return 1
    fi
}

# =====================================================
# FUNCIÓN PARA MOSTRAR DETALLES EN TABLA
# =====================================================

show_work_items_table() {
    local work_items_file="$1"
    local title="$2"
    
    if [ ! -f "$work_items_file" ] || [ ! -s "$work_items_file" ]; then
        log_warning "No hay datos para mostrar"
        return
    fi
    
    if ! command -v jq >/dev/null 2>&1; then
        log_warning "jq no está instalado. Mostrando datos raw:"
        head -20 "$work_items_file"
        return
    fi
    
    local item_count=$(jq '.workItems | length // .value | length // 1' "$work_items_file" 2>/dev/null || echo "0")
    
    if [ "$item_count" = "0" ] || [ "$item_count" = "null" ]; then
        log_info "No se encontraron elementos"
        return
    fi
    
    echo ""
    echo -e "${PURPLE}📋 $title${NC}"
    echo -e "${PURPLE}════════════════════════════════════════════════════════════════${NC}"
    
    # Extraer IDs de work items
    local work_item_ids=$(jq -r '.workItems[]?.id // .value[]?.id // empty' "$work_items_file" 2>/dev/null | head -10)
    
    if [ -z "$work_item_ids" ]; then
        log_info "No se pudieron extraer IDs de work items"
        return
    fi
    
    # Crear lista de IDs separados por coma
    local ids_list=$(echo "$work_item_ids" | tr '\n' ',' | sed 's/,$//')
    
    if [ -n "$ids_list" ]; then
        # Obtener detalles de múltiples work items
        local details_response=$(curl -s -w "%{http_code}" \
            "${AZURE_DEVOPS_ORG}/_apis/wit/workitems?ids=${ids_list}&api-version=7.1-preview.3" \
            -H "$AUTH_HEADER" \
            -o /tmp/work_items_details.json)
        
        local details_http_code=$(echo "$details_response" | tail -c 4)
        
        if [ "$details_http_code" = "200" ]; then
            # Mostrar tabla formateada
            echo ""
            printf "%-8s %-15s %-40s %-12s %-15s\n" "ID" "TIPO" "TÍTULO" "ESTADO" "ASIGNADO"
            echo "────────────────────────────────────────────────────────────────────────────────────────────────"
            
            jq -r '.value[] | [
                .id,
                .fields."System.WorkItemType",
                (.fields."System.Title" | if length > 38 then .[0:35] + "..." else . end),
                .fields."System.State",
                (.fields."System.AssignedTo".displayName // "Sin asignar" | if length > 13 then .[0:10] + "..." else . end)
            ] | @tsv' /tmp/work_items_details.json 2>/dev/null | while IFS=$'\t' read -r id type title state assigned; do
                printf "%-8s %-15s %-40s %-12s %-15s\n" "$id" "$type" "$title" "$state" "$assigned"
            done
            
            echo ""
            log_success "Mostrando primeros 10 elementos de $item_count total"
        else
            log_error "Error obteniendo detalles (HTTP $details_http_code)"
        fi
    fi
}

# =====================================================
# FUNCIÓN AUXILIAR PARA CURL MEJORADA
# =====================================================

api_call() {
    local method="$1"
    local url="$2"
    local data="$3"
    local description="$4"
    local show_details="${5:-false}"
    
    log_test "$description"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -X "$method" \
            "$url" \
            -H "$AUTH_HEADER" \
            -H "$CONTENT_TYPE" \
            -d "$data" \
            -o /tmp/ado_response.json)
    else
        response=$(curl -s -w "%{http_code}" -X "$method" \
            "$url" \
            -H "$AUTH_HEADER" \
            -o /tmp/ado_response.json)
    fi
    
    http_code=$(echo "$response" | tail -c 4)
    
    if [ "$http_code" = "200" ]; then
        log_success "HTTP $http_code - OK"
        if command -v jq >/dev/null 2>&1; then
            result_count=$(jq '.workItems | length // .value | length // 1' /tmp/ado_response.json 2>/dev/null || echo "1")
            log_result "Elementos encontrados: $result_count"
            
            # Mostrar tabla detallada si se solicita y hay work items
            if [ "$show_details" = "true" ] && [ "$result_count" -gt 0 ] && [ "$result_count" != "null" ]; then
                show_work_items_table "/tmp/ado_response.json" "$description"
            elif [ "$result_count" != "null" ] && [ "$result_count" -gt 0 ]; then
                log_info "Muestra de IDs (usar show_details=true para ver tabla completa):"
                jq '.workItems[0:3] // .value[0:3] // .' /tmp/ado_response.json 2>/dev/null | head -10 || cat /tmp/ado_response.json | head -5
            fi
        else
            log_warning "jq no está instalado. Mostrando respuesta raw (primeras 10 líneas):"
            head -10 /tmp/ado_response.json
        fi
    elif [ "$http_code" = "401" ]; then
        log_error "HTTP $http_code - No autorizado"
        log_error "Posibles causas:"
        log_error "  1. PAT inválido o expirado"
        log_error "  2. PAT sin permisos suficientes"
        log_error "  3. Organización o proyecto incorrectos"
        log_info "Verifica tu PAT en: $AZURE_DEVOPS_ORG/_usersSettings/tokens"
        log_info "Respuesta del servidor:"
        cat /tmp/ado_response.json | head -5
        return 1
    elif [ "$http_code" = "403" ]; then
        log_error "HTTP $http_code - Prohibido"
        log_error "El PAT no tiene permisos suficientes para esta operación"
        log_info "Permisos requeridos: Work Items (Read), Code (Read), Project and Team (Read)"
        return 1
    elif [ "$http_code" = "404" ]; then
        log_error "HTTP $http_code - No encontrado"
        log_error "El proyecto '$PROJECT_NAME' no existe o no tienes acceso"
        log_info "Verifica que el proyecto existe en: $AZURE_DEVOPS_ORG"
        return 1
    else
        log_error "HTTP $http_code - Error"
        log_error "Respuesta: $(cat /tmp/ado_response.json)"
        return 1
    fi
    
    echo ""
    return 0
}

# =====================================================
# PRUEBAS DE CONEXIÓN BÁSICA
# =====================================================

test_basic_connection() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    PRUEBAS DE CONEXIÓN                       ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # Primero obtener el ID del proyecto
    log_info "Obteniendo información del proyecto..."
    project_response=$(curl -s -w "%{http_code}" \
        "${AZURE_DEVOPS_ORG}/_apis/projects?api-version=7.1-preview.4" \
        -H "$AUTH_HEADER" \
        -o /tmp/projects.json)
    
    project_http_code=$(echo "$project_response" | tail -c 4)
    
    if [ "$project_http_code" = "200" ]; then
        if command -v jq >/dev/null 2>&1; then
            PROJECT_ID=$(jq -r ".value[] | select(.name==\"$PROJECT_NAME\") | .id" /tmp/projects.json 2>/dev/null)
            if [ -n "$PROJECT_ID" ] && [ "$PROJECT_ID" != "null" ]; then
                log_success "Proyecto encontrado - ID: $PROJECT_ID"
                
                # Exportar PROJECT_ID para uso en otras funciones
                export PROJECT_ID
                
                # Probar acceso al proyecto específico usando ID
                api_call "GET" \
                    "${AZURE_DEVOPS_ORG}/_apis/projects/${PROJECT_ID}?api-version=7.1-preview.4" \
                    "" \
                    "Verificando acceso detallado al proyecto"
            else
                log_error "Proyecto '$PROJECT_NAME' no encontrado en la organización"
                log_info "Proyectos disponibles:"
                jq -r '.value[].name' /tmp/projects.json 2>/dev/null || cat /tmp/projects.json
                return 1
            fi
        fi
    else
        log_error "No se pudieron obtener los proyectos (HTTP $project_http_code)"
        return 1
    fi
}

# =====================================================
# PRUEBAS DE ÉPICAS
# =====================================================

test_epics() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    CONSULTAR ÉPICAS                         ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # 1. Obtener todas las épicas usando nombre del proyecto
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate] FROM WorkItems WHERE [System.WorkItemType] = 'Epic' AND [System.TeamProject] = '${PROJECT_NAME}'\"}" \
        "📋 Todas las épicas del proyecto" \
        "true"
    
    # 2. Obtener épicas con campos específicos
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.Description], [System.AreaPath], [System.AssignedTo] FROM WorkItems WHERE [System.WorkItemType] = 'Epic' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "📋 Épicas con detalles completos (ordenadas por fecha)" \
        "true"
}

# =====================================================
# PRUEBAS DE HISTORIAS DE USUARIO
# =====================================================

test_user_stories() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║              CONSULTAR HISTORIAS DE USUARIO                 ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # Primero obtener una épica para probar las relaciones
    log_info "Obteniendo ID de una épica para pruebas..."
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id] FROM WorkItems WHERE [System.WorkItemType] = 'Epic' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "Obteniendo ID de épica para pruebas"
    
    # Extraer ID de épica si existe
    if command -v jq >/dev/null 2>&1; then
        EPIC_ID=$(jq -r '.workItems[0].id // empty' /tmp/ado_response.json 2>/dev/null)
        if [ -n "$EPIC_ID" ] && [ "$EPIC_ID" != "null" ]; then
            log_success "Usando Épica ID: $EPIC_ID para pruebas de relaciones"
            
            # Obtener historias de la épica usando relaciones
            api_call "GET" \
                "${AZURE_DEVOPS_ORG}/_apis/wit/workitems/${EPIC_ID}?\$expand=relations&api-version=7.1-preview.3" \
                "" \
                "Obteniendo relaciones de la épica $EPIC_ID"
                
            # Obtener historias usando WIQL con links (método corregido)
            api_call "POST" \
                "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
                "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.Description] FROM WorkItemLinks WHERE [Source].[System.Id] = ${EPIC_ID} AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward' MODE (Recursive)\"}" \
                "Obteniendo historias de usuario de la épica $EPIC_ID via WIQL"
        else
            log_warning "No se encontraron épicas para probar relaciones"
        fi
    fi
    
    # Obtener todas las historias (Issues) independientemente
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate] FROM WorkItems WHERE [System.WorkItemType] = 'Issue' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "📋 Todas las historias de usuario (Issues)" \
        "true"
}

# =====================================================
# PRUEBAS DE TAREAS
# =====================================================

test_tasks() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    CONSULTAR TAREAS                         ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # Obtener una historia de usuario para probar
    log_info "Obteniendo ID de una historia de usuario para pruebas..."
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id] FROM WorkItems WHERE [System.WorkItemType] = 'Issue' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "Obteniendo ID de historia de usuario para pruebas"
    
    # Extraer ID de historia si existe
    if command -v jq >/dev/null 2>&1; then
        USER_STORY_ID=$(jq -r '.workItems[0].id // empty' /tmp/ado_response.json 2>/dev/null)
        if [ -n "$USER_STORY_ID" ] && [ "$USER_STORY_ID" != "null" ]; then
            log_success "Usando Historia ID: $USER_STORY_ID para pruebas de tareas"
            
            # Obtener tareas de la historia usando relaciones
            api_call "GET" \
                "${AZURE_DEVOPS_ORG}/_apis/wit/workitems/${USER_STORY_ID}?\$expand=relations&api-version=7.1-preview.3" \
                "" \
                "Obteniendo relaciones de la historia $USER_STORY_ID"
                
            # Obtener tareas usando WIQL (método corregido)
            api_call "POST" \
                "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
                "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.Description] FROM WorkItemLinks WHERE [Source].[System.Id] = ${USER_STORY_ID} AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward' MODE (Recursive)\"}" \
                "Obteniendo tareas de la historia $USER_STORY_ID via WIQL"
        else
            log_warning "No se encontraron historias de usuario para probar relaciones"
        fi
    fi
    
    # Obtener todas las tareas independientemente
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate] FROM WorkItems WHERE [System.WorkItemType] = 'Task' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "📋 Todas las tareas" \
        "true"
}

# =====================================================
# PRUEBAS DE REPOSITORIOS
# =====================================================

test_repositories() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                  CONSULTAR REPOSITORIOS                     ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # Obtener todos los repositorios usando PROJECT_ID
    api_call "GET" \
        "${AZURE_DEVOPS_ORG}/_apis/git/repositories?api-version=7.1-preview.1" \
        "" \
        "Obteniendo todos los repositorios del proyecto"
    
    # Si hay repositorios, probar con el primero
    if command -v jq >/dev/null 2>&1; then
        REPO_ID=$(jq -r '.value[0].id // empty' /tmp/ado_response.json 2>/dev/null)
        REPO_NAME=$(jq -r '.value[0].name // empty' /tmp/ado_response.json 2>/dev/null)
        
        if [ -n "$REPO_ID" ] && [ "$REPO_ID" != "null" ]; then
            log_success "Probando con repositorio: $REPO_NAME (ID: $REPO_ID)"
            
            # Obtener detalles específicos del repositorio
            api_call "GET" \
                "${AZURE_DEVOPS_ORG}/_apis/git/repositories/${REPO_ID}?api-version=7.1-preview.1" \
                "" \
                "Obteniendo detalles del repositorio $REPO_NAME"
            
            # Obtener branches del repositorio
            api_call "GET" \
                "${AZURE_DEVOPS_ORG}/_apis/git/repositories/${REPO_ID}/refs?api-version=7.1-preview.1" \
                "" \
                "Obteniendo branches del repositorio $REPO_NAME"
            
            # Obtener estadísticas del repositorio
            api_call "GET" \
                "${AZURE_DEVOPS_ORG}/_apis/git/repositories/${REPO_ID}/stats/branches?api-version=7.1-preview.1" \
                "" \
                "Obteniendo estadísticas del repositorio $REPO_NAME"
        else
            log_warning "No se encontraron repositorios en el proyecto"
        fi
    fi
}

# =====================================================
# CONSULTAS AVANZADAS
# =====================================================

test_advanced_queries() {
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                  CONSULTAS AVANZADAS                        ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    # Jerarquía completa (método corregido)
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State] FROM WorkItemLinks WHERE [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward' MODE (Recursive)\"}" \
        "Obteniendo jerarquía completa: Épicas → Historias → Tareas"
    
    # Work items por estado - Active
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State] FROM WorkItems WHERE [System.State] = 'Active' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "Obteniendo work items en estado 'Active'"
    
    # Work items por estado - New
    api_call "POST" \
        "${AZURE_DEVOPS_ORG}/_apis/wit/wiql?api-version=7.1-preview.2" \
        "{\"query\": \"SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State] FROM WorkItems WHERE [System.State] = 'New' AND [System.TeamProject] = '${PROJECT_NAME}' ORDER BY [System.CreatedDate] DESC\"}" \
        "Obteniendo work items en estado 'New'"
}

# =====================================================
# RESUMEN FINAL
# =====================================================

show_summary() {
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                      RESUMEN FINAL                          ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    log_success "Pruebas completadas exitosamente"
    log_info "Todas las consultas del ADO_API.md han sido probadas"
    log_info "Los archivos de respuesta temporal se han guardado en /tmp/ado_response.json"
    
    if ! command -v jq >/dev/null 2>&1; then
        log_warning "Recomendación: Instala 'jq' para mejor formateo de JSON:"
        log_info "  macOS: brew install jq"
        log_info "  Ubuntu: apt-get install jq"
    fi
    
    echo ""
    log_success "🎉 Todas las APIs de Azure DevOps están funcionando correctamente!"
}

# =====================================================
# MAIN - EJECUCIÓN PRINCIPAL
# =====================================================

main() {
    clear
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║              AZURE DEVOPS REST API TESTER                   ║${NC}"
    echo -e "${CYAN}║              Probando consultas de ADO_API.md                ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    validate_config
    setup_auth
    
    echo ""
    log_info "Iniciando pruebas de las APIs..."
    echo ""
    
    # Primero probar autenticación básica
    if ! test_auth; then
        log_error "❌ No se puede continuar sin autenticación válida"
        exit 1
    fi
    
    echo ""
    
    # Ejecutar todas las pruebas solo si la autenticación funciona
    test_basic_connection
    test_epics
    test_user_stories
    test_tasks
    test_repositories
    test_advanced_queries
    
    # Mostrar resumen
    show_summary
}

# Ejecutar el script
main "$@"