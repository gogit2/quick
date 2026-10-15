package com.quickflow.task;

import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import com.quickflow.common.UserTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for tasks. Time comes from the injected Clock; "today" from UserTime. */
@Service
public class TaskService {

    private final TaskRepository repository;
    private final UserTime userTime;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public TaskService(TaskRepository repository, UserTime userTime, Clock clock,
                       ApplicationEventPublisher events) {
        this.repository = repository;
        this.userTime = userTime;
        this.clock = clock;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Task> list(TaskQuery query) {
        LocalDate today = userTime.today();
        return repository.findAll(query.toSpecification(today, userTime.weekStart(), userTime.weekEnd()))
                .stream()
                .map(e -> TaskMapper.toDto(e, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public Task get(long id) {
        return toDto(load(id));
    }

    /** New tasks default to status TODO and priority MEDIUM (D-11). */
    @Transactional
    public Task create(TaskCreate in) {
        TaskStatus status = in.status() == null ? TaskStatus.TODO : in.status();
        TaskPriority priority = in.priority() == null ? TaskPriority.MEDIUM : in.priority();
        TaskEntity entity = new TaskEntity(in.title().trim(), in.description(), status, priority, in.dueDate(),
                Instant.now(clock));
        return toDto(repository.save(entity));
    }

    /** Full replacement. A missing status or priority keeps the current value. */
    @Transactional
    public Task update(long id, TaskUpdate in) {
        TaskEntity entity = load(id);
        entity.setTitle(in.title().trim());
        entity.setDescription(in.description());
        if (in.status() != null) {
            entity.setStatus(in.status());
        }
        if (in.priority() != null) {
            entity.setPriority(in.priority());
        }
        entity.setDueDate(in.dueDate());
        touch(entity);
        return toDto(entity);
    }

    /** BR5: completing a task sets its status to DONE. */
    @Transactional
    public Task complete(long id) {
        TaskEntity entity = load(id);
        entity.setStatus(TaskStatus.DONE);
        touch(entity);
        return toDto(entity);
    }

    @Transactional
    public Task archive(long id) {
        TaskEntity entity = load(id);
        entity.setArchived(true);
        touch(entity);
        return toDto(entity);
    }

    @Transactional
    public Task restore(long id) {
        TaskEntity entity = load(id);
        entity.setArchived(false);
        touch(entity);
        return toDto(entity);
    }

    /** Internal (not in the API): sets status TODO. Used by plans later (D-3). */
    @Transactional
    public void reopen(long id) {
        TaskEntity entity = load(id);
        entity.setStatus(TaskStatus.TODO);
        touch(entity);
    }

    /** Deletes for good and tells the plans so their items are removed (BR14, D-5). */
    @Transactional
    public void delete(long id) {
        repository.delete(load(id));
        events.publishEvent(new SourceDeletedEvent(PlanItemSourceType.TASK, id));
    }

    private TaskEntity load(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
    }

    private void touch(TaskEntity entity) {
        entity.setUpdatedAt(Instant.now(clock));
    }

    private Task toDto(TaskEntity entity) {
        return TaskMapper.toDto(entity, userTime.today());
    }
}
