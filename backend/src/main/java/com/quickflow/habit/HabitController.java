package com.quickflow.habit;

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

/** Thin HTTP layer: delegates every rule to HabitService. */
@RestController
@RequestMapping("/api/habits")
@Tag(name = "Habits")
public class HabitController {

    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "listHabits", summary = "List habits")
    @ApiResponse(responseCode = "200", description = "Habits")
    public List<Habit> list(@RequestParam(name = "active", required = false) Boolean active) {
        return service.list(active);
    }

    @PostMapping
    @Operation(operationId = "createHabit", summary = "Create a habit")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ResponseStatus(HttpStatus.CREATED)
    public Habit create(@Valid @RequestBody HabitCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getHabit", summary = "Get a habit")
    @ApiResponse(responseCode = "200", description = "Habit")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Habit get(@PathVariable("id") long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateHabit", summary = "Update or (de)activate a habit")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Habit update(@PathVariable("id") long id, @Valid @RequestBody HabitUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteHabit", summary = "Delete a habit and its completions",
            description = "Also removed from plans")
    @ApiResponse(responseCode = "204", description = "Deleted (also removed from plans)")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/completions")
    @Operation(operationId = "listHabitCompletions", summary = "List completions of a habit")
    @ApiResponse(responseCode = "200", description = "Completions, newest first")
    @ApiResponse(responseCode = "404", description = "Not found")
    public List<HabitCompletion> listCompletions(
            @PathVariable("id") long id,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return service.listCompletions(id, from, to);
    }

    @PostMapping("/{id}/completions")
    @Operation(operationId = "completeHabit", summary = "Mark a habit complete for a date",
            description = "400 for a future date or an inactive habit. 409 if that habit already has a completion on that date (BR7), "
                    + "or, for a WEEKLY habit, in that Monday-Sunday week.")
    @ApiResponse(responseCode = "201", description = "Recorded")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "409", description = "Already completed")
    @ResponseStatus(HttpStatus.CREATED)
    public HabitCompletion complete(@PathVariable("id") long id,
                                    @Valid @RequestBody(required = false) HabitCompletionCreate body) {
        return service.complete(id, body == null ? null : body.completionDate());
    }

    @DeleteMapping("/{id}/completions/{date}")
    @Operation(operationId = "deleteHabitCompletion", summary = "Remove the completion of a date (undo)")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompletion(@PathVariable("id") long id,
                                 @PathVariable("date") @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        service.deleteCompletion(id, date);
    }
}
