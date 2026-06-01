package com.example.todoapp.dto;

/**
 * DTO utilisé pour la création d'une tâche (POST /tasks).
 */
public record TaskCreateDto(String title, String description) {}