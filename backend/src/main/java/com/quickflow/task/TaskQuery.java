package com.quickflow.task;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Search, filter and sort options of GET /api/tasks. Null options are not applied; sort defaults to CREATED_DESC.
 * archived=false (default) lists non-archived tasks only (BR4), archived=true lists archived tasks only.
 */
public record TaskQuery(
        String q,
        TaskStatus status,
        TaskPriority priority,
        TaskDueFilter due,
        LocalDate dueFrom,
        LocalDate dueTo,
        boolean archived,
        TaskSort sort) {

    public TaskQuery {
        sort = sort == null ? TaskSort.CREATED_DESC : sort;
    }

    /** Builds the JPA specification. today/weekStart/weekEnd come from UserTime (user time zone). */
    public Specification<TaskEntity> toSpecification(LocalDate today, LocalDate weekStart, LocalDate weekEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("archived"), archived));
            if (q != null && !q.isBlank()) {
                Expression<Integer> position = cb.locate(cb.lower(root.get("title")), q.toLowerCase(Locale.ROOT));
                predicates.add(cb.gt(position, 0));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (due != null) {
                predicates.add(duePredicate(root, cb, today, weekStart, weekEnd));
            }
            if (dueFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("dueDate"), dueFrom));
            }
            if (dueTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dueDate"), dueTo));
            }
            if (query != null) {
                query.orderBy(orders(root, cb));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate duePredicate(Root<TaskEntity> root, CriteriaBuilder cb,
                                   LocalDate today, LocalDate weekStart, LocalDate weekEnd) {
        Path<LocalDate> dueDate = root.get("dueDate");
        return switch (due) {
            case TODAY -> cb.equal(dueDate, today);
            // Same rule as TaskMapper.isOverdue: not DONE and not archived.
            case OVERDUE -> cb.and(
                    cb.lessThan(dueDate, today),
                    cb.notEqual(root.get("status"), TaskStatus.DONE),
                    cb.equal(root.get("archived"), false));
            case THIS_WEEK -> cb.between(dueDate, weekStart, weekEnd);
            case NO_DUE_DATE -> cb.isNull(dueDate);
        };
    }

    private List<Order> orders(Root<TaskEntity> root, CriteriaBuilder cb) {
        Path<LocalDate> dueDate = root.get("dueDate");
        Path<Long> id = root.get("id");
        // Null due dates last: a 0/1 key sorts the non-null dates first, then the date itself.
        Expression<Integer> dueMissing = cb.<Integer>selectCase()
                .when(cb.isNull(dueDate), 1)
                .otherwise(0);
        return switch (sort) {
            case CREATED_DESC -> List.of(cb.desc(root.get("createdAt")), cb.desc(id));
            case CREATED_ASC -> List.of(cb.asc(root.get("createdAt")), cb.asc(id));
            case DUE_DATE_ASC -> List.of(cb.asc(dueMissing), cb.asc(dueDate), cb.asc(id));
            case DUE_DATE_DESC -> List.of(cb.asc(dueMissing), cb.desc(dueDate), cb.desc(id));
        };
    }
}
