package com.quickflow.plan;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP layer: delegates every rule to PlanService. */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "listPlans", summary = "List plans ordered by priorityOrder, then start")
    @ApiResponse(responseCode = "200", description = "Plans (not paginated)")
    public List<Plan> list(@RequestParam(name = "group", required = false) PlanGroup group) {
        return service.list(group);
    }

    @PostMapping
    @Operation(operationId = "createPlan", summary = "Create a plan from existing tasks, habits and learning cards",
            description = "400 when there are no items (BR10), end is not after start (BR11), a source does not exist, "
                    + "an item is duplicated, or the duration is not positive. A duration longer than the window is "
                    + "accepted (durationExceedsWindow=true).")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ResponseStatus(HttpStatus.CREATED)
    public Plan create(@Valid @RequestBody PlanCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPlan", summary = "Get a plan")
    @ApiResponse(responseCode = "200", description = "Plan")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Plan get(@PathVariable("id") long id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deletePlan", summary = "Remove a plan (sources are untouched)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        service.delete(id);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(operationId = "updatePlanItem", summary = "Mark a plan item done or not done",
            description = "TASK items also set the task to DONE (done=true) or TODO (done=false). HABIT and "
                    + "LEARNING_RESOURCE items only change the item flag.")
    @ApiResponse(responseCode = "200", description = "The whole plan with recomputed progress")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Plan updateItem(@PathVariable("id") long id, @PathVariable("itemId") long itemId,
                           @Valid @RequestBody PlanItemUpdate body) {
        return service.setItemDone(id, itemId, body.done());
    }

    @PostMapping("/{id}/acknowledge-start")
    @Operation(operationId = "acknowledgePlanStart", summary = "Record that the start notice was shown",
            description = "Idempotent. Used for the one-time catch-up notice when a plan started while the app was closed.")
    @ApiResponse(responseCode = "200", description = "Plan with startNotified=true")
    @ApiResponse(responseCode = "404", description = "Not found")
    public Plan acknowledgeStart(@PathVariable("id") long id) {
        return service.acknowledgeStart(id);
    }
}
