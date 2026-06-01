package com.example.todoapp.dao;

import com.example.todoapp.Task;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Task} model using SQLite database.
 */
public class TaskDao {

    private static final String URL = "jdbc:sqlite:./todo.db";

    public TaskDao() {
        // Une seule et unique connexion pour TOUTE l'initialisation du fichier
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // 1. Création de la table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    done INTEGER NOT NULL DEFAULT 0
                );
            """);

            // 2. Vérification et insertion initiale directement en SQL
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO tasks (title, description, done) VALUES (?, ?, ?)")) {

                        pstmt.setString(1, "Réviser DS de maths");
                        pstmt.setString(2, "Séries numériques et probabilités.");
                        pstmt.setInt(3, 0);
                        pstmt.addBatch();

                        pstmt.setString(1, "Valider mon PIVE");
                        pstmt.setString(2, "PIVE Club Poker.");
                        pstmt.setInt(3, 1);
                        pstmt.addBatch();

                        pstmt.setString(1, "Choisir mon parcours de 4A");
                        pstmt.setString(2, "SIR ou SIA ?");
                        pstmt.setInt(3, 0);
                        pstmt.addBatch();

                        pstmt.executeBatch();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser la base de données SQLite", e);
        }
    }

    /**
     * Persist {@link Task} model into SQLite.
     */
    public Task save(Task task) {
        String sql = "INSERT INTO tasks (title, description, done) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, task.title());
            pstmt.setString(2, task.description());
            pstmt.setInt(3, task.done() ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Task(generatedKeys.getInt(1), task.title(), task.description(), task.done());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde SQL", e);
        }
        return task;
    }

    /**
     * Retrieve {@link Task} model by id from SQLite.
     */
    public Optional<Task> findById(int id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Task(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("done") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de recherche SQL", e);
        }
        return Optional.empty();
    }

    /**
     * Retrieve all {@link Task} models from SQLite.
     */
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("done") == 1
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de listing SQL", e);
        }
        return tasks;
    }

    /**
     * Delete a {@link Task} by its id from SQLite.
     */
    public void deleteById(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de suppression SQL", e);
        }
    }

    /**
     * Update an existing {@link Task} in SQLite.
     */
    public void update(int id, Task updatedTask) {
        String sql = "UPDATE tasks SET title = ?, description = ?, done = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, updatedTask.title());
            pstmt.setString(2, updatedTask.description());
            pstmt.setInt(3, updatedTask.done() ? 1 : 0);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de mise à jour SQL", e);
        }
    }
}