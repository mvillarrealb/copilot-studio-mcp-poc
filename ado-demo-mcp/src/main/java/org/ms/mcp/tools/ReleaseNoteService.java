package org.ms.mcp.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ms.mcp.workflows.dto.EpicWorkflowResult;
import org.ms.mcp.workflows.service.AdoWorkflowService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReleaseNoteService {

    private final AdoWorkflowService adoWorkflowService;
    @Tool(name = "generateReleaseNotes", description = ToolPrompts.RELEASE_NOTE_TOOL)
    public EpicWorkflowResult generateReleaseNotes(String epicNameOrId) {
        Mono<EpicWorkflowResult> epicMono;
        try {
            Long epicId = Long.parseLong(epicNameOrId);
            epicMono =  adoWorkflowService.getWorkflowById(epicId);
        } catch (NumberFormatException e) {
            epicMono = adoWorkflowService.getWorkflowByPartialName(epicNameOrId);
        }
        return epicMono.block();
    }
}
