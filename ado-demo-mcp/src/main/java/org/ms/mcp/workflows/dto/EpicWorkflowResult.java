package org.ms.mcp.workflows.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO UNIFICADO que encapsula el resultado de workflows de épica
 * Funciona para AMBOS flujos:
 * - Flujo 1 (por ID): Lista con 1 épica
 * - Flujo 2 (por nombre): Lista con N épicas
 * 
 * RESPUESTA ESTÁNDAR para ambos casos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicWorkflowResult {
    
    // Información de búsqueda
    private String searchType;        // "BY_ID" o "BY_PARTIAL_NAME"
    private String searchValue;       // El ID o nombre buscado
    
    // Lista de épicas encontradas (1 para BY_ID, N para BY_PARTIAL_NAME)
    @Builder.Default
    private List<EpicData> epics = new ArrayList<>();
    
    // Metadatos del workflow
    private WorkflowMetadata metadata;
    
    /**
     * Datos de una épica individual con sus historias y tareas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpicData {
        private EpicInfo epic;
        
        @Builder.Default
        private List<UserStoryInfo> userStories = new ArrayList<>();
        
        @Builder.Default
        private List<TaskInfo> tasks = new ArrayList<>();
    }
    
    
    /**
     * Obtiene el número total de épicas encontradas
     */
    public int getTotalEpics() {
        return epics != null ? epics.size() : 0;
    }
    
    /**
     * Obtiene el número total de historias de usuario en todas las épicas
     */
    public int getTotalUserStories() {
        return epics != null ? 
                epics.stream().mapToInt(epic -> epic.getUserStories().size()).sum() : 0;
    }
    
    /**
     * Obtiene el número total de tareas en todas las épicas
     */
    public int getTotalTasks() {
        return epics != null ? 
                epics.stream().mapToInt(epic -> epic.getTasks().size()).sum() : 0;
    }
    
    /**
     * Obtiene el número de historias completadas en todas las épicas
     */
    public long getCompletedUserStories() {
        return epics != null ? 
                epics.stream()
                     .flatMap(epic -> epic.getUserStories().stream())
                     .filter(UserStoryInfo::isCompleted)
                     .count() : 0;
    }
    
    /**
     * Obtiene el número de tareas completadas en todas las épicas
     */
    public long getCompletedTasks() {
        return epics != null ? 
                epics.stream()
                     .flatMap(epic -> epic.getTasks().stream())
                     .filter(TaskInfo::isCompleted)
                     .count() : 0;
    }
    
    /**
     * Indica si es búsqueda por ID (un solo resultado esperado)
     */
    public boolean isByIdSearch() {
        return "BY_ID".equals(searchType);
    }
    
    /**
     * Indica si es búsqueda por nombre parcial (múltiples resultados esperados)
     */
    public boolean isByNameSearch() {
        return "BY_PARTIAL_NAME".equals(searchType);
    }
    
    /**
     * Obtiene la primera épica (útil para búsquedas por ID)
     */
    public EpicData getFirstEpic() {
        return (epics != null && !epics.isEmpty()) ? epics.get(0) : null;
    }
    
    /**
     * Obtiene un resumen textual del resultado
     */
    public String getSummary() {
        return String.format(
                "Búsqueda %s '%s': %d épicas, %d historias (%d completadas), %d tareas (%d completadas)",
                searchType,
                searchValue,
                getTotalEpics(),
                getTotalUserStories(),
                getCompletedUserStories(),
                getTotalTasks(),
                getCompletedTasks()
        );
    }
    
    /**
     * Metadatos adicionales del workflow
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowMetadata {
        private Long executionTimeMs;
        private int apiCallsCount;
        private boolean hasErrors;
        private String errorMessage;
    }
}