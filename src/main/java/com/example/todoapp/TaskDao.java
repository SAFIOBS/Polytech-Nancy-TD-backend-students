package com.example.todoapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private final Map<Integer, Task> storage = new HashMap<>();

    {
        save(new Task(1, "Réviser DS de maths", "Séries numériques et probabilités.", false));
        save(new Task(2, "Valider mon PIVE", "PIVE Club Poker.", true));
        save(new Task(3, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
    }

    /**
     * Persist {@link Task} model.
     * @param task task to save.
     * @return task model.
     */
    public Task save(Task task) {
        storage.put(task.id(), task);
        return task;
    }

    /**
     * Retrieve {@link Task} model by id.
     * @param id identifier of the {@link Task}.
     * @return {@link Task} model wrapped by Optional.
     */
    public Optional<Task> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Retrieve all {@link Task} models.
     * @return List of all tasks.
     */
    public List<Task> findAll() {
        return new ArrayList<>(storage.values());
    }

    /**
     * Delete a {@link Task} by its id.
     * @param id identifier of the {@link Task}.
     */
    public void deleteById(int id) {
        storage.remove(id);
    }

    /**
     * Update an existing {@link Task}.
     * @param id identifier of the task to update.
     * @param updatedTask new task data.
     */
    public void update(int id, Task updatedTask) {
        // On s'assure que l'objet enregistré a bien l'ID passé dans l'URL
        Task taskToSave = new Task(id, updatedTask.title(), updatedTask.description(), updatedTask.done());
        storage.put(id, taskToSave);
    }
}