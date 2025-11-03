package org.ms.mcp.workflows.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.workflows.client.AdoParsingException;
import org.ms.mcp.workflows.dto.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilidad para parsear respuestas JSON de Azure DevOps a DTOs específicos
 * Maneja todas las transformaciones de datos para los workflows
 */
@Slf4j
@Component
public class AdoResponseParser {
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    };
    
    // =====================================================
    // MÉTODOS PRINCIPALES DE PARSING
    // =====================================================
    
    /**
     * Extrae IDs de work items de una respuesta WIQL
     */
    public List<Long> extractWorkItemIds(JsonNode response) {
        log.debug("Extracting work item IDs from response");
        
        try {
            List<Long> ids = new ArrayList<>();
            
            // Verificar si hay workItems en la respuesta
            if (response.has("workItems") && response.get("workItems").isArray()) {
                for (JsonNode workItem : response.get("workItems")) {
                    if (workItem.has("id")) {
                        ids.add(workItem.get("id").asLong());
                    }
                }
            }
            
            // También verificar si hay workItemRelations (para consultas con links)
            if (response.has("workItemRelations") && response.get("workItemRelations").isArray()) {
                for (JsonNode relation : response.get("workItemRelations")) {
                    if (relation.has("target") && relation.get("target").has("id")) {
                        ids.add(relation.get("target").get("id").asLong());
                    }
                }
            }
            
            log.debug("Extracted {} work item IDs", ids.size());
            return ids.stream().distinct().collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new AdoParsingException("Error extracting work item IDs", e);
        }
    }
    
    /**
     * Parsea épicas desde una respuesta de detalles de work items
     */
    public List<EpicInfo> parseEpics(JsonNode response) {
        log.debug("Parsing epics from response");
        
        try {
            List<EpicInfo> epics = new ArrayList<>();
            
            if (response.has("value") && response.get("value").isArray()) {
                for (JsonNode workItem : response.get("value")) {
                    if (isEpic(workItem)) {
                        epics.add(parseEpicFromWorkItem(workItem));
                    }
                }
            }
            
            log.debug("Parsed {} epics", epics.size());
            return epics;
            
        } catch (Exception e) {
            throw new AdoParsingException("Error parsing epics", e);
        }
    }
    
    /**
     * Parsea historias de usuario desde una respuesta de detalles de work items
     */
    public List<UserStoryInfo> parseUserStories(JsonNode response) {
        log.debug("Parsing user stories from response");
        
        try {
            List<UserStoryInfo> userStories = new ArrayList<>();
            
            if (response.has("value") && response.get("value").isArray()) {
                for (JsonNode workItem : response.get("value")) {
                    if (isUserStory(workItem)) {
                        userStories.add(parseUserStoryFromWorkItem(workItem));
                    }
                }
            }
            
            log.debug("Parsed {} user stories", userStories.size());
            return userStories;
            
        } catch (Exception e) {
            throw new AdoParsingException("Error parsing user stories", e);
        }
    }
    
    /**
     * Parsea tareas desde una respuesta de detalles de work items
     */
    public List<TaskInfo> parseTasks(JsonNode response) {
        log.debug("Parsing tasks from response");
        
        try {
            List<TaskInfo> tasks = new ArrayList<>();
            
            if (response.has("value") && response.get("value").isArray()) {
                for (JsonNode workItem : response.get("value")) {
                    if (isTask(workItem)) {
                        tasks.add(parseTaskFromWorkItem(workItem));
                    }
                }
            }
            
            log.debug("Parsed {} tasks", tasks.size());
            return tasks;
            
        } catch (Exception e) {
            throw new AdoParsingException("Error parsing tasks", e);
        }
    }
    
    // =====================================================
    // MÉTODOS DE PARSING ESPECÍFICOS POR TIPO
    // =====================================================
    
    /**
     * Parsea una épica individual desde un JsonNode de work item
     */
    private EpicInfo parseEpicFromWorkItem(JsonNode workItem) {
        JsonNode fields = workItem.get("fields");
        
        return EpicInfo.builder()
                .id(workItem.get("id").asLong())
                .title(getFieldValue(fields, "System.Title"))
                .state(getFieldValue(fields, "System.State"))
                .description(getFieldValue(fields, "System.Description"))
                .assignedTo(getAssignedToDisplayName(fields))
                .areaPath(getFieldValue(fields, "System.AreaPath"))
                .createdDate(parseDateTime(getFieldValue(fields, "System.CreatedDate")))
                .changedDate(parseDateTime(getFieldValue(fields, "System.ChangedDate")))
                .priority(getFieldValue(fields, "Microsoft.VSTS.Common.Priority"))
                .businessValue(getFieldValue(fields, "Microsoft.VSTS.Common.BusinessValue"))
                .acceptanceCriteria(getFieldValue(fields, "Microsoft.VSTS.Common.AcceptanceCriteria"))
                .startDate(parseDateTime(getFieldValue(fields, "Microsoft.VSTS.Scheduling.StartDate")))
                .targetDate(parseDateTime(getFieldValue(fields, "Microsoft.VSTS.Scheduling.TargetDate")))
                .build();
    }
    
    /**
     * Parsea una historia de usuario individual desde un JsonNode de work item
     */
    private UserStoryInfo parseUserStoryFromWorkItem(JsonNode workItem) {
        JsonNode fields = workItem.get("fields");
        
        return UserStoryInfo.builder()
                .id(workItem.get("id").asLong())
                .title(getFieldValue(fields, "System.Title"))
                .state(getFieldValue(fields, "System.State"))
                .description(getFieldValue(fields, "System.Description"))
                .assignedTo(getAssignedToDisplayName(fields))
                .areaPath(getFieldValue(fields, "System.AreaPath"))
                .createdDate(parseDateTime(getFieldValue(fields, "System.CreatedDate")))
                .changedDate(parseDateTime(getFieldValue(fields, "System.ChangedDate")))
                .acceptanceCriteria(getFieldValue(fields, "Microsoft.VSTS.Common.AcceptanceCriteria"))
                .storyPoints(getIntegerFieldValue(fields, "Microsoft.VSTS.Scheduling.StoryPoints"))
                .priority(getFieldValue(fields, "Microsoft.VSTS.Common.Priority"))
                .riskLevel(getFieldValue(fields, "Microsoft.VSTS.Common.Risk"))
                .build();
    }
    
    /**
     * Parsea una tarea individual desde un JsonNode de work item
     */
    private TaskInfo parseTaskFromWorkItem(JsonNode workItem) {
        JsonNode fields = workItem.get("fields");
        
        return TaskInfo.builder()
                .id(workItem.get("id").asLong())
                .title(getFieldValue(fields, "System.Title"))
                .state(getFieldValue(fields, "System.State"))
                .description(getFieldValue(fields, "System.Description"))
                .assignedTo(getAssignedToDisplayName(fields))
                .areaPath(getFieldValue(fields, "System.AreaPath"))
                .createdDate(parseDateTime(getFieldValue(fields, "System.CreatedDate")))
                .changedDate(parseDateTime(getFieldValue(fields, "System.ChangedDate")))
                .remainingWork(getDoubleFieldValue(fields, "Microsoft.VSTS.Scheduling.RemainingWork"))
                .originalEstimate(getDoubleFieldValue(fields, "Microsoft.VSTS.Scheduling.OriginalEstimate"))
                .completedWork(getDoubleFieldValue(fields, "Microsoft.VSTS.Scheduling.CompletedWork"))
                .activity(getFieldValue(fields, "Microsoft.VSTS.Common.Activity"))
                .priority(getFieldValue(fields, "Microsoft.VSTS.Common.Priority"))
                .build();
    }
    
    // =====================================================
    // MÉTODOS UTILITARIOS
    // =====================================================
    
    /**
     * Determina si un work item es una épica
     */
    private boolean isEpic(JsonNode workItem) {
        return "Epic".equals(getWorkItemType(workItem));
    }
    
    /**
     * Determina si un work item es una historia de usuario
     */
    private boolean isUserStory(JsonNode workItem) {
        String type = getWorkItemType(workItem);
        return "Issue".equals(type) || "User Story".equals(type);
    }
    
    /**
     * Determina si un work item es una tarea
     */
    private boolean isTask(JsonNode workItem) {
        return "Task".equals(getWorkItemType(workItem));
    }
    
    /**
     * Obtiene el tipo de work item
     */
    private String getWorkItemType(JsonNode workItem) {
        return getFieldValue(workItem.get("fields"), "System.WorkItemType");
    }
    
    /**
     * Obtiene el valor de un campo como string
     */
    private String getFieldValue(JsonNode fields, String fieldName) {
        if (fields != null && fields.has(fieldName) && !fields.get(fieldName).isNull()) {
            return fields.get(fieldName).asText();
        }
        return null;
    }
    
    /**
     * Obtiene el valor de un campo como integer
     */
    private Integer getIntegerFieldValue(JsonNode fields, String fieldName) {
        if (fields != null && fields.has(fieldName) && !fields.get(fieldName).isNull()) {
            return fields.get(fieldName).asInt();
        }
        return null;
    }
    
    /**
     * Obtiene el valor de un campo como double
     */
    private Double getDoubleFieldValue(JsonNode fields, String fieldName) {
        if (fields != null && fields.has(fieldName) && !fields.get(fieldName).isNull()) {
            return fields.get(fieldName).asDouble();
        }
        return null;
    }
    
    /**
     * Obtiene el nombre del usuario asignado
     */
    private String getAssignedToDisplayName(JsonNode fields) {
        if (fields != null && fields.has("System.AssignedTo") && !fields.get("System.AssignedTo").isNull()) {
            JsonNode assignedTo = fields.get("System.AssignedTo");
            if (assignedTo.has("displayName")) {
                return assignedTo.get("displayName").asText();
            }
        }
        return null;
    }
    
    /**
     * Parsea una fecha/hora desde string usando múltiples formatos
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
                // Intentar con el siguiente formato
            }
        }
        
        log.warn("Could not parse date time: {}", dateTimeStr);
        return null;
    }
    
    /**
     * Construye un resultado de workflow épica combinando los datos parseados
     * MÉTODO SIMPLIFICADO - ya no se usa, mantenido para compatibilidad
     */
    public EpicWorkflowResult buildEpicWorkflowResult(EpicInfo epic, List<UserStoryInfo> userStories, List<TaskInfo> tasks, long executionTimeMs, int apiCallsCount) {
        EpicWorkflowResult.EpicData epicData = EpicWorkflowResult.EpicData.builder()
                .epic(epic)
                .userStories(userStories != null ? userStories : new ArrayList<>())
                .tasks(tasks != null ? tasks : new ArrayList<>())
                .build();
                
        return EpicWorkflowResult.builder()
                .searchType("BY_ID")
                .searchValue(epic != null ? epic.getId().toString() : "unknown")
                .epics(List.of(epicData))
                .metadata(EpicWorkflowResult.WorkflowMetadata.builder()
                        .executionTimeMs(executionTimeMs)
                        .apiCallsCount(apiCallsCount)
                        .hasErrors(false)
                        .build())
                .build();
    }
}