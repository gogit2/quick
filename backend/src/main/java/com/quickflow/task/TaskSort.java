package com.quickflow.task;

/** Contract schema TaskSort. Tasks without a due date sort last for the DUE_DATE_* options. */
public enum TaskSort {
    CREATED_DESC,
    CREATED_ASC,
    DUE_DATE_ASC,
    DUE_DATE_DESC
}
