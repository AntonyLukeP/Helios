package io.helios.core.event;

/**
 * An event is an immutable fact appended to a workflow run history.
 *
 * <p>Events are always past-tense: something <em>happened</em>. They are never
 * modified after being appended. A run history is the ordered sequence of all
 * events that have occurred for one workflow run.
 *
 * <p>Event records carry facts only. The sequence number that orders events
 * within a run is assigned by the history, not by the event itself. This keeps
 * the event type free of infrastructure concerns.
 *
 * <p>Permitted subtypes:
 * <ul>
 *   <li>{@link WorkflowStarted} — the run was initiated.
 *   <li>{@link ActivityScheduled} — an activity was requested (intent only).
 *   <li>{@link ActivityCompleted} — an external source reported an activity result.
 *   <li>{@link WorkflowCompleted} — the workflow declared a successful outcome.
 * </ul>
 *
 * <p>{@link ActivityCompleted} is produced by external sources (workers, test
 * harnesses), not by workflow command translation. It must not appear in
 * {@link io.helios.core.decision.CommandToEventTranslator}.
 */
public sealed interface Event
        permits WorkflowStarted, ActivityScheduled, ActivityCompleted, WorkflowCompleted {}
