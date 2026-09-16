package io.helios.core.workflow;

import io.helios.core.command.Command;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.event.ActivityCompleted;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.history.RecordedEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The authoring surface available to {@link ReplayableWorkflowDefinition} code.
 *
 * <p>On each replay turn the executor creates a {@code WorkflowContext} wired
 * to the run's current event history. When workflow code calls
 * {@link #executeActivity}, the context determines whether the requested
 * activity is:
 * <ul>
 *   <li><strong>Already complete in history</strong> â€” return the recorded result
 *       and let execution continue (fast-forward, Case A).
 *   <li><strong>Scheduled but not yet complete</strong> â€” throw
 *       {@link WorkflowSuspended} without emitting a new command (Case B).
 *   <li><strong>New, at the history frontier</strong> â€” emit a
 *       {@link ScheduleActivity} command and throw {@link WorkflowSuspended}
 *       (Case C).
 * </ul>
 *
 * <p>Construction is package-private. Only the replay executor (same package)
 * creates {@code WorkflowContext} instances. Workflow authors receive a fully
 * configured context as a parameter and must never construct one themselves.
 *
 * <p>This class is intentionally single-threaded. No concurrent access is
 * supported in Phase 3.
 */
public final class WorkflowContext {

    /**
     * Scheduled activities extracted from the run history, in sequence order.
     * This is the "command history" that the replay must match exactly.
     */
    private final List<ActivityScheduled> scheduledHistory;

    /**
     * Maps {@code activityId} â†’ recorded result for every completed activity.
     * Uses a plain {@code HashMap} (not {@code Map.copyOf}) because result values
     * may be null, and {@code Map.copyOf} forbids null values.
     */
    private final Map<String, String> completionResultById;

    /**
     * Counts {@link #executeActivity} calls made on this context instance.
     * Used to generate the deterministic activity ID {@code activity-<callCounter>}.
     */
    private int callCounter = 0;

    /**
     * Current position in {@link #scheduledHistory}.
     * Advances on every successful history match or frontier emission.
     * When {@code commandCursor == scheduledHistory.size()}, execution is at
     * the <em>history frontier</em>: all previously recorded decisions have
     * been replayed and any new decision will create new history.
     */
    private int commandCursor = 0;

    /** Newly emitted commands accumulated during this replay turn. */
    private final List<Command> newCommandsList = new ArrayList<>();

    /** Unmodifiable view of {@link #newCommandsList} exposed to the executor. */
    private final List<Command> newCommandsView = Collections.unmodifiableList(newCommandsList);

    // â”€â”€ construction â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Package-private: empty-history seam.
     *
     * <p>Convenience form used by tests and the future executor when starting a
     * brand-new run with no prior history. Equivalent to
     * {@code WorkflowContext(List.of())}.
     */
    WorkflowContext() {
        this(List.of());
    }

    /**
     * Package-private: the replay executor creates contexts with the run's events.
     *
     * <p>Extracts all {@link ActivityScheduled} events in sequence order to form
     * the command history, and indexes all {@link ActivityCompleted} events by
     * {@code activityId} for O(1) completion lookup.
     *
     * @param runHistory the ordered recorded events for exactly one workflow run
     */
    WorkflowContext(List<RecordedEvent> runHistory) {
        Objects.requireNonNull(runHistory, "runHistory must not be null");

        List<ActivityScheduled> scheduled = new ArrayList<>();
        Map<String, String> completions = new HashMap<>();

        for (RecordedEvent re : runHistory) {
            switch (re.event()) {
                case ActivityScheduled as -> scheduled.add(as);
                case ActivityCompleted ac -> completions.put(ac.activityId(), ac.result());
                default -> {} // WorkflowStarted, WorkflowCompleted â€” not relevant here
            }
        }

        this.scheduledHistory = List.copyOf(scheduled);
        this.completionResultById = completions; // private; read-only after construction
    }

    // â”€â”€ public authoring API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Schedules an external activity and returns its recorded result.
     *
     * <p>Behaviour depends on replay position:
     * <ol>
     *   <li><strong>Replay (cursor inside history):</strong> compare the generated
     *       ID, activity type, and input against the recorded
     *       {@link ActivityScheduled} at the cursor. Any mismatch is a programming
     *       error and throws {@link NonDeterminismException}. If fields match:
     *       advance the cursor, then return the completion result if present, or
     *       throw {@link WorkflowSuspended} if the activity is not yet complete.
     *   <li><strong>Frontier (cursor at end of history):</strong> emit a new
     *       {@link ScheduleActivity} command and throw {@link WorkflowSuspended}.
     * </ol>
     *
     * @param activityType the name of the activity to execute; must not be null or blank
     * @param input        the serialized input for the activity; may be null
     * @return the serialized result recorded by a prior {@link ActivityCompleted} event
     * @throws IllegalArgumentException  if {@code activityType} is null or blank
     * @throws NonDeterminismException   if the call does not match the history at this position
     * @throws WorkflowSuspended         if execution must pause (incomplete or new activity)
     */
    public String executeActivity(String activityType, String input) {
        if (activityType == null || activityType.isBlank()) {
            throw new IllegalArgumentException("activityType must not be blank");
        }

        callCounter++;
        String activityId = "activity-" + callCounter;

        if (commandCursor < scheduledHistory.size()) {
            return replayAgainstHistory(activityId, activityType, input);
        }

        return emitNewCommand(activityId, activityType, input);
    }

    // â”€â”€ package-private executor seam â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns an unmodifiable view of the commands emitted during this replay turn.
     *
     * <p>The executor reads this list after catching {@link WorkflowSuspended} to
     * determine what new events to append to history.
     */
    List<Command> newCommands() {
        return newCommandsView;
    }

    /**
     * Package-private: true if the executor has not yet replayed all recorded
     * scheduled activities.
     *
     * <p>The executor calls this after the workflow returns normally. If true,
     * the workflow returned before consuming all activities that history records
     * were previously scheduled — a non-determinism error.
     *
     * <p>Formally: {@code commandCursor < scheduledHistory.size()}.
     */
    boolean hasUnreplayedHistory() {
        return commandCursor < scheduledHistory.size();
    }

    // â”€â”€ private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Replay path: the cursor is inside the recorded scheduled-activity history.
     *
     * <p>Validates fields, advances the cursor, and either returns a completion
     * result (Case A) or suspends (Case B).
     */
    private String replayAgainstHistory(String activityId, String activityType, String input) {
        ActivityScheduled expected = scheduledHistory.get(commandCursor);

        if (!expected.activityId().equals(activityId)) {
            throw new NonDeterminismException("activityId", expected.activityId(), activityId);
        }
        if (!expected.activityType().equals(activityType)) {
            throw new NonDeterminismException(
                    "activityType", expected.activityType(), activityType);
        }
        if (!Objects.equals(expected.input(), input)) {
            throw new NonDeterminismException("input", expected.input(), input);
        }

        commandCursor++;

        // Case A: completion recorded â€” return result and let execution continue.
        if (completionResultById.containsKey(activityId)) {
            return completionResultById.get(activityId);
        }

        // Case B: scheduled but not yet complete â€” suspend with no new command.
        throw WorkflowSuspended.INSTANCE;
    }

    /**
     * Frontier path: the cursor is at or beyond the end of recorded history.
     *
     * <p>Emits one {@link ScheduleActivity} command and suspends (Case C).
     */
    private String emitNewCommand(String activityId, String activityType, String input) {
        newCommandsList.add(new ScheduleActivity(activityId, activityType, input));
        commandCursor++;
        throw WorkflowSuspended.INSTANCE;
    }
}
