package org.ms.mcp;

import org.ms.mcp.tools.ReleaseNoteTools;
import org.ms.mcp.tools.UserStoryTools;
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
            ReleaseNoteTools releaseNoteService,
            UserStoryTools codeReviewService
    ) {
        List<Object> toolCallbacks = new ArrayList<>();
        toolCallbacks.add(releaseNoteService);
        toolCallbacks.add(codeReviewService);
        return List.of(ToolCallbacks.from(toolCallbacks.toArray()));
    }
}
