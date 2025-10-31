package org.ms.mcp.workflows.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.workflows.client.AdoWorkflowWebClient;
import org.ms.mcp.workflows.dto.*;
import org.ms.mcp.workflows.util.AdoResponseParser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio SIMPLIFICADO para workflows de Azure DevOps
 * 
 * DOS MÉTODOS, UNA SOLA RESPUESTA:
 * - getWorkflowById(Long epicId) -> EpicWorkflowResult
 * - getWorkflowByPartialName(String partialName) -> EpicWorkflowResult
 * 
 * Ambos devuelven EpicWorkflowResult con lista de épicas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdoWorkflowService {
    
    private final AdoWorkflowWebClient workflowClient;
    private final AdoResponseParser responseParser;
    
    /**
     * FLUJO 1: Búsqueda por Epic ID específica
     * Retorna EpicWorkflowResult con 1 épica en la lista
     */
    public Mono<EpicWorkflowResult> getWorkflowById(Long epicId) {
        log.info("Executing workflow for Epic ID: {}", epicId);
        long startTime = System.currentTimeMillis();
        
        return workflowClient.getEpicWithRelations(epicId)
                .flatMap(epicResponse -> {
                    // Parsear la épica
                    List<EpicInfo> epics = responseParser.parseEpics(createSingleItemResponse(epicResponse));
                    if (epics.isEmpty()) {
                        return Mono.just(createEmptyResult("BY_ID", epicId.toString(), startTime, 1));
                    }
                    
                    EpicInfo epic = epics.get(0);
                    
                    // Obtener historias y tareas en paralelo
                    Mono<List<UserStoryInfo>> userStoriesMono = workflowClient
                            .getUserStoriesByEpicId(epicId)
                            .flatMap(response -> workflowClient.getWorkItemsDetails(
                                    responseParser.extractWorkItemIds(response)))
                            .map(responseParser::parseUserStories)
                            .onErrorReturn(new ArrayList<>());
                    
                    Mono<List<TaskInfo>> tasksMono = workflowClient
                            .getTasksByEpicId(epicId)
                            .flatMap(response -> workflowClient.getWorkItemsDetails(
                                    responseParser.extractWorkItemIds(response)))
                            .map(responseParser::parseTasks)
                            .onErrorReturn(new ArrayList<>());
                    
                    return Mono.zip(userStoriesMono, tasksMono)
                            .map(tuple -> {
                                long executionTime = System.currentTimeMillis() - startTime;
                                
                                EpicWorkflowResult.EpicData epicData = EpicWorkflowResult.EpicData.builder()
                                        .epic(epic)
                                        .userStories(tuple.getT1())
                                        .tasks(tuple.getT2())
                                        .build();
                                
                                return EpicWorkflowResult.builder()
                                        .searchType("BY_ID")
                                        .searchValue(epicId.toString())
                                        .epics(List.of(epicData))
                                        .metadata(EpicWorkflowResult.WorkflowMetadata.builder()
                                                .executionTimeMs(executionTime)
                                                .apiCallsCount(3)
                                                .hasErrors(false)
                                                .build())
                                        .build();
                            });
                })
                .doOnSuccess(result -> log.info("Workflow completed for Epic ID: {} in {}ms", 
                        epicId, result.getMetadata().getExecutionTimeMs()))
                .doOnError(error -> log.error("Error executing workflow for Epic ID: {}", epicId, error));
    }
    
    /**
     * FLUJO 2: Búsqueda por nombre parcial de épica
     * Retorna EpicWorkflowResult con N épicas en la lista
     */
    public Mono<EpicWorkflowResult> getWorkflowByPartialName(String partialName) {
        log.info("Executing workflow for Epic partial name: {}", partialName);
        long startTime = System.currentTimeMillis();
        
        return workflowClient.findEpicsByPartialName(partialName)
                .flatMap(epicsResponse -> {
                    List<Long> epicIds = responseParser.extractWorkItemIds(epicsResponse);
                    if (epicIds.isEmpty()) {
                        return Mono.just(createEmptyResult("BY_PARTIAL_NAME", partialName, startTime, 1));
                    }
                    
                    // Obtener detalles de todas las épicas
                    return workflowClient.getWorkItemsDetails(epicIds)
                            .flatMap(epicsDetailsResponse -> {
                                List<EpicInfo> epics = responseParser.parseEpics(epicsDetailsResponse);
                                
                                // Para cada épica, obtener sus historias y tareas
                                List<Mono<EpicWorkflowResult.EpicData>> epicWorkflows = epics.stream()
                                        .map(epic -> getEpicWorkflowData(epic.getId(), epic))
                                        .toList();
                                
                                return Mono.zip(epicWorkflows, epicDataArray -> {
                                    long executionTime = System.currentTimeMillis() - startTime;
                                    
                                    List<EpicWorkflowResult.EpicData> epicDataList = new ArrayList<>();
                                    for (Object epicData : epicDataArray) {
                                        epicDataList.add((EpicWorkflowResult.EpicData) epicData);
                                    }
                                    
                                    return EpicWorkflowResult.builder()
                                            .searchType("BY_PARTIAL_NAME")
                                            .searchValue(partialName)
                                            .epics(epicDataList)
                                            .metadata(EpicWorkflowResult.WorkflowMetadata.builder()
                                                    .executionTimeMs(executionTime)
                                                    .apiCallsCount(2 + epicIds.size() * 2) // epic search + details + (stories + tasks per epic)
                                                    .hasErrors(false)
                                                    .build())
                                            .build();
                                });
                            });
                })
                .doOnSuccess(result -> log.info("Workflow completed for partial name: '{}', found {} epics in {}ms", 
                        partialName, result.getTotalEpics(), result.getMetadata().getExecutionTimeMs()))
                .doOnError(error -> log.error("Error executing workflow for partial name: {}", partialName, error));
    }
    
    // =====================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =====================================================
    
    private Mono<EpicWorkflowResult.EpicData> getEpicWorkflowData(Long epicId, EpicInfo epic) {
        Mono<List<UserStoryInfo>> userStoriesMono = workflowClient
                .getUserStoriesByEpicId(epicId)
                .flatMap(response -> workflowClient.getWorkItemsDetails(
                        responseParser.extractWorkItemIds(response)))
                .map(responseParser::parseUserStories)
                .onErrorReturn(new ArrayList<>());
        
        Mono<List<TaskInfo>> tasksMono = workflowClient
                .getTasksByEpicId(epicId)
                .flatMap(response -> workflowClient.getWorkItemsDetails(
                        responseParser.extractWorkItemIds(response)))
                .map(responseParser::parseTasks)
                .onErrorReturn(new ArrayList<>());
        
        return Mono.zip(userStoriesMono, tasksMono)
                .map(tuple -> EpicWorkflowResult.EpicData.builder()
                        .epic(epic)
                        .userStories(tuple.getT1())
                        .tasks(tuple.getT2())
                        .build());
    }
    
    private EpicWorkflowResult createEmptyResult(String searchType, String searchValue, long startTime, int apiCalls) {
        return EpicWorkflowResult.builder()
                .searchType(searchType)
                .searchValue(searchValue)
                .epics(new ArrayList<>())
                .metadata(EpicWorkflowResult.WorkflowMetadata.builder()
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .apiCallsCount(apiCalls)
                        .hasErrors(false)
                        .build())
                .build();
    }
    
    private com.fasterxml.jackson.databind.JsonNode createSingleItemResponse(com.fasterxml.jackson.databind.JsonNode singleItem) {
        com.fasterxml.jackson.databind.node.ObjectNode response = 
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        com.fasterxml.jackson.databind.node.ArrayNode valueArray = 
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        valueArray.add(singleItem);
        response.set("value", valueArray);
        return response;
    }
}