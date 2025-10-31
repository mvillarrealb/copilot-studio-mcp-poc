package org.ms.mcp.workflows.exception;

/**
 * Excepción base para todos los errores relacionados con workflows de Azure DevOps
 */
public class AdoWorkflowException extends RuntimeException {
    
    public AdoWorkflowException(String message) {
        super(message);
    }
    
    public AdoWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AdoWorkflowException(Throwable cause) {
        super(cause);
    }
}