package io.helios.common.run;

public enum WorkflowIdReusePolicy {
    
    AllowDuplicate,
    AllowDuplicateFailedOnly,
    RejectDuplicate,
    TerminateIfRunning;

    public String wire() {
        return name();
    }

    public static WorkflowIdReusePolicy fromWire(String s) {
        return valueOf(s);
    }
}
