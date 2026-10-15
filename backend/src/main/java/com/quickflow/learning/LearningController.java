package com.quickflow.learning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP layer: delegates every rule to LearningService. */
@RestController
@RequestMapping("/api/learning-cards")
@Tag(name = "Learning")
public class LearningController {

    private final LearningService service;

    public LearningController(LearningService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "listLearningCards", summary = "List learning cards with milestones and notes")
    @ApiResponse(responseCode = "200", description = "Cards, newest first")
    public List<LearningCard> list() {
        return service.list();
    }

    @PostMapping
    @Operation(operationId = "createLearningCard", summary = "Add a learning card")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningCard create(@Valid @RequestBody LearningCardCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getLearningCard", summary = "Get a learning card")
    @ApiResponse(responseCode = "200", description = "Card")
    @ApiResponse(responseCode = "404", description = "Not found")
    public LearningCard get(@PathVariable("id") long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateLearningCard", summary = "Update a learning card",
            description = "A status is accepted only while the card has no milestones; otherwise 409.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "409", description = "Status follows the milestones")
    public LearningCard update(@PathVariable("id") long id, @Valid @RequestBody LearningCardUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteLearningCard", summary = "Remove a learning card with its milestones and notes",
            description = "Also removed from plans")
    @ApiResponse(responseCode = "204", description = "Deleted (also removed from plans)")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/milestones")
    @Operation(operationId = "createLearningMilestone", summary = "Add a milestone to a card")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMilestone createMilestone(@PathVariable("id") long id,
                                             @Valid @RequestBody LearningMilestoneCreate body) {
        return service.addMilestone(id, body);
    }

    @PutMapping("/{id}/milestones/{milestoneId}")
    @Operation(operationId = "updateLearningMilestone", summary = "Update or complete a milestone")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    public LearningMilestone updateMilestone(@PathVariable("id") long id,
                                             @PathVariable("milestoneId") long milestoneId,
                                             @Valid @RequestBody LearningMilestoneUpdate body) {
        return service.updateMilestone(id, milestoneId, body);
    }

    @DeleteMapping("/{id}/milestones/{milestoneId}")
    @Operation(operationId = "deleteLearningMilestone", summary = "Remove a milestone")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMilestone(@PathVariable("id") long id, @PathVariable("milestoneId") long milestoneId) {
        service.deleteMilestone(id, milestoneId);
    }

    @PostMapping("/{id}/notes")
    @Operation(operationId = "createLearningNote", summary = "Add a note to a card")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningNote createNote(@PathVariable("id") long id, @Valid @RequestBody LearningNoteCreate body) {
        return service.addNote(id, body);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @Operation(operationId = "deleteLearningNote", summary = "Remove a note")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable("id") long id, @PathVariable("noteId") long noteId) {
        service.deleteNote(id, noteId);
    }
}
