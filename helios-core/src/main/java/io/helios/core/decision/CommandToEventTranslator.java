package io.helios.core.decision;

import io.helios.core.command.Command;
import io.helios.core.command.CompleteWorkflow;
import io.helios.core.command.ScheduleActivity;
import io.helios.core.event.ActivityScheduled;
import io.helios.core.event.Event;
import io.helios.core.event.WorkflowCompleted;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates an ordered list of validated commands into an equally ordered list
 * of events.
 *
 * <p>Translation is a pure, stateless mapping: each command produces exactly one
 * event with identical field values. No validation is performed here — callers
 * must pass commands that have already been accepted by {@link DecisionValidator}.
 * Translation must never silently correct an invalid command.
 *
 * <p>Mapping:
 * <ul>
 *   <li>{@link ScheduleActivity} → {@link ActivityScheduled} (same activity ID,
 *       type, and input)
 *   <li>{@link CompleteWorkflow} → {@link WorkflowCompleted} (same result)
 * </ul>
 *
 * <p>The exhaustive {@code switch} over the sealed {@link Command} interface
 * ensures the compiler will reject this class at build time if a new permitted
 * subtype is ever added without a corresponding translation arm.
 *
 * <p>This class is stateless and safe to reuse across calls.
 */
public final class CommandToEventTranslator {

    /**
     * Translates each command in the supplied list into its corresponding event,
     * preserving the original order.
     *
     * @param commands the ordered commands to translate; must not be null
     * @return an unmodifiable list of events in the same order as the commands
     * @throws NullPointerException if {@code commands} is null
     */
    public List<Event> translate(List<Command> commands) {
        Objects.requireNonNull(commands, "commands must not be null");
        List<Event> events = new ArrayList<>(commands.size());
        for (Command command : commands) {
            events.add(toEvent(command));
        }
        return List.copyOf(events);
    }

    /** Maps one command to its corresponding event. The switch is exhaustive over the sealed type. */
    private static Event toEvent(Command command) {
        return switch (command) {
            case ScheduleActivity sa ->
                    new ActivityScheduled(sa.activityId(), sa.activityType(), sa.input());
            case CompleteWorkflow cw -> new WorkflowCompleted(cw.result());
        };
    }
}
