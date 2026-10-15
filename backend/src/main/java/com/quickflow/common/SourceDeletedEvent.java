package com.quickflow.common;

/** Published when a task, habit or learning card is deleted, so plan items can be cleaned up (FR-07, BR14). */
public record SourceDeletedEvent(PlanItemSourceType type, long id) {
}
