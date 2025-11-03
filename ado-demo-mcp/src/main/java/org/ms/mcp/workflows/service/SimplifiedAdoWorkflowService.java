package org.ms.mcp.workflows.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.workflows.client.AdoWorkflowClient;
import org.ms.mcp.workflows.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio ULTRA-SIMPLIFICADO para workflows de Azure DevOps
 * 
 * Solo DOS métodos principales que retornan EpicWorkflowResult:
 * - getWorkflowById(Long epicId)
 * - getWorkflowByPartialName(String partialName)
 * 
 * El WebClient ya maneja toda la complejidad de parsing y transformación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimplifiedAdoWorkflowService {
    
    private final AdoWorkflowClient client;
    
    /**
     * FLUJO 1: Obtener workflow completo por Epic ID
     * Una sola llamada al client hace todo el trabajo
     */
    public Mono<EpicWorkflowResult> getWorkflowById(Long epicId) {
        log.info("Getting workflow for Epic ID: {}", epicId);
        long startTime = System.currentTimeMillis();
        
        return client.getCompleteEpicWorkflow(epicId)
                .map(epicData -> EpicWorkflowResult.builder()
                        .searchType("BY_ID")
                        .searchValue(epicId.toString())
                        .epics(epicData.getEpic() != null ? List.of(epicData) : List.of())
                        .metadata(createMetadata(startTime, 3))
                        .build())
                .doOnSuccess(result -> log.info("Workflow completed for Epic ID: {} with {} epics in {}ms", 
                        epicId, result.getTotalEpics(), result.getMetadata().getExecutionTimeMs()))
                .doOnError(error -> log.error("Error in workflow for Epic ID: {}", epicId, error));
    }
    
    /**
     * FLUJO 2: Obtener workflow completo por nombre parcial
     * Busca épicas y luego obtiene sus workflows completos
     */
    public Mono<EpicWorkflowResult> getWorkflowByPartialName(String partialName) {
        log.info("Getting workflow for Epic partial name: {}", partialName);
        long startTime = System.currentTimeMillis();
        
        return client.findEpicsByName(partialName)
                .flatMap(epics -> {
                    if (epics.isEmpty()) {
                        return Mono.just(createEmptyResult("BY_PARTIAL_NAME", partialName, startTime));
                    }
                    
                    // Para cada épica encontrada, obtener su workflow completo
                    List<Mono<EpicWorkflowResult.EpicData>> workflows = epics.stream()
                            .map(epic -> client.getCompleteEpicWorkflow(epic.getId()))
                            .toList();
                    
                    return Mono.zip(workflows, epicDataArray -> {
                        List<EpicWorkflowResult.EpicData> epicDataList = new ArrayList<>();
                        for (Object epicData : epicDataArray) {
                            epicDataList.add((EpicWorkflowResult.EpicData) epicData);
                        }
                        
                        return EpicWorkflowResult.builder()
                                .searchType("BY_PARTIAL_NAME")
                                .searchValue(partialName)
                                .epics(epicDataList)
                                .metadata(createMetadata(startTime, 1 + epics.size() * 3)) // find + (epic+stories+tasks per epic)
                                .build();
                    });
                })
                .doOnSuccess(result -> log.info("Workflow completed for partial name: '{}', found {} epics in {}ms", 
                        partialName, result.getTotalEpics(), result.getMetadata().getExecutionTimeMs()))
                .doOnError(error -> log.error("Error in workflow for partial name: {}", partialName, error));
    }
    
    /**
     * MÉTODO ADICIONAL: Obtener solo las épicas (sin historias/tareas)
     * Para casos donde solo necesitas información básica de épicas
     */
    public Mono<List<EpicInfo>> getEpicsOnly(String partialName) {
        log.info("Getting epics only for partial name: {}", partialName);
        
        return client.findEpicsByName(partialName)
                .doOnSuccess(epics -> log.info("Found {} epics for partial name: {}", epics.size(), partialName))
                .doOnError(error -> log.error("Error getting epics for partial name: {}", partialName, error));
    }
    
    /**
     * MÉTODO ADICIONAL: Obtener solo historias de usuario para épicas específicas
     */
    public Mono<List<UserStoryInfo>> getUserStoriesOnly(List<Long> epicIds) {
        log.info("Getting user stories only for epic IDs: {}", epicIds);
        
        return client.getUserStories(epicIds)
                .doOnSuccess(stories -> log.info("Found {} user stories for {} epics", stories.size(), epicIds.size()))
                .doOnError(error -> log.error("Error getting user stories for epics: {}", epicIds, error));
    }
    
    /**
     * MÉTODO ADICIONAL: Obtener solo tareas para épicas específicas
     */
    public Mono<List<TaskInfo>> getTasksOnly(List<Long> epicIds) {
        log.info("Getting tasks only for epic IDs: {}", epicIds);
        
        return client.getTasks(epicIds)
                .doOnSuccess(tasks -> log.info("Found {} tasks for {} epics", tasks.size(), epicIds.size()))
                .doOnError(error -> log.error("Error getting tasks for epics: {}", epicIds, error));
    }
    
    // =====================================================
    // MÉTODOS UTILITARIOS PRIVADOS
    // =====================================================
    
    private EpicWorkflowResult createEmptyResult(String searchType, String searchValue, long startTime) {
        return EpicWorkflowResult.builder()
                .searchType(searchType)
                .searchValue(searchValue)
                .epics(List.of())
                .metadata(createMetadata(startTime, 1))
                .build();
    }
    
    private EpicWorkflowResult.WorkflowMetadata createMetadata(long startTime, int apiCalls) {
        return EpicWorkflowResult.WorkflowMetadata.builder()
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .apiCallsCount(apiCalls)
                .hasErrors(false)
                .build();
    }
}