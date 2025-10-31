package org.ms.mcp.tools;

public class ToolPrompts {
    public static final String RELEASE_NOTE_TOOL = """
    Eres el encargado de generar notas de la versión para un proyecto de software, deberás recibir como argumento
    un descriptor del hito(milestone) o Epica de la cual estamos generando el Release, también podrías recibir
    el Identificador de la Epica o Hito.
    
    En base a cualquiera de estos 2 parámetros va a ocurrir lo siguiente:
    1. Se realizara la búsqueda de todas las historias de usuario (Issues) que estén asociadas a dicha épica o hito.
    2. Se buscaran las Tareas (Tasks) que estén asociadas a las historias de usuario encontradas en el paso anterior.
    
    Con esta información recibirás un json consolidado que deberás transformar en la nota de la versión en formato markdown
    siguiendo la siguiente estructura:
    # Notas de la Versión - [Nombre del Proyecto]
    ## Hito: [Nombre del Hito o Epica] ([ID del Hito])
    ### Fecha de Lanzamiento: [Fecha Actual]
    ### Historias de Usuario:
    - [ID de la Historia de Usuario]: [Título de la Historia de Usuario]
        - Tareas:
            - [ID de la Tarea]: [Título de la Tarea]
            - [ID de la Tarea]: [Título de la Tarea]
    - [ID de la Historia de Usuario]: [Título de la Historia de Usuario]
        - Tareas:
        
     Al final deberás agregar una sección de Resumen con el siguiente formato:
    ### Resumen:
    - Total de Historias de Usuario: [Número Total]
    - Total de Tareas: [Número Total]
    """;

    public static final String USER_STORY_TOOL = """
    Lista Todas las historias de usuario (Issues) del proyecto en Azure DevOps.
    * Si el usuario proporciona un ID de épica, filtra las historias de usuario que pertenecen a esa épica.
    * Si el usuario no proporciona un ID de épica, devuelve todas las historias de usuario.
    * Si el usuario suministra el nombre de una Epica deberás buscar su ID primero para posteriormente
    Buscar las historias de usuario que pertenecen a esa épica.
    """;
}
