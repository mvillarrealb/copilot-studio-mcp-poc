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
public class EpicWebClient {
    
    private final AdoConfiguration config;
    private final WebClient webClient;
    
    public EpicWebClient(AdoConfiguration config) {
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
     * Obtiene todas las épicas del proyecto
     * Basado en: test_epics() función del script test_api.sh
     */
    public Mono<JsonNode> getAllEpics() {
        log.debug("Getting all epics for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Epic'
            AND [System.TeamProject] = '%s'
            """.formatted(config.getProject());
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved epics"))
                .doOnError(error -> log.error("Error getting all epics", error));
    }
    
    /**
     * Obtiene épicas con detalles completos ordenadas por fecha
     * Basado en: test_epics() función del script test_api.sh
     */
    public Mono<JsonNode> getEpicsWithDetails() {
        log.debug("Getting epics with complete details for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.Description],
            [System.AreaPath], [System.AssignedTo]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Epic'
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
                .doOnSuccess(response -> log.debug("Successfully retrieved epics with details"))
                .doOnError(error -> log.error("Error getting epics with details", error));
    }
    
    /**
     * Obtiene una épica específica por ID
     */
    public Mono<JsonNode> getEpicById(Long epicId) {
        log.debug("Getting epic by ID: {}", epicId);
        
        return webClient.get()
                .uri(config.buildWorkItemByIdUrl(epicId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved epic: {}", epicId))
                .doOnError(error -> log.error("Error getting epic by ID: {}", epicId, error));
    }
    
    /**
     * Obtiene una épica con sus relaciones (historias de usuario hijas)
     */
    public Mono<JsonNode> getEpicWithRelations(Long epicId) {
        log.debug("Getting epic with relations for ID: {}", epicId);
        
        return webClient.get()
                .uri(config.buildWorkItemWithRelationsUrl(epicId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved epic {} with relations", epicId))
                .doOnError(error -> log.error("Error getting epic with relations: {}", epicId, error));
    }
}