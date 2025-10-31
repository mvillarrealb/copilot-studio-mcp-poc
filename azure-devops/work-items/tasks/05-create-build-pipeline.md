# Tarea: Crear pipeline YAML de construcción con etapas de build y test

## Historia de Usuario
Configurar Pipeline de Construcción

## Descripción
Desarrollar un pipeline YAML en Azure DevOps que automatice la construcción y ejecución de pruebas para la aplicación.

## Criterios de Aceptación
- [ ] Pipeline YAML con trigger automático en main branch
- [ ] Etapa de build con compilación de aplicación
- [ ] Etapa de test con ejecución de unit tests
- [ ] Variables de entorno configuradas
- [ ] Logs detallados de cada etapa

## Esfuerzo Estimado
8 Story Points

## Asignado a
DevOps Engineer

## Prioridad
Alta

## Tareas Técnicas
1. Crear azure-pipelines.yml en raíz del proyecto
2. Configurar trigger para rama main
3. Definir etapas de build con Gradle/Maven
4. Configurar ejecución de pruebas unitarias
5. Implementar publicación de resultados de tests