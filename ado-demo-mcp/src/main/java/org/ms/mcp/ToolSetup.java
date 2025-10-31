package org.ms.mcp;

import org.ms.mcp.tools.ReleaseNoteService;
import org.ms.mcp.tools.UserStoryService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ToolSetup {
    @Bean
    public List<ToolCallback> tools(
            ReleaseNoteService releaseNoteService,
            UserStoryService codeReviewService
    ) {
        List<Object> toolCallbacks = new ArrayList<>();
        toolCallbacks.add(releaseNoteService);
        toolCallbacks.add(codeReviewService);
        return List.of(ToolCallbacks.from(toolCallbacks.toArray()));
    }
}
