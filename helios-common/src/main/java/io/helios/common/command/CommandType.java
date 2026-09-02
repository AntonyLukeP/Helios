package io.helios.common.command;

public enum CommandType {
    ScheduleActivity,
    RequestCancelActivity,
    StartTimer,
    CancelTimer,
    RecordMarker,
    StartChildWorkflow,
    RequestCancelChildWorkflow,
    SignalExternalWorkflow,
    RequestCancelExternalWorkflow,
    CompleteWorkflow,
    FailWorkflow,
    CancelWorkflow,
    ContinueAsNew;

    public String wire() {
        return name();
    }

    public static CommandType from(String name) {
        return valueOf(name);
    }
}
