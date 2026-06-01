package com.example.todoapp;

import com.example.todoapp.dao.TaskDao;
import com.example.todoapp.dto.ErrorDto;
import com.example.todoapp.dto.TaskCreateDto;
import com.example.todoapp.dto.TaskUpdateDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main application entry point starting the HTTP Server.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    // Utilisation de ton port 8081
    private static final int PORT = 8081;
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/(\\d+)$");

    private static final TaskDao dao = new TaskDao();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/tasks", Application::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:" + PORT);
    }

    /**
     * Core router handling incoming HTTP requests on /tasks.
     */
    private static void handleTasks(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            log.info("Incoming request: {} {}", method, path);

            // ==========================================
            // [POST] /tasks - Création d'une tâche
            // ==========================================
            if ("POST".equals(method) && "/tasks".equals(path)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                TaskCreateDto inputDto = JsonUtils.deserialize(body, TaskCreateDto.class);

                // Validation du titre (Max 50 caractères)
                if (inputDto.title() == null || inputDto.title().isBlank() || inputDto.title().length() > 50) {
                    ErrorDto error = new ErrorDto("title", "Le titre est obligatoire et doit faire 50 caractères maximum.");
                    sendResponse(exchange, 400, JsonUtils.serialize(error));
                    return;
                }
                // Validation de la description (Max 255 caractères)
                if (inputDto.description() != null && inputDto.description().length() > 255) {
                    ErrorDto error = new ErrorDto("description", "La description doit faire 255 caractères maximum.");
                    sendResponse(exchange, 400, JsonUtils.serialize(error));
                    return;
                }

                // Conversion DTO -> Model (id géré par SQLite, done par défaut à false)
                Task taskToSave = new Task(null, inputDto.title(), inputDto.description(), false);
                Task createdTask = dao.save(taskToSave);

                exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
                sendResponse(exchange, 201, JsonUtils.serialize(createdTask));
                return;
            }

            // ==========================================
            // [GET] /tasks - Récupération de toutes les tâches
            // ==========================================
            if ("GET".equals(method) && "/tasks".equals(path)) {
                String query = exchange.getRequestURI().getQuery();
                boolean todoOnly = query != null && query.contains("todo-only=true");
                List<Task> tasks = dao.findAll();

                if (todoOnly) {
                    tasks = tasks.stream().filter(task -> !task.done()).toList();
                }

                if (tasks.isEmpty()) {
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 200, JsonUtils.serialize(tasks));
                }
                return;
            }

            // Gestion des endpoints à ID (/tasks/{id})
            Matcher m = ID_PATH.matcher(path);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(1));

                // ==========================================
                // [GET] /tasks/{id} - Récupération d'une tâche par ID
                // ==========================================
                if ("GET".equals(method)) {
                    Optional<Task> task = dao.findById(id);
                    if (task.isPresent()) {
                        sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
                    } else {
                        sendResponse(exchange, 404, null);
                    }
                    return;
                }

                // ==========================================
                // [DELETE] /tasks/{id} - Suppression d'une tâche
                // ==========================================
                if ("DELETE".equals(method)) {
                    Optional<Task> task = dao.findById(id);
                    if (task.isPresent()) {
                        dao.deleteById(id);
                        sendResponse(exchange, 204, null);
                    } else {
                        sendResponse(exchange, 404, null);
                    }
                    return;
                }

                // ==========================================
                // [PUT] /tasks/{id} - Modification complète d'une tâche
                // ==========================================
                if ("PUT".equals(method)) {
                    Optional<Task> task = dao.findById(id);
                    if (task.isEmpty()) {
                        sendResponse(exchange, 404, null);
                        return;
                    }

                    String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                    TaskUpdateDto updateDto = JsonUtils.deserialize(body, TaskUpdateDto.class);

                    // Validation du titre
                    if (updateDto.title() == null || updateDto.title().isBlank() || updateDto.title().length() > 50) {
                        ErrorDto error = new ErrorDto("title", "Le titre est obligatoire et doit faire 50 caractères maximum.");
                        sendResponse(exchange, 400, JsonUtils.serialize(error));
                        return;
                    }
                    // Validation de la description
                    if (updateDto.description() != null && updateDto.description().length() > 255) {
                        ErrorDto error = new ErrorDto("description", "La description doit faire 255 caractères maximum.");
                        sendResponse(exchange, 400, JsonUtils.serialize(error));
                        return;
                    }

                    // Enregistrement des modifications en BDD
                    Task updatedTask = new Task(id, updateDto.title(), updateDto.description(), updateDto.done());
                    dao.update(id, updatedTask);
                    sendResponse(exchange, 204, null);
                    return;
                }
            }

            // Route non prise en charge
            sendResponse(exchange, 404, null);

        } catch (Exception e) {
            log.error("Critical error encountered during request handling", e);
            // Étape 4 : Capture des exceptions inattendues et réponse HTTP 500
            String errorJson = "{\"error\": \"Internal Server Error\", \"message\": \"" + e.getMessage() + "\"}";
            sendResponse(exchange, 500, errorJson);
        }
    }

    /**
     * Utility method to write HTTP response headers and body.
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        if (responseBody == null) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            byte[] bytes = responseBody.getBytes(UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}