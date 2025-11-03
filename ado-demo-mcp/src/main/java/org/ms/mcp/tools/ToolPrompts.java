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

    public static final String FIND_EPICS_TOOL = """
      Encuentra una o varias épicas (hitos o milestones) en el proyecto de Azure DevOps.
      El usuario podrá hacer referencia al ID de la Epica o al nombre de la misma en ambos casos podrás
      hacer la búsqueda, si no se suministra alguno de estos 2 parámetros no podrás hacer uso de esta herramienta.
      Ejemplos:
      - Quiero saber sobre la epica con ID 12345
      - Encuentra la epica llamada "Lanzamiento Versión 2.0"
      - Muéstrame todas las épicas relacionadas con "Mejoras de rendimiento"
      Considera que también podrá referenciarse una epica a terminos similares como:
        - Hito
        - Milestone
        - Objetivo
        - Meta
        - Proyecto
       Consideraciones: Si el usuario no brinda una pista clara sobre la epica solo enviar una cadena de texto vacía
       para realizar la búsqueda
    
       Ejemplos:
       Prompt: Quiero ver mis epicas/ tool Call: findEpics('') // Lista todas las historias de usuario
       Prompt: Quiero ver los detalles de la Epica 12345 / tool Call: findEpics('12345') // Lista las historias de usuario asociadas a la epica con ID 12345
       Prompt: Quiero ver la epica del Lanzamiento Versión 2.0 / tool Call:   findEpics('Lanzamiento Versión 2.0') // Lista las historias de usuario asociadas a la epica llamada "Lanzamiento Versión 2.0"
   
     """;

    public static final String USER_STORY_TOOL = """
    Lista Todas las historias de usuario (Issues) del proyecto en Azure DevOps.
    """;

    public static final String USER_STORY_BY_EPIC_PROMPT = """
    Lista Todas las historias de usuario (Issues) del proyecto en Azure DevOps que pertenecen
    El usuario podrá hacer referencia al ID de la Epica o al nombre de la misma en ambos casos podrás
    hacer la búsqueda, si no se suministra alguno de estos 2 parámetros no podrás hacer uso de esta herramienta.
    Consideraciones: Si el usuario no brinda una pista clara sobre la epica solo enviar una cadena de texto vacía
    para realizar la búsqueda
    Ejemplos:
       Prompt: Quiero ver todas mis historias de usuario/ tool Call: findUserStoriesByEpic('') // Lista todas las historias de usuario
       Prompt: Quiero ver las historias de usuario de la Epica 12345 / tool Call: findUserStoriesByEpic('12345') // Lista las historias de usuario asociadas a la epica con ID 12345
       Prompt: Quiero ver las historias de usuario del Lanzamiento Versión 2.0 / tool Call:   findUserStoriesByEpic('Lanzamiento Versión 2.0') // Lista las historias de usuario asociadas a la epica llamada "Lanzamiento Versión 2.0"
    """;
}
