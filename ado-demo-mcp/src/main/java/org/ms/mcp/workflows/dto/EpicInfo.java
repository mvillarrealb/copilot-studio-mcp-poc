package org.ms.mcp.workflows.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO específico para información de Épicas
 * Extiende la información básica con campos específicos de épicas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicInfo {
    
    private Long id;
    private String title;
    private String state;
    private String description;
    private String assignedTo;
    private String areaPath;
    private LocalDateTime createdDate;
    private LocalDateTime changedDate;
    
    // Campos específicos de épicas
    private String priority;
    private String businessValue;
    private String acceptanceCriteria;
    private LocalDateTime startDate;
    private LocalDateTime targetDate;
    
    /**
     * Indica si la épica está activa
     */
    public boolean isActive() {
        return "Active".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la épica está completada
     */
    public boolean isCompleted() {
        return "Closed".equalsIgnoreCase(state) || "Done".equalsIgnoreCase(state);
    }
    
    /**
     * Indica si la épica es nueva
     */
    public boolean isNew() {
        return "New".equalsIgnoreCase(state);
    }
}