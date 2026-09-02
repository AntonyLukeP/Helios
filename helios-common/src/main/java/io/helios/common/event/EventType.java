package io.helios.common.event;

public enum EventType {
    WorkflowExecutionStarted,
    WorkflowExecutionCompleted,
    WorkflowExecutionFailed,
    WorkflowExecutionTimedOut,
    WorkflowExecutionCanceled,
    WorkflowExecutionTerminated,
    WorkflowExecutionContinuedAsNew,
    WorkflowExecutionSignaled,
    WorkflowExecutionCancelRequested,

    WorkflowTaskScheduled,
    WorkflowTaskStarted,
    WorkflowTaskCompleted,
    WorkflowTaskFailed,
    WorkflowTaskTimedOut,

    ActivityTaskScheduled,
    ActivityTaskStarted,
    ActivityTaskCompleted,
    ActivityTaskFailed,
    ActivityTaskTimedOut,
    ActivityTaskCancelRequested,
    ActivityTaskCanceled,

    TimerStarted,
    TimerFired,
    TimerCanceled,

    MarkerRecorded,

    StartChildWorkflowExecutionInitiated,
    ChildWorkflowExecutionStarted,
    ChildWorkflowExecutionCompleted,
    ChildWorkflowExecutionFailed,
    ChildWorkflowExecutionTimedOut,
    ChildWorkflowExecutionCanceled,
    ChildWorkflowExecutionTerminated,

    SignalExternalWorkflowExecutionInitiated,
    ExternalWorkflowExecutionSignaled,
    RequestCancelExternalWorkflowExecutionInitiated,
    ExternalWorkflowExecutionCancelRequested;

    public String wire() {
        return name();
    }

    public static EventType fromWire(String s) {
        return valueOf(s);
    }
}