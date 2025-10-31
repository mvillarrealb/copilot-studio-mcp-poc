package org.ms.mcp.webclient;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.config.AdoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class TaskWebClient {
    
    private final AdoConfiguration config;
    private final WebClient webClient;
    
    public TaskWebClient(AdoConfiguration config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    private String getBasicAuthHeader() {
        String auth = ":" + config.getPatToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Obtiene todas las tareas del proyecto
     * Basado en: test_tasks() función del script test_api.sh
     */
    public Mono<JsonNode> getAllTasks() {
        log.debug("Getting all tasks for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Task'
            AND [System.TeamProject] = '%s'
            ORDER BY [System.CreatedDate] DESC
            """.formatted(config.getProject());
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks"))
                .doOnError(error -> log.error("Error getting all tasks", error));
    }
    
    /**
     * Obtiene tareas de una historia de usuario específica usando WorkItemLinks
     * Basado en: test_tasks() función del script test_api.sh
     */
    public Mono<JsonNode> getTasksByUserStory(Long userStoryId) {
        log.debug("Getting tasks for user story ID: {}", userStoryId);
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.Description]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] = %d
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            MODE (Recursive)
            """.formatted(userStoryId);
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks for user story: {}", userStoryId))
                .doOnError(error -> log.error("Error getting tasks for user story: {}", userStoryId, error));
    }
    
    /**
     * Obtiene una tarea específica por ID
     */
    public Mono<JsonNode> getTaskById(Long taskId) {
        log.debug("Getting task by ID: {}", taskId);
        
        return webClient.get()
                .uri(config.buildWorkItemByIdUrl(taskId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved task: {}", taskId))
                .doOnError(error -> log.error("Error getting task by ID: {}", taskId, error));
    }
    
    /**
     * Obtiene una tarea con sus relaciones
     */
    public Mono<JsonNode> getTaskWithRelations(Long taskId) {
        log.debug("Getting task with relations for ID: {}", taskId);
        
        return webClient.get()
                .uri(config.buildWorkItemWithRelationsUrl(taskId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved task {} with relations", taskId))
                .doOnError(error -> log.error("Error getting task with relations: {}", taskId, error));
    }
    
    /**
     * Obtiene tareas por estado específico
     */
    public Mono<JsonNode> getTasksByState(String state) {
        log.debug("Getting tasks by state: {}", state);
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Task'
            AND [System.TeamProject] = '%s'
            AND [System.State] = '%s'
            ORDER BY [System.CreatedDate] DESC
            """.formatted(config.getProject(), state);
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks with state: {}", state))
                .doOnError(error -> log.error("Error getting tasks by state: {}", state, error));
    }
    
    /**
     * Obtiene jerarquía completa de tareas (consulta avanzada)
     * Basado en: test_advanced_queries() función del script test_api.sh
     */
    public Mono<JsonNode> getTasksHierarchy() {
        log.debug("Getting tasks hierarchy for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State]
            FROM WorkItemLinks
            WHERE [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Task'
            MODE (Recursive)
            """;
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks hierarchy"))
                .doOnError(error -> log.error("Error getting tasks hierarchy", error));
    }
}