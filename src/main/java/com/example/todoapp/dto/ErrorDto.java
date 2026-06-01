package com.example.todoapp.dto;

/**
 * DTO retourné en cas d'erreur de validation (Code HTTP 400).
 */
public record ErrorDto(String field, String message) {}