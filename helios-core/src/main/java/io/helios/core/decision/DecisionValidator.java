package io.helios.core.decision;

import io.helios.core.command.Command;
import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates an ordered list of commands that form one workflow decision.
 *
 * <p>A decision is the set of commands a workflow issues in a single execution
 * step. This validator enforces structural invariants before any command is
 * translated into events or appended to history. Validation is kept strictly
 * separate from translation so that {@link CommandToEventTranslator} can assume
 * its input is already valid.
 *
 * <p>Invariants enforced:
 * <ol>
 *   <li>The command list is non-null and non-empty.
 *   <li>{@link CompleteWorkflow} is terminal: it may appear only as the last
 *       command. Any command following a {@code CompleteWorkflow} is rejected.
 *   <li>Within one decision, no two {@link ScheduleActivity} commands may share
 *       the same {@code activityId}. Duplicate IDs create ambiguity in the event
 *       history that cannot be resolved later.
 * </ol>
 *
 * <p>This class is stateless and safe to reuse across calls.
 */
public final class DecisionValidator {

    /**
     * Validates the supplied command list against all decision invariants.
     *
     * @param commands the ordered commands to validate; must not be null or empty
     * @throws NullPointerException     if {@code commands} is null
     * @throws IllegalArgumentException if any invariant is violated
     */
    public void validate(List<Command> commands) {
        Objects.requireNonNull(commands, "commands must not be null");
        if (commands.isEmpty()) {
            throw new IllegalArgumentException(
                    "a decision must contain at least one command");
        }

        Set<String> seenActivityIds = new HashSet<>();
        int lastIndex = commands.size() - 1;

        for (int i = 0; i < commands.size(); i++) {
            Command cmd = commands.get(i);

            if (cmd instanceof CompleteWorkflow && i != lastIndex) {
                throw new IllegalArgumentException(
                        "CompleteWorkflow is terminal and must be the last command"
                                + " in a decision, but was found at index "
                                + i
                                + " of "
                                + commands.size());
            }

            if (cmd instanceof ScheduleActivity sa) {
                if (!seenActivityIds.add(sa.activityId())) {
                    throw new IllegalArgumentException(
                            "duplicate activityId in decision: '"
                                    + sa.activityId()
                                    + "' — each activity must have a unique ID"
                                    + " within one decision");
                }
            }
        }
    }
}
