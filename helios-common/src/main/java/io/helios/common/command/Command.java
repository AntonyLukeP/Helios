package io.helios.common.command;

import java.util.Objects;

public sealed interface Command 
        permits Command.ScheduleActivity, Command.StartTimer, Command.RecordMarker,
        Command.CompleteWorkflow, Command.FailWorkflow{

        CommandType type();

        default boolean isTerminal() {
        return false;
    }

    record ScheduleActivity(String activityId, String activityType, String taskQueue,
            EncodedPayload input) implements Command {
        public ScheduleActivity {
            Objects.requireNonNull(activityId, "activityId");
            Objects.requireNonNull(activityType, "activityType");
            Objects.requireNonNull(taskQueue, "taskQueue");
        }

        @Override
        public CommandType type() {
            return CommandType.ScheduleActivity;
        }
    }

    record StartTimer(String timerId, Duration fireAfter) implements Command {
        public StartTimer {
            Objects.requireNonNull(timerId, "timerId");
            Objects.requireNonNull(fireAfter, "fireAfter");
        }

        @Override
        public CommandType type() {
            return CommandType.StartTimer;
        }
    }

    record RecordMarker(String markerName, EncodedPayload details) implements Command {
        public RecordMarker {
            Objects.requireNonNull(markerName, "markerName");
        }

        @Override
        public CommandType type() {
            return CommandType.RecordMarker;
        }
    }

    record CompleteWorkflow(EncodedPayload result) implements Command {
        @Override
        public CommandType type() {
            return CommandType.CompleteWorkflow;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    }

    record FailWorkflow(String reason, EncodedPayload details) implements Command {
        @Override
        public CommandType type() {
            return CommandType.FailWorkflow;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    }


}
