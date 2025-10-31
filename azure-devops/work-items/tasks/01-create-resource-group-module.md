# Tarea: Crear módulo de Resource Group con variables de ubicación y tags

## Historia de Usuario
Configurar Módulos de Terraform para Azure

## Descripción
Crear un módulo reutilizable de Terraform para gestionar Resource Groups con variables parametrizables para ubicación y tags.

## Criterios de Aceptación
- [ ] Archivo main.tf con recurso azurerm_resource_group
- [ ] Variables definidas para name, location y tags
- [ ] Outputs para id, name y location del resource group
- [ ] Validación de variables implementada

## Esfuerzo Estimado
3 Story Points

## Asignado a
DevOps Engineer

## Prioridad
Alta

## Tareas Técnicas
1. Crear estructura de carpetas modules/resource-group/
2. Definir variables.tf con validaciones
3. Implementar main.tf con recurso principal
4. Configurar outputs.tf
5. Crear README.md con documentación de uso