package org.ms.mcp.workflows.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO específico para información de Historias de Usuario
 * Extiende la información básica con campos específicos de historias
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryInfo {
    
    private Long id;
    private String title;
    private String state;
    private String description;
    private String assignedTo;
    private String areaPath;
    private LocalDateTime createdDate;
    private LocalDateTime changedDate;
    
    // Campos específicos de historias de usuario
    private String acceptanceCriteria;
    private Integer storyPoints;
    private String priority;
    private String riskLevel;
    private Long parentEpicId;
    
    /**
     * Indica si la historia está en progreso
     */
    public boolean isInProgress() {
        return "Active".equalsIgnoreCase(state) || "In Progress".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la historia está completada
     */
    public boolean isCompleted() {
        return "Closed".equalsIgnoreCase(state) || "Done".equalsIgnoreCase(state) || "Resolved".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la historia es nueva
     */
    public boolean isNew() {
        return "New".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la historia tiene story points asignados
     */
    public boolean hasStoryPoints() {
        return storyPoints != null && storyPoints > 0;
    }
}