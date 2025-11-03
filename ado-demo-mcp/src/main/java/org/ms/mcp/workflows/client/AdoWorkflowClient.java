package org.ms.mcp.workflows.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.config.AdoConfiguration;
import org.ms.mcp.workflows.dto.*;
import org.ms.mcp.workflows.exception.AdoWorkflowException;
import org.ms.mcp.workflows.util.AdoResponseParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class AdoWorkflowClient {
    
    private final AdoConfiguration config;
    private final WebClient webClient;
    private final AdoResponseParser parser;
    
    public AdoWorkflowClient(AdoConfiguration config, AdoResponseParser parser) {
        this.config = config;
        this.parser = parser;
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
    // MÉTODOS QUE RETORNAN DTOs DIRECTAMENTE
    // =====================================================
    
    /**
     * Obtener épica por ID - Retorna DTO directamente
     */
    public Mono<EpicInfo> getEpicById(Long epicId) {
        log.debug("Getting epic by ID: {}", epicId);
        
        String url = config.buildWorkItemWithRelationsUrl(epicId);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parser.parseEpics(createSingleItemResponse(response)))
                .map(epics -> epics.isEmpty() ? null : epics.get(0))
                .onErrorMap(this::mapException)
                .doOnSuccess(epic -> log.debug("Retrieved epic: {}", epic != null ? epic.getId() : "null"))
                .doOnError(error -> log.error("Error getting epic: {}", epicId, error));
    }
    
    /**
     * Buscar épicas por nombre parcial - Retorna DTOs directamente
     */
    public Mono<List<EpicInfo>> findEpicsByName(String partialName) {
        log.debug("Finding epics by partial name: {}", partialName);
        
        String wiqlQuery = String.format("""
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Epic'
            AND [System.TeamProject] = '%s'
            AND [System.Title] CONTAINS '%s'
            ORDER BY [System.CreatedDate] DESC
            """, config.getProject(), partialName);
        
        return executeWiqlAndGetWorkItems(wiqlQuery)
                .map(parser::parseEpics)
                .doOnSuccess(epics -> log.debug("Found {} epics", epics.size()))
                .doOnError(error -> log.error("Error finding epics by name: {}", partialName, error));
    }

    public Mono<List<UserStoryInfo>> getUserStoriesByEpicName(String epicName) {
        log.debug("Getting user stories for epic name: {}", epicName);
        Mono<List<Long>> epicIds = findEpicsByName(epicName)
                .flatMapMany(Flux::fromIterable)
                .map(EpicInfo::getId)
                .collectList();
        return epicIds
                .flatMap(this::getUserStories)
                .doOnSuccess(stories -> log.debug("Retrieved {} user stories for epic name: {}", stories.size(), epicName))
                .doOnError(error -> log.error("Error getting user stories for epic name: {}", epicName, error));
    }
    /**
     * Obtener historias de usuario por épica(s) - Retorna DTOs directamente
     */
    public Mono<List<UserStoryInfo>> getUserStories(List<Long> epicIds) {
        log.debug("Getting user stories for epic IDs: {}", epicIds);
        
        if (epicIds == null || epicIds.isEmpty()) {
            return Mono.just(List.of());
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
        
        return executeWiqlAndGetWorkItems(wiqlQuery)
                .map(parser::parseUserStories)
                .doOnSuccess(stories -> log.debug("Retrieved {} user stories", stories.size()))
                .doOnError(error -> log.error("Error getting user stories for epics: {}", epicIds, error));
    }
    
    /**
     * Obtener tareas por épica(s) - Retorna DTOs directamente
     */
    public Mono<List<TaskInfo>> getTasks(List<Long> epicIds) {
        log.debug("Getting tasks for epic IDs: {}", epicIds);
        
        if (epicIds == null || epicIds.isEmpty()) {
            return Mono.just(List.of());
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
        
        return executeWiqlAndGetWorkItems(wiqlQuery)
                .map(parser::parseTasks)
                .doOnSuccess(tasks -> log.debug("Retrieved {} tasks", tasks.size()))
                .doOnError(error -> log.error("Error getting tasks for epics: {}", epicIds, error));
    }
    
    /**
     * Obtener épica completa con sus historias y tareas
     * UN SOLO MÉTODO que hace todo el flujo
     */
    public Mono<EpicWorkflowResult.EpicData> getCompleteEpicWorkflow(Long epicId) {
        log.debug("Getting complete workflow for epic: {}", epicId);
        
        return getEpicById(epicId)
                .flatMap(epic -> {
                    if (epic == null) {
                        return Mono.just(EpicWorkflowResult.EpicData.builder()
                                .epic(null)
                                .userStories(List.of())
                                .tasks(List.of())
                                .build());
                    }
                    
                    Mono<List<UserStoryInfo>> userStoriesMono = getUserStories(List.of(epicId));
                    Mono<List<TaskInfo>> tasksMono = getTasks(List.of(epicId));
                    
                    return Mono.zip(userStoriesMono, tasksMono)
                            .map(tuple -> EpicWorkflowResult.EpicData.builder()
                                    .epic(epic)
                                    .userStories(tuple.getT1())
                                    .tasks(tuple.getT2())
                                    .build());
                })
                .doOnSuccess(result -> log.debug("Retrieved complete workflow for epic: {}", epicId))
                .doOnError(error -> log.error("Error getting complete workflow for epic: {}", epicId, error));
    }
    
    // =====================================================
    // MÉTODOS UTILITARIOS PRIVADOS
    // =====================================================
    
    /**
     * Ejecuta WIQL y obtiene los work items completos automáticamente
     */
    private Mono<JsonNode> executeWiqlAndGetWorkItems(String wiqlQuery) {
        return executeWiqlQuery(wiqlQuery)
                .flatMap(wiqlResponse -> {
                    List<Long> workItemIds = parser.extractWorkItemIds(wiqlResponse);
                    if (workItemIds.isEmpty()) {
                        return Mono.just(createEmptyWorkItemsResponse());
                    }
                    return getWorkItemsDetails(workItemIds);
                });
    }
    
    /**
     * Ejecutar consulta WIQL básica
     */
    private Mono<JsonNode> executeWiqlQuery(String wiqlQuery) {
        String requestBody = String.format("{\"query\": \"%s\"}", 
                wiqlQuery.replace("\"", "\\\"").replace("\n", "\\n"));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(this::mapException);
    }
    
    /**
     * Obtener detalles de work items por sus IDs
     */
    private Mono<JsonNode> getWorkItemsDetails(List<Long> workItemIds) {
        if (workItemIds.isEmpty()) {
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
                .onErrorMap(this::mapException);
    }
    
    private JsonNode createSingleItemResponse(JsonNode singleItem) {
        com.fasterxml.jackson.databind.node.ObjectNode response = 
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        com.fasterxml.jackson.databind.node.ArrayNode valueArray = 
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        valueArray.add(singleItem);
        response.set("value", valueArray);
        return response;
    }
    
    private JsonNode createEmptyWorkItemsResponse() {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                .put("queryType", "flat")
                .set("workItems", com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
    }
    
    private Throwable mapException(Throwable error) {
        if (error instanceof WebClientResponseException webEx) {
            String message = switch (webEx.getStatusCode().value()) {
                case 401 -> "PAT token inválido o expirado";
                case 403 -> "Sin permisos suficientes para Work Items";
                case 404 -> String.format("Proyecto '%s' no encontrado", config.getProject());
                case 400 -> "Consulta WIQL inválida: " + webEx.getResponseBodyAsString();
                default -> String.format("Error API Azure DevOps (HTTP %d)", webEx.getStatusCode().value());
            };
            return new AdoWorkflowException(message, webEx);
        }
        return error;
    }
}