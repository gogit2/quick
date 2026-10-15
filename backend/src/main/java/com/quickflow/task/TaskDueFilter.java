package com.quickflow.task;

/** Contract schema TaskDueFilter. THIS_WEEK = Monday-Sunday of the current week in the user time zone. */
public enum TaskDueFilter {
    TODAY,
    OVERDUE,
    THIS_WEEK,
    NO_DUE_DATE
}
