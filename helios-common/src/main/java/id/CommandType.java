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
}
