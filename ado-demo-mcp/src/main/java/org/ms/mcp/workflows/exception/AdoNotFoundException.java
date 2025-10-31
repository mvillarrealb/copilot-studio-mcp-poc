package org.ms.mcp.workflows.exception;

/**
 * Excepción específica para recursos no encontrados en workflows de Azure DevOps
 */
public class AdoNotFoundException extends AdoWorkflowException {
    
    public AdoNotFoundException(String message) {
        super("Recurso no encontrado: " + message);
    }
    
    public AdoNotFoundException(String message, Throwable cause) {
        super("Recurso no encontrado: " + message, cause);
    }
}