package io.helios.core.engine;

import io.helios.core.command.Command;
import io.helios.core.decision.CommandToEventTranslator;
import io.helios.core.decision.DecisionValidator;
import io.helios.core.event.Event;
import io.helios.core.event.WorkflowStarted;
import io.helios.core.history.EventHistory;
import io.helios.core.history.InMemoryEventStore;
import io.helios.core.history.RecordedEvent;
import io.helios.core.run.WorkflowRunId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Application service that connects the Phase 2 in-memory components into a
 * single, coherent API for starting workflow runs and recording decisions.
 *
 * <p>This service is the only place that coordinates the four components:
 * {@link InMemoryEventStore} (persistence), {@link DecisionValidator} (guard),
 * {@link CommandToEventTranslator} (mapping), and the {@link WorkflowStarted}
 * event that anchors every run's history.
 *
 * <p><strong>Phase 2 scope</strong> — intentionally absent:
 * <ul>
 *   <li>No activity execution: {@code ScheduleActivity} records intent only.
 *   <li>No terminal-state guard: decisions after completion are not yet blocked;
 *       that belongs to the state-machine phase.
 *   <li>No database, no concurrency, no retry, no replay.
 * </ul>
 *
 * <p>All dependencies are supplied by the caller through the constructor.
 * No static stores, no DI framework.
 *
 * <p>This service is intentionally single-threaded. Concurrent access is not
 * supported or tested in Phase 2.
 */
public final class InMemoryWorkflowHistoryService {

    private final InMemoryEventStore store;
    private final DecisionValidator validator;
    private final CommandToEventTranslator translator;

    /**
     * Tracks which run IDs have been started. Used to:
     * <ul>
     *   <li>Reject duplicate starts.
     *   <li>Reject decisions for unknown (never-started) runs.
     * </ul>
     */
    private final Set<WorkflowRunId> startedRuns = new HashSet<>();

    /**
     * Creates the service with the three required collaborators.
     *
     * @param store      the event store that persists (in memory) all histories
     * @param validator  validates command lists before translation
     * @param translator maps validated commands to their corresponding events
     */
    public InMemoryWorkflowHistoryService(
            InMemoryEventStore store,
            DecisionValidator validator,
            CommandToEventTranslator translator) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.translator = Objects.requireNonNull(translator, "translator must not be null");
    }

    /**
     * Starts a new workflow run by appending a {@link WorkflowStarted} event as
     * the very first event in the run's history.
     *
     * @param runId        the stable, unique identifier for this run; must not be null
     * @param workflowType the name of the workflow definition (e.g. "LoanApproval")
     * @param input        the workflow input; may be null
     * @return the single {@link RecordedEvent} wrapping {@link WorkflowStarted}
     *         at sequence 1
     * @throws NullPointerException     if {@code runId} is null
     * @throws IllegalArgumentException if the run ID has already been started
     */
    public RecordedEvent start(WorkflowRunId runId, String workflowType, String input) {
        Objects.requireNonNull(runId, "runId must not be null");
        if (startedRuns.contains(runId)) {
            throw new IllegalArgumentException(
                    "run already started: '"
                            + runId.value()
                            + "' — each run ID may be started at most once");
        }
        startedRuns.add(runId);
        List<RecordedEvent> recorded =
                store.append(runId, List.of(new WorkflowStarted(workflowType, input)));
        return recorded.get(0);
    }

    /**
     * Applies a validated decision to an existing run by translating commands to
     * events and appending them to the run's history in a single operation.
     *
     * <p>The full path per command:
     * <ol>
     *   <li>Guard: confirm the run was started.
     *   <li>Validate: {@link DecisionValidator} checks structural invariants.
     *   <li>Translate: {@link CommandToEventTranslator} maps each command to an event.
     *   <li>Append: {@link InMemoryEventStore} assigns sequence numbers and stores events.
     * </ol>
     *
     * @param runId    the run to apply the decision to; must not be null
     * @param commands the ordered commands forming this decision; must not be null or empty
     * @return an unmodifiable list of the newly recorded events with their sequences
     * @throws NullPointerException     if {@code runId} or {@code commands} is null
     * @throws IllegalArgumentException if the run is unknown, or if validation fails
     */
    public List<RecordedEvent> applyDecision(WorkflowRunId runId, List<Command> commands) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(commands, "commands must not be null");
        if (!startedRuns.contains(runId)) {
            throw new IllegalArgumentException(
                    "unknown run: '"
                            + runId.value()
                            + "' — start the run before applying a decision");
        }
        validator.validate(commands);
        List<Event> events = translator.translate(commands);
        return store.append(runId, events);
    }

    /**
     * Returns the ordered, immutable event history for the given run.
     *
     * <p>If the run has never been started, an empty {@link EventHistory} is
     * returned rather than null. Callers do not need to null-check the result.
     *
     * @param runId the run to retrieve history for; must not be null
     * @return the run's complete event history, possibly empty
     * @throws NullPointerException if {@code runId} is null
     */
    public EventHistory history(WorkflowRunId runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        return store.history(runId);
    }
}
