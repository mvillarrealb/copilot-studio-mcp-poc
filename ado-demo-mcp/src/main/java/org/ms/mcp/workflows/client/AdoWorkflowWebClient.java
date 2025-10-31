package org.ms.mcp.workflows.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.config.AdoConfiguration;
import org.ms.mcp.workflows.exception.AdoWorkflowException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebClient especializado para workflows de Azure DevOps
 * Implementa los flujos documentados en WORKFLOW_DOCUMENTATION.md
 * 
 * Este cliente está completamente aislado de los WebClients existentes
 * y se enfoca únicamente en los workflows de búsqueda jerárquica.
 */
@Slf4j
@Component
public class AdoWorkflowWebClient {
    
    private final AdoConfiguration config;
    private final WebClient webClient;
    
    public AdoWorkflowWebClient(AdoConfiguration config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    private String getBasicAuthHeader() {
        String auth = ":" + config.getPatToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
    
    // =====================================================
    // FLUJO 1: MÉTODOS PARA EPIC ID ESPECÍFICA
    // =====================================================
    
    /**
     * FLUJO 1.1: Buscar historias asociadas a una épica específica
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 1.1
     */
    public Mono<JsonNode> getUserStoriesByEpicId(Long epicId) {
        log.debug("Getting user stories for epic ID: {}", epicId);
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.Description]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] = %d
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Issue'
            MODE (Recursive)
            """, epicId);
        
        return executeWiqlQuery(wiqlQuery, "getUserStoriesByEpicId")
                .doOnSuccess(response -> log.debug("Successfully retrieved user stories for epic: {}", epicId))
                .doOnError(error -> log.error("Error getting user stories for epic: {}", epicId, error));
    }
    
    /**
     * FLUJO 1.2: Buscar tareas asociadas a una épica específica
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 1.2
     */
    public Mono<JsonNode> getTasksByEpicId(Long epicId) {
        log.debug("Getting tasks for epic ID: {}", epicId);
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] = %d
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Task'
            MODE (Recursive)
            """, epicId);
        
        return executeWiqlQuery(wiqlQuery, "getTasksByEpicId")
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks for epic: {}", epicId))
                .doOnError(error -> log.error("Error getting tasks for epic: {}", epicId, error));
    }
    
    /**
     * Obtener detalles completos de una épica con sus relaciones
     * Usado como complemento en el Flujo 1
     */
    public Mono<JsonNode> getEpicWithRelations(Long epicId) {
        log.debug("Getting epic with relations for ID: {}", epicId);
        
        String url = config.buildWorkItemWithRelationsUrl(epicId);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(this::mapWebClientException)
                .doOnSuccess(response -> log.debug("Successfully retrieved epic {} with relations", epicId))
                .doOnError(error -> log.error("Error getting epic with relations: {}", epicId, error));
    }
    
    // =====================================================
    // FLUJO 2: MÉTODOS PARA EPIC NAME PARCIAL
    // =====================================================
    
    /**
     * FLUJO 2.1: Buscar épicas que coincidan con nombre parcial
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 2.1
     */
    public Mono<JsonNode> findEpicsByPartialName(String partialName) {
        log.debug("Finding epics with partial name: {}", partialName);
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Epic'
            AND [System.TeamProject] = '%s'
            AND [System.Title] CONTAINS '%s'
            ORDER BY [System.CreatedDate] DESC
            """, config.getProject(), partialName);
        
        return executeWiqlQuery(wiqlQuery, "findEpicsByPartialName")
                .doOnSuccess(response -> log.debug("Successfully found epics with partial name: {}", partialName))
                .doOnError(error -> log.error("Error finding epics with partial name: {}", partialName, error));
    }
    
    /**
     * FLUJO 2.2: Buscar historias asociadas a múltiples épicas
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 2.2
     */
    public Mono<JsonNode> getUserStoriesByMultipleEpics(List<Long> epicIds) {
        log.debug("Getting user stories for multiple epics: {}", epicIds);
        
        if (epicIds == null || epicIds.isEmpty()) {
            return Mono.just(createEmptyWorkItemsResponse());
        }
        
        String epicIdsStr = epicIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.Description]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] IN (%s)
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Issue'
            MODE (Recursive)
            """, epicIdsStr);
        
        return executeWiqlQuery(wiqlQuery, "getUserStoriesByMultipleEpics")
                .doOnSuccess(response -> log.debug("Successfully retrieved user stories for {} epics", epicIds.size()))
                .doOnError(error -> log.error("Error getting user stories for multiple epics: {}", epicIds, error));
    }
    
    /**
     * FLUJO 2.3: Buscar tareas asociadas a múltiples épicas
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 2.3
     */
    public Mono<JsonNode> getTasksByMultipleEpics(List<Long> epicIds) {
        log.debug("Getting tasks for multiple epics: {}", epicIds);
        
        if (epicIds == null || epicIds.isEmpty()) {
            return Mono.just(createEmptyWorkItemsResponse());
        }
        
        String epicIdsStr = epicIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] IN (%s)
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Task'
            MODE (Recursive)
            """, epicIdsStr);
        
        return executeWiqlQuery(wiqlQuery, "getTasksByMultipleEpics")
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks for {} epics", epicIds.size()))
                .doOnError(error -> log.error("Error getting tasks for multiple epics: {}", epicIds, error));
    }
    
    // =====================================================
    // MÉTODOS COMPLEMENTARIOS
    // =====================================================
    
    /**
     * Obtener detalles completos de múltiples work items por sus IDs
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 3.1
     */
    public Mono<JsonNode> getWorkItemsDetails(List<Long> workItemIds) {
        log.debug("Getting work items details for IDs: {}", workItemIds);
        
        if (workItemIds == null || workItemIds.isEmpty()) {
            return Mono.just(createEmptyWorkItemsResponse());
        }
        
        String idsStr = workItemIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        String url = String.format("%s?ids=%s&api-version=%s", 
                config.buildWorkItemsUrl(), 
                idsStr, 
                config.getApi().getVersions().getWorkItems());
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(this::mapWebClientException)
                .doOnSuccess(response -> log.debug("Successfully retrieved {} work items details", workItemIds.size()))
                .doOnError(error -> log.error("Error getting work items details: {}", workItemIds, error));
    }
    
    /**
     * Obtener jerarquía completa del proyecto
     * Basado en WORKFLOW_DOCUMENTATION.md - Sección 3.2
     */
    public Mono<JsonNode> getCompleteHierarchy() {
        log.debug("Getting complete hierarchy for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.WorkItemType], [System.Title], [System.State]
            FROM WorkItemLinks
            WHERE [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            MODE (Recursive)
            """;
        
        return executeWiqlQuery(wiqlQuery, "getCompleteHierarchy")
                .doOnSuccess(response -> log.debug("Successfully retrieved complete hierarchy"))
                .doOnError(error -> log.error("Error getting complete hierarchy", error));
    }
    
    /**
     * Buscar tareas asociadas a una historia de usuario específica
     * Método auxiliar para el enfoque de dos pasos del Flujo 1
     */
    public Mono<JsonNode> getTasksByUserStoryId(Long userStoryId) {
        log.debug("Getting tasks for user story ID: {}", userStoryId);
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] = %d
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            AND [Target].[System.WorkItemType] = 'Task'
            """, userStoryId);
        
        return executeWiqlQuery(wiqlQuery, "getTasksByUserStoryId")
                .doOnSuccess(response -> log.debug("Successfully retrieved tasks for user story: {}", userStoryId))
                .doOnError(error -> log.error("Error getting tasks for user story: {}", userStoryId, error));
    }
    
    // =====================================================
    // MÉTODOS UTILITARIOS PRIVADOS
    // =====================================================
    
    /**
     * Ejecutar una consulta WIQL
     */
    private Mono<JsonNode> executeWiqlQuery(String wiqlQuery, String operationName) {
        log.debug("Executing WIQL query for operation: {}", operationName);
        log.trace("WIQL Query: {}", wiqlQuery);
        
        String requestBody = String.format("{\"query\": \"%s\"}", 
                wiqlQuery.replace("\"", "\\\"").replace("\n", "\\n"));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(this::mapWebClientException);
    }
    
    /**
     * Crear una respuesta vacía de work items
     */
    private JsonNode createEmptyWorkItemsResponse() {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                .put("queryType", "flat")
                .set("workItems", com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
    }
    
    /**
     * Mapear excepciones de WebClient a excepciones específicas del dominio
     */
    private Throwable mapWebClientException(Throwable error) {
        if (error instanceof WebClientResponseException webEx) {
            String message = switch (webEx.getStatusCode().value()) {
                case 401 -> "PAT token inválido o expirado. Verifica tu Personal Access Token.";
                case 403 -> "Sin permisos suficientes. Verifica que el PAT tenga permisos de lectura para Work Items.";
                case 404 -> String.format("Proyecto '%s' no encontrado o sin acceso.", config.getProject());
                case 400 -> "Consulta WIQL inválida: " + webEx.getResponseBodyAsString();
                default -> String.format("Error API Azure DevOps (HTTP %d): %s", 
                        webEx.getStatusCode().value(), webEx.getStatusText());
            };
            return new AdoWorkflowException(message, webEx);
        }
        return error;
    }
}