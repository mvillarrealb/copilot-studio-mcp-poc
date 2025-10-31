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
public class UserStoryWebClient {
    
    private final AdoConfiguration config;
    private final WebClient webClient;
    
    public UserStoryWebClient(AdoConfiguration config) {
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
     * Obtiene todas las historias de usuario (Issues) del proyecto
     * Basado en: test_user_stories() función del script test_api.sh
     */
    public Mono<JsonNode> getAllUserStories() {
        log.debug("Getting all user stories for project: {}", config.getProject());
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.CreatedDate]
            FROM WorkItems
            WHERE [System.WorkItemType] = 'Issue'
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
                .doOnSuccess(response -> log.debug("Successfully retrieved user stories"))
                .doOnError(error -> log.error("Error getting all user stories", error));
    }
    
    /**
     * Obtiene historias de usuario de una épica específica usando WorkItemLinks
     * Basado en: test_user_stories() función del script test_api.sh
     */
    public Mono<JsonNode> getUserStoriesByEpic(Long epicId) {
        log.debug("Getting user stories for epic ID: {}", epicId);
        
        String wiqlQuery = """
            SELECT [System.Id], [System.Title], [System.State], [System.Description]
            FROM WorkItemLinks
            WHERE [Source].[System.Id] = %d
            AND [System.Links.LinkType] = 'System.LinkTypes.Hierarchy-Forward'
            MODE (Recursive)
            """.formatted(epicId);
        
        String requestBody = """
            {"query": "%s"}
            """.formatted(wiqlQuery.replace("\"", "\\\""));
        
        return webClient.post()
                .uri(config.buildWiqlUrl())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> log.debug("Successfully retrieved user stories for epic: {}", epicId))
                .doOnError(error -> log.error("Error getting user stories for epic: {}", epicId, error));
    }

}