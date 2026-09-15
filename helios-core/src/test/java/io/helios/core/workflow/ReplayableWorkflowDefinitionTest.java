package io.helios.core.workflow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReplayableWorkflowDefinition")
class ReplayableWorkflowDefinitionTest {

    @Test
    @DisplayName("can be expressed as a lambda (functional interface usability)")
    void canBeExpressedAsLambda() {
        ReplayableWorkflowDefinition greet = (context, input) -> "Hello, " + input + "!";
        WorkflowContext ctx = new WorkflowContext();
        Assertions.assertThat(greet.execute(ctx, "Ada")).isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("context and input are forwarded into the lambda body unchanged")
    void contextAndInputPassedThrough() {
        WorkflowContext[] capturedCtx = {null};
        String[] capturedInput = {null};

        ReplayableWorkflowDefinition capturer = (context, input) -> {
            capturedCtx[0] = context;
            capturedInput[0] = input;
            return "done";
        };

        WorkflowContext ctx = new WorkflowContext();
        capturer.execute(ctx, "loan-app-42");

        Assertions.assertThat(capturedCtx[0]).isSameAs(ctx);
        Assertions.assertThat(capturedInput[0]).isEqualTo("loan-app-42");
    }

    @Test
    @DisplayName("a null return from the lambda is permitted")
    void nullReturnIsPermitted() {
        ReplayableWorkflowDefinition returnsNull = (context, input) -> null;
        WorkflowContext ctx = new WorkflowContext();
        Assertions.assertThat(returnsNull.execute(ctx, "anything")).isNull();
    }
}
