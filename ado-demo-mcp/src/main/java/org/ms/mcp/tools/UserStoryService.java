package org.ms.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.ms.mcp.webclient.EpicWebClient;
import org.ms.mcp.webclient.UserStoryWebClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class UserStoryService {
    private final UserStoryWebClient userStoryWebClient;
    private final EpicWebClient epicWebClient;

    @Tool(name = "findEpics", description = ToolPrompts.USER_STORY_TOOL)
    public JsonNode findEpics() {
        return epicWebClient.getAllEpics().block();
    }

    @Tool(name = "listUserStories", description = ToolPrompts.USER_STORY_TOOL)
    public JsonNode listUserStories() {
        return userStoryWebClient.getAllUserStories().block();
    }

    @Tool(name = "findUserStoriesByEpic", description = ToolPrompts.USER_STORY_TOOL)
    public JsonNode findUserStoriesByEpic(Long epicId) {
        return userStoryWebClient.getUserStoriesByEpic(epicId).block();
    }

}
