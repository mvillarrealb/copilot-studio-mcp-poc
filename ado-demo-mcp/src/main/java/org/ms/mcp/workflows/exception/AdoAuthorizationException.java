package org.ms.mcp.workflows.exception;

/**
 * Excepción específica para errores de autorización en workflows de Azure DevOps
 */
public class AdoAuthorizationException extends AdoWorkflowException {
    
    public AdoAuthorizationException(String message) {
        super("Error de autorización: " + message);
    }
    
    public AdoAuthorizationException(String message, Throwable cause) {
        super("Error de autorización: " + message, cause);
    }
}