package org.ms.mcp.workflows.client;

import org.ms.mcp.workflows.exception.AdoWorkflowException;

public class AdoParsingException extends AdoWorkflowException {
    public AdoParsingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
