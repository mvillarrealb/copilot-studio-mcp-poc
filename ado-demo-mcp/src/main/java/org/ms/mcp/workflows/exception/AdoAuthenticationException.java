package org.ms.mcp.workflows.exception;

/**
 * Excepción específica para errores de autenticación en workflows de Azure DevOps
 */
public class AdoAuthenticationException extends AdoWorkflowException {
    
    public AdoAuthenticationException(String message) {
        super("Error de autenticación: " + message);
    }
    
    public AdoAuthenticationException(String message, Throwable cause) {
        super("Error de autenticación: " + message, cause);
    }
}