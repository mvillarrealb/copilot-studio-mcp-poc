package org.ms.mcp.workflows.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO base para información de Work Items
 * Contiene los campos comunes a todos los tipos de work items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemInfo {
    
    private Long id;
    private String title;
    private String state;
    private String workItemType;
    private String description;
    private String assignedTo;
    private String areaPath;
    private LocalDateTime createdDate;
    private LocalDateTime changedDate;
    
    /**
     * Método factory para crear WorkItemInfo desde un tipo específico
     */
    public static WorkItemInfo from(EpicInfo epic) {
        return WorkItemInfo.builder()
                .id(epic.getId())
                .title(epic.getTitle())
                .state(epic.getState())
                .workItemType("Epic")
                .description(epic.getDescription())
                .assignedTo(epic.getAssignedTo())
                .areaPath(epic.getAreaPath())
                .createdDate(epic.getCreatedDate())
                .changedDate(epic.getChangedDate())
                .build();
    }
    
    /**
     * Método factory para crear WorkItemInfo desde una UserStory
     */
    public static WorkItemInfo from(UserStoryInfo userStory) {
        return WorkItemInfo.builder()
                .id(userStory.getId())
                .title(userStory.getTitle())
                .state(userStory.getState())
                .workItemType("Issue")
                .description(userStory.getDescription())
                .assignedTo(userStory.getAssignedTo())
                .areaPath(userStory.getAreaPath())
                .createdDate(userStory.getCreatedDate())
                .changedDate(userStory.getChangedDate())
                .build();
    }
    
    /**
     * Método factory para crear WorkItemInfo desde una Task
     */
    public static WorkItemInfo from(TaskInfo task) {
        return WorkItemInfo.builder()
                .id(task.getId())
                .title(task.getTitle())
                .state(task.getState())
                .workItemType("Task")
                .description(task.getDescription())
                .assignedTo(task.getAssignedTo())
                .areaPath(task.getAreaPath())
                .createdDate(task.getCreatedDate())
                .changedDate(task.getChangedDate())
                .build();
    }
}