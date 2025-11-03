package org.ms.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.workflows.client.AdoWorkflowClient;
import org.ms.mcp.workflows.dto.EpicInfo;
import org.ms.mcp.workflows.dto.UserStoryInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserStoryTools {
    
    private final AdoWorkflowClient simplifiedClient;
    private final ObjectMapper objectMapper;

    @Tool(name = "findEpics", description = ToolPrompts.FIND_EPICS_TOOL)
    public JsonNode findEpics(String epicNameOrId) {
        Mono<List<EpicInfo>> epicInfoMono;
        if(StringUtils.hasLength(epicNameOrId)) {
            if(epicNameOrId.matches("\\d+")) {
                Long epicId = Long.parseLong(epicNameOrId);
                epicInfoMono = simplifiedClient
                        .getEpicById(epicId)
                        .flatMapMany(Mono::just)
                        .collectList();
            } else {
                epicInfoMono = simplifiedClient.findEpicsByName(epicNameOrId);
            }
        } else {
            epicInfoMono = simplifiedClient.findEpicsByName("");
        }
        Mono<JsonNode> jsonNodeMono = epicInfoMono.map(objectMapper::valueToTree);
        return jsonNodeMono
                .onErrorResume(throwable -> {
                    log.error("Error in findEpics MCP tool for epic: {}", epicNameOrId, throwable);
                    return Mono.just(objectMapper.createObjectNode()
                            .put("error", "Error finding user stories for all Epics" + throwable.getMessage())
                            .put("success", false));
                })
                .block();
    }

    @Tool(name = "listUserStories", description = ToolPrompts.USER_STORY_TOOL)
    public JsonNode listUserStories() {
        Mono<List<UserStoryInfo>>  userStoryInfo = simplifiedClient.getUserStoriesByEpicName("");
        Mono<JsonNode> jsonNodeMono = userStoryInfo.map(objectMapper::valueToTree);
        return jsonNodeMono
                .onErrorResume(throwable -> {
                    log.error("Error in listUserStories MCP tool", throwable);
                    return Mono.just(objectMapper.createObjectNode()
                            .put("error", "Error finding user stories for all Epics" + throwable.getMessage())
                            .put("success", false));
                })
                .block();
    }

    @Tool(name = "findUserStoriesByEpic", description = ToolPrompts.USER_STORY_BY_EPIC_PROMPT)
    public JsonNode findUserStoriesByEpic(String epicNameOrId) {
        Mono<List<UserStoryInfo>> userStoryInfo;
        try {
            Long epicId = Long.parseLong(epicNameOrId);
            userStoryInfo =  simplifiedClient.getUserStories(List.of(epicId));
        } catch (NumberFormatException e) {
            userStoryInfo = simplifiedClient.getUserStoriesByEpicName(epicNameOrId);
        }
        Mono<JsonNode> jsonNodeMono = userStoryInfo.map(objectMapper::valueToTree);
        return jsonNodeMono
                .onErrorResume(throwable -> {
                    log.error("Error in findUserStoriesByEpic MCP tool for epic: {}", epicNameOrId, throwable);
                    return Mono.just(objectMapper.createObjectNode()
                            .put("error", "Error finding user stories for epic " + epicNameOrId + ": " + throwable.getMessage())
                            .put("epic", epicNameOrId)
                            .put("success", false));
                })
                .block();
    }
}
