package org.ms.mcp.workflows.exception;

/**
 * Excepción específica para errores de parsing en workflows de Azure DevOps
 */
public class AdoParsingException extends AdoWorkflowException {
    
    public AdoParsingException(String message) {
        super("Error de parsing: " + message);
    }
    
    public AdoParsingException(String message, Throwable cause) {
        super("Error de parsing: " + message, cause);
    }
}