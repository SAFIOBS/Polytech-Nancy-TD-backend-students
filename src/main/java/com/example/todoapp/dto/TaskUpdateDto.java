package com.example.todoapp.dto;

/**
 * DTO utilisé pour la mise à jour d'une tâche (PUT /tasks/{id}).
 */
public record TaskUpdateDto(String title, String description, boolean done) {}