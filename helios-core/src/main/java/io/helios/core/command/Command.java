package io.helios.core.command;

/**
 * A command is an intent issued by workflow code to the engine.
 *
 * <p>Commands express what the workflow <em>wants</em> to happen next. They are
 * not proof that anything happened. The engine translates each command into one
 * or more events, which are then appended to the run history.
 *
 * <p>Commands never appear in the event history directly.
 *
 * <p>Permitted subtypes: {@link ScheduleActivity}, {@link CompleteWorkflow}.
 */
public sealed interface Command permits ScheduleActivity, CompleteWorkflow {}
