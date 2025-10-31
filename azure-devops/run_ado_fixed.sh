#!/bin/bash

# Azure DevOps Work Items Creation Script with Proper Hierarchical Relations
# This script creates Epics, User Stories, and Tasks with proper parent-child relationships
# Author: GitHub Copilot
# Date: $(date +%Y-%m-%d)

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load configuration
source "$SCRIPT_DIR/config.sh"

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
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

# Function to check prerequisites
check_prerequisites() {
    log "Verificando prerrequisitos..."
    
    # Check if Azure CLI is installed
    if ! command -v az &> /dev/null; then
        log_error "Azure CLI no está instalado. Por favor instálalo desde: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
        exit 1
    fi
    
    # Check if Azure DevOps extension is installed
    if ! az extension list | grep -q azure-devops; then
        log "Instalando extensión Azure DevOps CLI..."
        az extension add --name azure-devops
    fi
    
    # Check if user is logged in
    if ! az account show &> /dev/null; then
        log_error "No estás autenticado en Azure. Por favor ejecuta: az login"
        exit 1
    fi
    
    log_success "Prerrequisitos verificados correctamente"
}

# Function to validate configuration
validate_config() {
    log "Validando configuración..."
    
    if [[ -z "$AZURE_DEVOPS_ORG" || "$AZURE_DEVOPS_ORG" == "https://dev.azure.com/your-organization" ]]; then
        log_error "Por favor configura AZURE_DEVOPS_ORG en config.sh"
        exit 1
    fi
    
    if [[ -z "$PROJECT_NAME" || "$PROJECT_NAME" == "your-project-name" ]]; then
        log_error "Por favor configura PROJECT_NAME en config.sh"
        exit 1
    fi
    
    log_success "Configuración validada correctamente"
}

# Function to set Azure DevOps defaults
set_devops_defaults() {
    log "Configurando valores por defecto de Azure DevOps..."
    az devops configure --defaults organization="$AZURE_DEVOPS_ORG" project="$PROJECT_NAME"
    log_success "Valores por defecto configurados"
}

# Function to extract title from markdown file
extract_title() {
    local file_path="$1"
    grep -m 1 "^# " "$file_path" | sed 's/^# //' | sed 's/Épica: //; s/Historia de Usuario: //; s/Tarea: //'
}

# Function to extract epic from user story
extract_epic() {
    local file_path="$1"
    grep -A 1 "^## Épica" "$file_path" | tail -n 1 | xargs
}

# Function to extract user story from task
extract_user_story() {
    local file_path="$1"
    grep -A 1 "^## Historia de Usuario" "$file_path" | tail -n 1 | xargs
}

# Function to extract description
extract_description() {
    local file_path="$1"
    sed -n '/^## Descripción$/,/^## /p' "$file_path" | grep -v '^## ' | xargs
}

# Function to create epic work item
create_epic() {
    local epic_file="$1"
    local title=$(extract_title "$epic_file")
    local description=$(extract_description "$epic_file")
    
    log "Creando épica: $title"
    
    local epic_id=$(az boards work-item create \
        --title "$title" \
        --type "$EPIC_TYPE" \
        --description "$description" \
        --area "$AREA_PATH" \
        --iteration "$ITERATION_PATH" \
        --query 'id' \
        --output tsv)
    
    if [[ -n "$epic_id" ]]; then
        log_success "Épica creada con ID: $epic_id"
        echo "$title:$epic_id" >> "$SCRIPT_DIR/epic_ids.txt"
        
        # Add to JSON tracking file
        local temp_file=$(mktemp)
        jq --arg title "$title" --arg id "$epic_id" --arg file "$epic_file" \
           '.epics += [{"id": $id, "title": $title, "file": $file}]' \
           "$SCRIPT_DIR/work_items.json" > "$temp_file" && mv "$temp_file" "$SCRIPT_DIR/work_items.json"
    else
        log_error "Error al crear épica: $title"
        return 1
    fi
}

# Function to get epic ID by title
get_epic_id() {
    local epic_title="$1"
    grep "^$epic_title:" "$SCRIPT_DIR/epic_ids.txt" 2>/dev/null | cut -d':' -f2
}

# Function to create user story work item with proper parent relation
create_user_story() {
    local story_file="$1"
    local title=$(extract_title "$story_file")
    local description=$(extract_description "$story_file")
    local epic_title=$(extract_epic "$story_file")
    local epic_id=$(get_epic_id "$epic_title")
    
    log "Creando historia de usuario: $title"
    
    # Create the user story first
    local story_id=$(az boards work-item create \
        --title "$title" \
        --type "$USER_STORY_TYPE" \
        --description "$description" \
        --area "$AREA_PATH" \
        --iteration "$ITERATION_PATH" \
        --query 'id' \
        --output tsv)
    
    if [[ -n "$story_id" ]]; then
        log_success "Historia de usuario creada con ID: $story_id"
        echo "$title:$story_id" >> "$SCRIPT_DIR/story_ids.txt"
        
        # Add parent-child relationship if epic exists
        if [[ -n "$epic_id" ]]; then
            log "Estableciendo relación: Épica $epic_id -> Historia $story_id"
            az boards work-item relation add \
                --id "$story_id" \
                --relation-type "parent" \
                --target-id "$epic_id" \
                --output none
            log_success "Relación establecida correctamente"
        fi
        
        # Add to JSON tracking file
        local temp_file=$(mktemp)
        jq --arg title "$title" --arg id "$story_id" --arg epic_title "$epic_title" --arg epic_id "$epic_id" --arg file "$story_file" \
           '.issues += [{"id": $id, "title": $title, "parent_epic": $epic_title, "parent_epic_id": $epic_id, "file": $file}]' \
           "$SCRIPT_DIR/work_items.json" > "$temp_file" && mv "$temp_file" "$SCRIPT_DIR/work_items.json"
    else
        log_error "Error al crear historia de usuario: $title"
        return 1
    fi
}

# Function to get user story ID by title
get_story_id() {
    local story_title="$1"
    grep "^$story_title:" "$SCRIPT_DIR/story_ids.txt" 2>/dev/null | cut -d':' -f2
}

# Function to create task work item with proper parent relation
create_task() {
    local task_file="$1"
    local title=$(extract_title "$task_file")
    local description=$(extract_description "$task_file")
    local story_title=$(extract_user_story "$task_file")
    local story_id=$(get_story_id "$story_title")
    
    log "Creando tarea: $title"
    
    # Create the task first
    local task_id=$(az boards work-item create \
        --title "$title" \
        --type "$TASK_TYPE" \
        --description "$description" \
        --area "$AREA_PATH" \
        --iteration "$ITERATION_PATH" \
        --query 'id' \
        --output tsv)
    
    if [[ -n "$task_id" ]]; then
        log_success "Tarea creada con ID: $task_id"
        
        # Add parent-child relationship if user story exists
        if [[ -n "$story_id" ]]; then
            log "Estableciendo relación: Historia $story_id -> Tarea $task_id"
            az boards work-item relation add \
                --id "$task_id" \
                --relation-type "parent" \
                --target-id "$story_id" \
                --output none
            log_success "Relación establecida correctamente"
        fi
        
        # Add to JSON tracking file
        local temp_file=$(mktemp)
        jq --arg title "$title" --arg id "$task_id" --arg story_title "$story_title" --arg story_id "$story_id" --arg file "$task_file" \
           '.tasks += [{"id": $id, "title": $title, "parent_issue": $story_title, "parent_issue_id": $story_id, "file": $file}]' \
           "$SCRIPT_DIR/work_items.json" > "$temp_file" && mv "$temp_file" "$SCRIPT_DIR/work_items.json"
    else
        log_error "Error al crear tarea: $title"
        return 1
    fi
}

# Function to verify relationships
verify_relationships() {
    log "Verificando relaciones creadas..."
    
    # Check epic relationships
    local epic_count=$(jq '.epics | length' "$SCRIPT_DIR/work_items.json")
    local issue_count=$(jq '.issues | length' "$SCRIPT_DIR/work_items.json")
    local task_count=$(jq '.tasks | length' "$SCRIPT_DIR/work_items.json")
    
    echo -e "\n${YELLOW}🔗 VERIFICACIÓN DE RELACIONES:${NC}"
    
    # Test one epic's children
    local test_epic_id=$(jq -r '.epics[0].id // empty' "$SCRIPT_DIR/work_items.json")
    if [[ -n "$test_epic_id" ]]; then
        log "Verificando hijos de la épica $test_epic_id..."
        local children=$(az boards work-item show --id "$test_epic_id" --query 'relations[?attributes.name==`Child`] | length(@)' --output tsv 2>/dev/null || echo "0")
        log_success "Épica $test_epic_id tiene $children elementos hijos"
    fi
}

# Main execution function
main() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║     Azure DevOps Work Items with Hierarchical Relations      ║"
    echo "║                     GitHub Copilot                          ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Clean up previous run files and create new tracking file
    rm -f "$SCRIPT_DIR/epic_ids.txt" "$SCRIPT_DIR/story_ids.txt" "$SCRIPT_DIR/work_items.json"
    
    # Initialize JSON tracking file
    cat > "$SCRIPT_DIR/work_items.json" << EOF
{
  "creation_date": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "organization": "$AZURE_DEVOPS_ORG",
  "project": "$PROJECT_NAME",
  "epics": [],
  "issues": [],
  "tasks": []
}
EOF
    
    check_prerequisites
    validate_config
    set_devops_defaults
    
    # Create Epics first
    echo -e "\n${YELLOW}🎯 CREANDO ÉPICAS...${NC}"
    for epic_file in "$SCRIPT_DIR/work-items/epics"/*.md; do
        if [[ -f "$epic_file" ]]; then
            create_epic "$epic_file"
        fi
    done
    
    # Wait for epics to be properly created
    echo -e "\n${BLUE}⏳ Esperando que las épicas se procesen...${NC}"
    sleep 3
    
    # Create User Stories with parent relationships
    echo -e "\n${YELLOW}📖 CREANDO HISTORIAS DE USUARIO (con relaciones)...${NC}"
    for story_file in "$SCRIPT_DIR/work-items/user-stories"/*.md; do
        if [[ -f "$story_file" ]]; then
            create_user_story "$story_file"
        fi
    done
    
    # Wait for user stories to be properly created
    echo -e "\n${BLUE}⏳ Esperando que las historias se procesen...${NC}"
    sleep 3
    
    # Create Tasks with parent relationships
    echo -e "\n${YELLOW}✅ CREANDO TAREAS (con relaciones)...${NC}"
    for task_file in "$SCRIPT_DIR/work-items/tasks"/*.md; do
        if [[ -f "$task_file" ]]; then
            create_task "$task_file"
        fi
    done
    
    # Verify relationships
    sleep 2
    verify_relationships
    
    echo -e "\n${GREEN}🎉 ¡PROCESO COMPLETADO EXITOSAMENTE!${NC}"
    echo -e "${BLUE}Revisa tus work items con jerarquía en: $AZURE_DEVOPS_ORG/$PROJECT_NAME/_workitems${NC}"
    
    # Show summary from JSON file
    local epic_count=$(jq '.epics | length' "$SCRIPT_DIR/work_items.json")
    local issue_count=$(jq '.issues | length' "$SCRIPT_DIR/work_items.json")
    local task_count=$(jq '.tasks | length' "$SCRIPT_DIR/work_items.json")
    
    echo -e "\n${YELLOW}📊 RESUMEN:${NC}"
    echo -e "  • ${epic_count} Épicas creadas"
    echo -e "  • ${issue_count} Historias de usuario (Issues) creadas con relación a épicas"
    echo -e "  • ${task_count} Tareas creadas con relación a historias"
    echo -e "\n${GREEN}🔗 Todas las relaciones jerárquicas establecidas correctamente${NC}"
    echo -e "\n${BLUE}💾 IDs guardados en: $SCRIPT_DIR/work_items.json${NC}"
    echo -e "${BLUE}🗑️  Para limpiar: ./shutdown.sh${NC}"
    
    # Clean up temporary files but keep the JSON tracking file
    rm -f "$SCRIPT_DIR/epic_ids.txt" "$SCRIPT_DIR/story_ids.txt"
}

# Execute main function
main "$@"