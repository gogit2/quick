package com.quickflow.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP layer: delegates every rule to TaskService. */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "listTasks", summary = "List tasks")
    @ApiResponse(responseCode = "200", description = "Tasks (not paginated)")
    public List<Task> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) TaskPriority priority,
            @RequestParam(name = "due", required = false) TaskDueFilter due,
            @RequestParam(name = "dueFrom", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dueFrom,
            @RequestParam(name = "dueTo", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dueTo,
            @RequestParam(name = "archived", defaultValue = "false") boolean archived,
            @RequestParam(name = "sort", required = false) TaskSort sort) {
        return service.list(new TaskQuery(q, status, priority, due, dueFrom, dueTo, archived, sort));
    }

    @PostMapping
    @Operation(operationId = "createTask", summary = "Create a task",
            description = "Defaults: status TODO, priority MEDIUM.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getTask", summary = "Get a task")
    @ApiResponse(responseCode = "200", description = "Task")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Task get(@PathVariable("id") long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateTask", summary = "Update a task")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Task update(@PathVariable("id") long id, @Valid @RequestBody TaskUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteTask", summary = "Delete a task for good",
            description = "Also removed from plans")
    @ApiResponse(responseCode = "204", description = "Deleted (also removed from plans)")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(operationId = "completeTask", summary = "Mark a task completed (status DONE)")
    @ApiResponse(responseCode = "200", description = "Updated task")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Task complete(@PathVariable("id") long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/archive")
    @Operation(operationId = "archiveTask", summary = "Archive a task")
    @ApiResponse(responseCode = "200", description = "Updated task")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Task archive(@PathVariable("id") long id) {
        return service.archive(id);
    }

    @PostMapping("/{id}/restore")
    @Operation(operationId = "restoreTask", summary = "Restore an archived task")
    @ApiResponse(responseCode = "200", description = "Updated task")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Task restore(@PathVariable("id") long id) {
        return service.restore(id);
    }
}
