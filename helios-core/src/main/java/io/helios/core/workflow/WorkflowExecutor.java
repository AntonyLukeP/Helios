package io.helios.core.workflow;

import io.helios.core.command.Command;
import io.helios.core.command.CompleteWorkflow;
import io.helios.core.event.WorkflowCompleted;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.history.EventHistory;
import io.helios.core.history.RecordedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Phase 3 replay executor.
 *
 * <p>On every turn, this executor runs the workflow definition from its first
 * line against the current event history. It does not store an instruction
 * pointer. Instead it builds a {@link WorkflowContext} wired to the history
 * and lets the context fast-forward through recorded decisions or suspend at
 * the frontier.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Validate the history and locate the single {@link WorkflowStarted} event.
 *   <li>Build a fresh {@link WorkflowContext} from the run's recorded events.
 *   <li>Invoke {@link ReplayableWorkflowDefinition#execute} from the first line.
 *   <li>Translate the outcome into a list of new {@link Command}s:
 *       <ul>
 *         <li>Suspension → return context's newly emitted commands.
 *         <li>Normal return → validate history consumed, then return
 *             {@code CompleteWorkflow} (or empty list if already recorded).
 *       </ul>
 * </ol>
 *
 * <p>This class is stateless and safe to reuse across calls.
 */
public final class WorkflowExecutor {

    /**
     * Executes one replay turn for the given workflow against the current history.
     *
     * @param workflow the deterministic workflow definition to replay
     * @param history  the ordered event history for exactly one run
     * @return an unmodifiable list of new commands produced by this turn;
     *         empty if the run is already complete
     * @throws NullPointerException     if {@code workflow} or {@code history} is null
     * @throws IllegalArgumentException if the history is structurally invalid
     *                                  (no or multiple {@code WorkflowStarted}, or
     *                                  multiple {@code WorkflowCompleted})
     * @throws NonDeterminismException  if the workflow code is inconsistent with history
     */
    public List<Command> execute(ReplayableWorkflowDefinition workflow, EventHistory history) {
        Objects.requireNonNull(workflow, "workflow must not be null");
        Objects.requireNonNull(history, "history must not be null");

        List<WorkflowStarted> startedEvents = new ArrayList<>();
        List<WorkflowCompleted> completedEvents = new ArrayList<>();

        for (RecordedEvent re : history.events()) {
            switch (re.event()) {
                case WorkflowStarted ws -> startedEvents.add(ws);
                case WorkflowCompleted wc -> completedEvents.add(wc);
                default -> {}
            }
        }

        if (startedEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "history has no WorkflowStarted event"
                            + " — cannot determine workflow input for replay");
        }
        if (startedEvents.size() > 1) {
            throw new IllegalArgumentException(
                    "history has "
                            + startedEvents.size()
                            + " WorkflowStarted events — invalid history for replay");
        }
        if (completedEvents.size() > 1) {
            throw new IllegalArgumentException(
                    "history has "
                            + completedEvents.size()
                            + " WorkflowCompleted events — invalid history for replay");
        }

        String input = startedEvents.get(0).input();
        WorkflowContext context = new WorkflowContext(history.events());

        String workflowResult;
        try {
            workflowResult = workflow.execute(context, input);
        } catch (WorkflowSuspended ignored) {
            // Normal outcome: workflow reached the frontier and suspended.
            // Return whatever commands the context emitted during this turn.
            return List.copyOf(context.newCommands());
        }
        // NonDeterminismException and all other exceptions propagate unchanged.

        // Workflow returned normally — validate it consumed all recorded history.
        if (context.hasUnreplayedHistory()) {
            throw new NonDeterminismException(
                    "replay position",
                    "all recorded scheduled activities consumed before return",
                    "workflow returned while recorded scheduled activities remain unreplayed");
        }

        // Validate or append WorkflowCompleted.
        if (!completedEvents.isEmpty()) {
            // History already contains WorkflowCompleted — this is a final validation replay.
            WorkflowCompleted recorded = completedEvents.get(0);
            if (!Objects.equals(recorded.result(), workflowResult)) {
                throw new NonDeterminismException(
                        "WorkflowCompleted result", recorded.result(), workflowResult);
            }
            return List.of(); // run is already complete — no new commands
        }

        // First time the workflow has completed — append CompleteWorkflow.
        // context.newCommands() is empty when the workflow returns normally
        // (emitNewCommand always throws WorkflowSuspended before returning), but
        // the list is accumulated here for robustness.
        List<Command> result = new ArrayList<>(context.newCommands());
        result.add(new CompleteWorkflow(workflowResult));
        return List.copyOf(result);
    }
}
