package io.helios.common.run;

public enum RunState {
    Running,
    Completed,
    Failed,
    TimedOut,
    Canceled,
    Terminated,
    ContinuedAsNew,
    Quarantined;

    public boolean isClosed() {
        return switch (this) {
            case Completed, Failed, TimedOut, Canceled, Terminated, ContinuedAsNew -> true;
            case Running, Quarantined -> false;
        };
    }

    public boolean isOpen() {
        return !isClosed();
    }

}
