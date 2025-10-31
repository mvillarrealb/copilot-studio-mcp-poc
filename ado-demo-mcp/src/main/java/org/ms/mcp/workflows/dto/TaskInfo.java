package org.ms.mcp.workflows.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO específico para información de Tareas
 * Extiende la información básica con campos específicos de tareas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInfo {
    
    private Long id;
    private String title;
    private String state;
    private String description;
    private String assignedTo;
    private String areaPath;
    private LocalDateTime createdDate;
    private LocalDateTime changedDate;
    
    // Campos específicos de tareas
    private Double remainingWork;
    private Double originalEstimate;
    private Double completedWork;
    private String activity;
    private String priority;
    private Long parentUserStoryId;
    private Long parentEpicId;
    
    /**
     * Indica si la tarea está en progreso
     */
    public boolean isInProgress() {
        return "Active".equalsIgnoreCase(state) || "In Progress".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la tarea está completada
     */
    public boolean isCompleted() {
        return "Closed".equalsIgnoreCase(state) || "Done".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la tarea es nueva
     */
    public boolean isNew() {
        return "New".equalsIgnoreCase(state);
    }
    
    /**
     * Calcula el porcentaje de progreso de la tarea
     */
    public Double getProgressPercentage() {
        if (originalEstimate == null || originalEstimate == 0) {
            return isCompleted() ? 100.0 : 0.0;
        }
        
        if (completedWork == null) {
            return 0.0;
        }
        
        return Math.min(100.0, (completedWork / originalEstimate) * 100.0);
    }
    
    /**
     * Indica si la tarea tiene estimación de tiempo
     */
    public boolean hasTimeEstimate() {
        return originalEstimate != null && originalEstimate > 0;
    }
}